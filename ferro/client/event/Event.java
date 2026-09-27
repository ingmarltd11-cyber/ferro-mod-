package dev.ferro.client.event;

/**
 * Base type of every event that FERRO dispatches.
 *
 * <p>An event is bound to exactly one {@link Listener} subtype ({@code T}). Dispatching is done by
 * {@link EventManager}, which calls {@link #call(Listener)} with every registered listener whose
 * type matches {@link #getListenerType()}.</p>
 *
 * @param <T> the listener interface that receives this event
 */
public abstract class Event<T extends Listener> {

    private final Class<T> listenerType;

    /**
     * Creates a new event bound to the given listener interface.
     *
     * @param listenerType the listener interface, never {@code null}
     */
    protected Event(Class<T> listenerType) {
        this.listenerType = listenerType;
    }

    /**
     * @return the listener interface this event is dispatched to
     */
    public Class<T> getListenerType() {
        return listenerType;
    }

    /**
     * Invokes the matching callback on a single listener. Implemented by every concrete event so
     * that {@link EventManager} does not need reflection.
     *
     * @param listener the listener to notify
     */
    public abstract void call(T listener);

    /**
     * Dispatches this event through the global {@link EventManager}.
     */
    public void post() {
        EventManager.getInstance().post(this);
    }
}
