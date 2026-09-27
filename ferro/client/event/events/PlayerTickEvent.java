package dev.ferro.client.event.events;

import dev.ferro.client.event.Event;

/**
 * Fired at the head of {@code LocalPlayer#tick()}.
 */
public class PlayerTickEvent extends Event<PlayerTickListener> {

    /**
     * Creates a new player tick event.
     */
    public PlayerTickEvent() {
        super(PlayerTickListener.class);
    }

    @Override
    public void call(PlayerTickListener listener) {
        listener.onPlayerTick(this);
    }
}
