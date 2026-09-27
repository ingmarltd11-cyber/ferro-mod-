package dev.ferro.client.event.events;

import dev.ferro.client.event.Event;

/**
 * Fired once per client tick via Fabric's client tick callback.
 */
public class TickEvent extends Event<TickListener> {

    /** Number of ticks the client has been running for since the event was created. */
    private final int tickCount;

    /**
     * @param tickCount the current tick counter
     */
    public TickEvent(int tickCount) {
        super(TickListener.class);
        this.tickCount = tickCount;
    }

    /**
     * @return the client tick counter
     */
    public int getTickCount() {
        return tickCount;
    }

    @Override
    public void call(TickListener listener) {
        listener.onTick(this);
    }
}
