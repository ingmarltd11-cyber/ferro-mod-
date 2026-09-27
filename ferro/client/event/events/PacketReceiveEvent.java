package dev.ferro.client.event.events;

import dev.ferro.client.event.CancellableEvent;
import net.minecraft.network.protocol.Packet;

/**
 * Fired at the head of the network read handler. Cancelling it drops the incoming packet.
 */
public class PacketReceiveEvent extends CancellableEvent<PacketReceiveListener> {

    private final Packet<?> packet;

    /**
     * @param packet the incoming packet
     */
    public PacketReceiveEvent(Packet<?> packet) {
        super(PacketReceiveListener.class);
        this.packet = packet;
    }

    /**
     * @return the incoming packet
     */
    public Packet<?> getPacket() {
        return packet;
    }

    @Override
    public void call(PacketReceiveListener listener) {
        listener.onPacketReceive(this);
    }
}
