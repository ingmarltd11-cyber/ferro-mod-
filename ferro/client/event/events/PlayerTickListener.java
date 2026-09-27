package dev.ferro.client.event.events;

import dev.ferro.client.event.Listener;

/**
 * Called once per tick of the local player.
 */
public interface PlayerTickListener extends Listener {

    /**
     * @param event the event instance
     */
    void onPlayerTick(PlayerTickEvent event);
}
