package dev.ferro.client.event.events;

import dev.ferro.client.event.Listener;

/**
 * Called right before an incoming packet is handled.
 */
public interface PacketReceiveListener extends Listener {

    /**
     * @param event the event instance
     */
    void onPacketReceive(PacketReceiveEvent event);
}
