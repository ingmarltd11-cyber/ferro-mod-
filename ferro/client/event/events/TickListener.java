package dev.ferro.client.event.events;

import dev.ferro.client.event.Listener;

/**
 * Called once per client tick, after the game finished ticking.
 */
public interface TickListener extends Listener {

    /**
     * @param event the event instance
     */
    void onTick(TickEvent event);
}
