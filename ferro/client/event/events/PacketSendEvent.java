package dev.ferro.client.event.events;

import dev.ferro.client.event.CancellableEvent;
import net.minecraft.network.protocol.Packet;

/**
 * Fired at the head of {@code Connection#send}. Cancelling it drops the packet.
 */
public class PacketSendEvent extends CancellableEvent<PacketSendListener> {

    private Packet<?> packet;

    /**
     * @param packet the outgoing packet
     */
    public PacketSendEvent(Packet<?> packet) {
        super(PacketSendListener.class);
        this.packet = packet;
    }

    /**
     * @return the outgoing packet
     */
    public Packet<?> getPacket() {
        return packet;
    }

    /**
     * Replaces the outgoing packet, which is how spoofing modules rewrite movement or rotation.
     *
     * @param packet the packet to send instead
     */
    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }

    @Override
    public void call(PacketSendListener listener) {
        listener.onPacketSend(this);
    }
}
