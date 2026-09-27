
package dev.ferro.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.phys.Vec3;

/**
 * The packet API modules use.
 *
 * <p>Everything here delegates to {@link PacketHandler}, so a module never has to know how the pipeline
 * works: it either sends through the pipeline with {@link #send} or deliberately bypasses it with
 * {@link #sendDirect}.</p>
 */
public final class PacketUtils {

    private PacketUtils() {
    }

    /**
     * Sends a packet through the pipeline, so listeners and hold rules still apply.
     *
     * @param packet the packet to send
     */
    public static void send(Packet<?> packet) {
        if (packet == null) {
            return;
        }
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            Log.warn("Dropped {} because there is no connection", packet.getClass().getSimpleName());
            return;
        }
        connection.send(packet);
    }

    /**
     * Sends a packet straight to the server, bypassing every listener and hold rule.
     *
     * @param packet the packet to send
     */
    public static void sendDirect(Packet<?> packet) {
        PacketHandler.sendDirect(packet);
    }

    /**
     * Reports the on ground flag, used to reset the server side fall distance.
     *
     * @param onGround the value the server should believe
     */
    public static void sendStatus(boolean onGround) {
        PacketHandler.spoofGround(onGround);
    }

    /**
     * Reports a rotation without changing the client rotation.
     *
     * @param yaw                the yaw
     * @param pitch              the pitch
     * @param onGround           the on ground flag
     * @param horizontalCollision whether the player collides horizontally
     */
    public static void sendRotation(float yaw, float pitch, boolean onGround, boolean horizontalCollision) {
        PacketHandler.spoofRotation(yaw, pitch);
    }

    /**
     * Reports a full position and rotation.
     *
     * @param position           the position
     * @param yaw                the yaw
     * @param pitch              the pitch
     * @param onGround           the on ground flag
     * @param horizontalCollision whether the player collides horizontally
     */
    public static void sendPositionRotation(Vec3 position, float yaw, float pitch, boolean onGround,
                                            boolean horizontalCollision) {
        PacketHandler.spoofPosition(position, yaw, pitch, onGround);
    }

    /**
     * Reports the held hotbar slot without changing the client slot.
     *
     * @param slot the hotbar slot
     */
    public static void sendCarriedItem(int slot) {
        PacketHandler.spoofHeldSlot(slot);
    }

    /**
     * @param packet the packet to inspect
     * @return {@code true} when the packet is a player movement packet
     */
    public static boolean isMovementPacket(Packet<?> packet) {
        return packet instanceof ServerboundMovePlayerPacket;
    }

    /**
     * @param packet the packet to inspect
     * @return {@code true} when the packet reports a rotation
     */
    public static boolean isRotationPacket(Packet<?> packet) {
        return packet instanceof ServerboundMovePlayerPacket.Rot
                || packet instanceof ServerboundMovePlayerPacket.PosRot
                || packet instanceof ServerboundMovePlayerPacket.Pos;
    }

    /**
     * @param packet the packet to inspect
     * @return {@code true} when the packet updates the held hotbar slot
     */
    public static boolean isCarriedItemPacket(Packet<?> packet) {
        return packet instanceof ServerboundSetCarriedItemPacket;
    }
}
