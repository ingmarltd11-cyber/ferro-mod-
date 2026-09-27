package dev.ferro.client.event.events;

import dev.ferro.client.event.Listener;

/**
 * Called right before a packet leaves the client.
 */
public interface PacketSendListener extends Listener {

    /**
     * @param event the event instance
     */
    void onPacketSend(PacketSendEvent event);
}
