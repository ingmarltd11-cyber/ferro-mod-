package dev.ferro.client.event;

/**
 * An {@link Event} that can be cancelled.
 *
 * <p>Cancellation is cooperative: {@link EventManager} keeps dispatching to the remaining listeners
 * so that every module can run its own bookkeeping. Listeners that need to know about cancellation
 * should check {@link #isCancelled()}, and the mixin that fired the event must check it before
 * running vanilla logic.</p>
 *
 * @param <T> the listener interface that receives this event
 */
public abstract class CancellableEvent<T extends Listener> extends Event<T> {

    private boolean cancelled;

    /**
     * Creates a new cancellable event bound to the given listener interface.
     *
     * @param listenerType the listener interface, never {@code null}
     */
    protected CancellableEvent(Class<T> listenerType) {
        super(listenerType);
    }

    /**
     * Marks this event as cancelled.
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * @param cancelled the new cancellation state
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * @return {@code true} when a listener cancelled this event
     */
    public boolean isCancelled() {
        return cancelled;
    }
}
