
package dev.ferro.client.utils;

import dev.ferro.client.event.events.PacketReceiveEvent;
import dev.ferro.client.event.events.PacketSendEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.phys.Vec3;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * The single place where every packet of this client is handled.
 *
 * <p>Nothing in FERRO talks to the network on its own. {@code ConnectionMixin} hands every outgoing and
 * incoming packet to {@link #dispatchSend} and {@link #dispatchReceive}, and this class decides, in one
 * order that is easy to reason about, what happens to it:</p>
 *
 * <ol>
 *   <li><b>Internal sends bypass everything.</b> Packets sent by {@link #sendDirect} carry an internal flag,
 *       so a module that rewrites a packet can never feed its own rewrite back into the event chain.</li>
 *   <li><b>Mod payloads are refused.</b> A {@code ServerboundCustomPayloadPacket} is dropped and logged. That
 *       is the mechanism by which a client would register a plugin channel with the server, so refusing it is
 *       what guarantees FERRO leaves no trace server side: no channel, no registry entry, nothing.</li>
 *   <li><b>Modules decide through events.</b> {@link PacketSendEvent} and {@link PacketReceiveEvent} are
 *       posted, and a listener can cancel, or replace the packet.</li>
 *   <li><b>Holders get their turn.</b> Held back packets live here, not in the modules: {@link #delay} for the
 *       time based queue that PingSpoof uses, {@link #buffer} for the "keep it until I say so" buffer that
 *       Blink uses. A bounded queue means a buggy module can never starve the connection.</li>
 * </ol>
 *
 * <p>Incoming handlers run on the network thread, exactly like vanilla packet handling. They may touch game
 * state, they must not render and they must not block.</p>
 */
public final class PacketHandler {

    /** Maximum amount of packets in the delayed queue before the oldest one is released anyway. */
    public static final int MAX_HELD = 256;

    /** Maximum amount of packets kept in the diagnostic history. */
    public static final int MAX_HISTORY = 64;

    /** A packet waiting for its release time. */
    private record Held(Packet<?> packet, long releaseAt) {
    }

    /**
     * One entry of the diagnostic history.
     *
     * @param time     the timestamp
     * @param outgoing {@code true} for a packet sent to the server
     * @param name     the simple class name of the packet
     */
    public record Entry(long time, boolean outgoing, String name) {
    }

    /**
     * A snapshot of the counters.
     *
     * @param sent            packets sent to the server
     * @param received        packets received from the server
     * @param dropped         packets a module cancelled
     * @param blockedPayloads outgoing mod payloads that were refused
     * @param held            packets currently in the delayed queue
     * @param buffered        packets currently in the hold buffer
     */
    public record Stats(int sent, int received, int dropped, int blockedPayloads, int held, int buffered) {
    }

    private static final ThreadLocal<Boolean> INTERNAL = ThreadLocal.withInitial(() -> Boolean.FALSE);

    // The netty thread and the render thread both touch these: incoming and outgoing packets are recorded on
    // the network thread while modules read the diagnostics on the render thread. They must be thread safe,
    // or a torn ArrayDeque/Map makes a read throw a NoSuchElementException and kick the client off the
    // server, which happened with the array based queues.
    private static final Deque<Held> delayed = new ConcurrentLinkedDeque<>();
    private static final Deque<Packet<?>> buffered = new ConcurrentLinkedDeque<>();
    private static final Deque<Entry> history = new ConcurrentLinkedDeque<>();
    private static final Map<String, Integer> sentTypes = new ConcurrentHashMap<>();
    private static final Map<String, Integer> receivedTypes = new ConcurrentHashMap<>();

    private static int sent;
    private static int received;
    private static int dropped;
    private static int blockedPayloads;
    private static boolean warnedAboutServerPayload;
    private static boolean armed = true;

    private PacketHandler() {
    }

    /**
     * Refuses to run anywhere except on a client.
     *
     * <p>FERRO ships no server entrypoint and registers its mixins in the {@code client} section of
     * {@code ferro.mixins.json}, so the loader already keeps it out of dedicated servers. This check is the
     * explicit belt on top of those braces: if somebody ever changes the environment in
     * {@code fabric.mod.json} to {@code *}, the client fails loudly instead of quietly installing a packet
     * handler on a server.</p>
     *
     * @throws IllegalStateException when the loader reports anything other than a client environment
     */
    public static void validateEnvironment() {
        EnvType environment = FabricLoader.getInstance().getEnvironmentType();
        if (environment != EnvType.CLIENT) {
            throw new IllegalStateException("FERRO is a client side mod and must not run in a "
                    + environment + " environment");
        }
        Log.info("PacketHandler armed: client only, no server component, no plugin channel");
    }

    /**
     * @return {@code true} while an internal packet is being sent
     */
    public static boolean isInternal() {
        return INTERNAL.get();
    }

    /**
     * Decides what happens to an outgoing packet.
     *
     * @param packet the packet the client is about to send
     * @return {@code true} when the packet may be sent, {@code false} when it was dropped or replaced
     */
    public static boolean dispatchSend(Packet<?> packet) {
        if (packet == null) {
            return false;
        }
        if (INTERNAL.get()) {
            return true;
        }
        if (packet instanceof ServerboundCustomPayloadPacket) {
            blockedPayloads++;
            Log.warn("Refused an outgoing mod payload: FERRO claims no channel on the server");
            return false;
        }
        if (!armed) {
            return true;
        }
        PacketSendEvent event = new PacketSendEvent(packet);
        event.post();
        if (event.isCancelled()) {
            dropped++;
            record(true, packet);
            return false;
        }
        Packet<?> replacement = event.getPacket();
        if (replacement != null && replacement != packet) {
            dropped++;
            sendDirect(replacement);
            return false;
        }
        sent++;
        record(true, packet);
        return true;
    }

    /**
     * Decides what happens to an incoming packet.
     *
     * @param packet the packet the server sent
     * @return {@code true} when the packet may be handled, {@code false} when it was dropped
     */
    public static boolean dispatchReceive(Packet<?> packet) {
        if (packet == null) {
            return false;
        }
        if (packet instanceof ClientboundCustomPayloadPacket && !warnedAboutServerPayload) {
            warnedAboutServerPayload = true;
            Log.warn("A server side mod sent a payload; FERRO answers no channel, the packet is counted only");
        }
        if (!armed) {
            return true;
        }
        PacketReceiveEvent event = new PacketReceiveEvent(packet);
        event.post();
        if (event.isCancelled()) {
            dropped++;
            record(false, packet);
            return false;
        }
        received++;
        record(false, packet);
        return true;
    }

    /**
     * Sends a packet straight to the server, bypassing every listener, hold rule and replacement.
     *
     * <p>This is the path for packets the client itself is responsible for, such as a teleport confirmation:
     * they must never be delayed, rewritten or vetoed by a module.</p>
     *
     * @param packet the packet to send
     */
    public static void sendDirect(Packet<?> packet) {
        if (packet == null) {
            return;
        }
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            Log.warn("Dropped {} because there is no connection", packet.getClass().getSimpleName());
            return;
        }
        INTERNAL.set(Boolean.TRUE);
        try {
            connection.send(packet);
            sent++;
            record(true, packet);
        } catch (Throwable throwable) {
            Log.error("Failed to send " + packet.getClass().getSimpleName(), throwable);
        } finally {
            INTERNAL.set(Boolean.FALSE);
        }
    }

    /**
     * Reports a rotation to the server without changing the client rotation.
     *
     * @param yaw   the yaw the server should believe
     * @param pitch the pitch the server should believe
     */
    public static void spoofRotation(float yaw, float pitch) {
        LocalPlayer player = Minecraft.getInstance().player;
        boolean onGround = player != null && player.onGround();
        boolean collision = player != null && player.horizontalCollision;
        sendDirect(new ServerboundMovePlayerPacket.Rot(yaw, pitch, onGround, collision));
    }

    /**
     * Reports a position, rotation and ground state to the server.
     *
     * @param position the position the server should believe
     * @param yaw      the yaw
     * @param pitch    the pitch
     * @param onGround the on ground flag
     */
    public static void spoofPosition(Vec3 position, float yaw, float pitch, boolean onGround) {
        LocalPlayer player = Minecraft.getInstance().player;
        boolean collision = player != null && player.horizontalCollision;
        sendDirect(new ServerboundMovePlayerPacket.PosRot(position.x, position.y, position.z, yaw, pitch, onGround,
                collision));
    }

    /**
     * Reports the on ground flag to the server.
     *
     * @param onGround the value the server should believe
     */
    public static void spoofGround(boolean onGround) {
        LocalPlayer player = Minecraft.getInstance().player;
        boolean collision = player != null && player.horizontalCollision;
        sendDirect(new ServerboundMovePlayerPacket.StatusOnly(onGround, collision));
    }

    /**
     * Tells the server which hotbar slot is held, without changing the client slot.
     *
     * @param slot the hotbar slot, {@code 0} to {@code 8}
     */
    public static void spoofHeldSlot(int slot) {
        sendDirect(new ServerboundSetCarriedItemPacket(slot));
    }

    /**
     * Queues a packet for a later release.
     *
     * @param packet  the packet
     * @param millis  the delay in milliseconds
     */
    public static void delay(Packet<?> packet, long millis) {
        if (packet == null) {
            return;
        }
        if (delayed.size() >= MAX_HELD) {
            Held oldest = delayed.pollFirst();
            if (oldest != null) {
                Log.warn("Delayed queue full, releasing the oldest packet early");
                sendDirect(oldest.packet());
            }
        }
        delayed.addLast(new Held(packet, System.currentTimeMillis() + Math.max(0L, millis)));
    }

    /**
     * Adds a packet to the hold buffer.
     *
     * @param packet the packet
     */
    public static void buffer(Packet<?> packet) {
        if (packet == null) {
            return;
        }
        if (buffered.size() >= MAX_HELD) {
            buffered.pollFirst();
        }
        buffered.addLast(packet);
    }

    /**
     * Takes everything out of the hold buffer.
     *
     * @return the buffered packets, oldest first
     */
    public static List<Packet<?>> drainBuffer() {
        List<Packet<?>> drained = List.copyOf(buffered);
        buffered.clear();
        return drained;
    }

    /**
     * Sends every delayed packet whose release time has passed. Driven from the client tick.
     */
    public static void tick() {
        releaseExpired();
    }

    /**
     * Sends every delayed packet whose release time has passed.
     */
    public static void releaseExpired() {
        long now = System.currentTimeMillis();
        while (!delayed.isEmpty() && delayed.peekFirst().releaseAt() <= now) {
            Held held = delayed.pollFirst();
            if (held != null) {
                sendDirect(held.packet());
            }
        }
    }

    /**
     * Sends every delayed packet immediately, whatever its release time.
     */
    public static void releaseDelayed() {
        while (!delayed.isEmpty()) {
            Held held = delayed.pollFirst();
            if (held != null) {
                sendDirect(held.packet());
            }
        }
    }

    /**
     * Sends every buffered packet in order.
     */
    public static void releaseBuffer() {
        List<Packet<?>> drained = drainBuffer();
        for (Packet<?> packet : drained) {
            sendDirect(packet);
        }
    }

    /**
     * Releases everything this handler is holding.
     */
    public static void flush() {
        releaseDelayed();
        releaseBuffer();
    }

    /**
     * Disarms the handler and flushes what it holds, used when the game shuts down.
     */
    public static void shutdown() {
        flush();
        armed = false;
        INTERNAL.set(Boolean.FALSE);
    }

    /**
     * Re-arms the handler, used when a config reload has to keep the client running.
     */
    public static void arm() {
        armed = true;
    }

    /**
     * @return {@code true} when the handler is actively filtering packets
     */
    public static boolean isArmed() {
        return armed;
    }

    /**
     * @return the amount of packets in the delayed queue
     */
    public static int heldCount() {
        return delayed.size();
    }

    /**
     * @return the amount of packets in the hold buffer
     */
    public static int bufferedCount() {
        return buffered.size();
    }

    /**
     * @return a snapshot of the counters
     */
    public static Stats getStats() {
        return new Stats(sent, received, dropped, blockedPayloads, delayed.size(), buffered.size());
    }

    /**
     * @return a one line summary, used by PacketLogger and by the log at shutdown
     */
    public static String summary() {
        Stats stats = getStats();
        return "packets sent=" + stats.sent() + " received=" + stats.received() + " dropped=" + stats.dropped()
                + " blockedPayloads=" + stats.blockedPayloads() + " held=" + stats.held()
                + " buffered=" + stats.buffered();
    }

    /**
     * @return the counters per outgoing packet type, most frequent first
     */
    public static Map<String, Integer> sentTypes() {
        return sortByCount(sentTypes);
    }

    /**
     * @return the counters per incoming packet type, most frequent first
     */
    public static Map<String, Integer> receivedTypes() {
        return sortByCount(receivedTypes);
    }

    /**
     * @return the most recent packets, newest first
     */
    public static List<Entry> recentPackets() {
        return List.copyOf(history);
    }

    /**
     * Resets every counter and the history.
     */
    public static void clearStats() {
        sent = 0;
        received = 0;
        dropped = 0;
        blockedPayloads = 0;
        sentTypes.clear();
        receivedTypes.clear();
        history.clear();
    }

    /**
     * Records a packet for the diagnostics.
     *
     * @param outgoing the direction
     * @param packet   the packet
     */
    private static void record(boolean outgoing, Packet<?> packet) {
        String name = packet.getClass().getSimpleName();
        Map<String, Integer> counters = outgoing ? sentTypes : receivedTypes;
        counters.merge(name, 1, Integer::sum);
        history.addFirst(new Entry(System.currentTimeMillis(), outgoing, name));
        while (history.size() > MAX_HISTORY) {
            history.removeLast();
        }
    }

    /**
     * @param counters the counters to sort
     * @return the counters sorted by value, descending
     */
    private static Map<String, Integer> sortByCount(Map<String, Integer> counters) {
        Map<String, Integer> sorted = new LinkedHashMap<>();
        counters.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return sorted;
    }
}
