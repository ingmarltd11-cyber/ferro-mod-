package dev.ferro.client.event;

import dev.ferro.client.utils.Log;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central, priority ordered event bus.
 *
 * <p>Listeners are stored per concrete event class. Registration is cheap and thread safe, which
 * matters because modules enable and disable themselves while the game is running. Duplicate
 * registrations of the same instance for the same event class are ignored, so a module that calls
 * {@code add(...)} twice cannot receive an event twice.</p>
 */
public final class EventManager {

    /**
     * Dispatch order of listeners. {@link #HIGHEST} runs first.
     */
    public enum Priority {
        /** Runs before every other listener. */
        HIGHEST,
        /** Runs early. */
        HIGH,
        /** Default priority. */
        NORMAL,
        /** Runs late. */
        LOW,
        /** Runs last, ideal for observers that should see the final state. */
        LOWEST
    }

    private static final EventManager INSTANCE = new EventManager();

    private final Map<Class<?>, CopyOnWriteArrayList<Registration>> listeners = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong();

    private EventManager() {
    }

    /**
     * @return the process wide event bus
     */
    public static EventManager getInstance() {
        return INSTANCE;
    }

    private record Registration(Listener listener, Priority priority, long order) {
    }

    /**
     * Registers a listener with {@link Priority#NORMAL}.
     *
     * @param eventType the concrete event class
     * @param listener  the listener instance
     */
    public void add(Class<? extends Event<?>> eventType, Listener listener) {
        add(eventType, Priority.NORMAL, listener);
    }

    /**
     * Registers a listener for an event class.
     *
     * @param eventType the concrete event class
     * @param priority  dispatch priority
     * @param listener  the listener instance
     */
    public void add(Class<? extends Event<?>> eventType, Priority priority, Listener listener) {
        if (eventType == null || listener == null) {
            return;
        }
        CopyOnWriteArrayList<Registration> list = listeners.computeIfAbsent(eventType, key -> new CopyOnWriteArrayList<>());
        for (Registration registration : list) {
            if (registration.listener() == listener) {
                return;
            }
        }
        list.add(new Registration(listener, priority, counter.incrementAndGet()));
    }

    /**
     * Removes every registration that belongs to the given listener instance.
     *
     * @param listener the listener to unregister
     */
    public void remove(Listener listener) {
        if (listener == null) {
            return;
        }
        for (CopyOnWriteArrayList<Registration> list : listeners.values()) {
            list.removeIf(registration -> registration.listener() == listener);
        }
    }

    /**
     * Removes all registrations for the given event class.
     *
     * @param eventType the event class to purge
     */
    public void removeAll(Class<? extends Event<?>> eventType) {
        listeners.remove(eventType);
    }

    /**
     * Removes every registration in the bus. Used when the client shuts down.
     */
    public void clear() {
        listeners.clear();
    }

    /**
     * @param eventType the event class to inspect
     * @return the amount of listeners registered for that class
     */
    public int listenerCount(Class<? extends Event<?>> eventType) {
        CopyOnWriteArrayList<Registration> list = listeners.get(eventType);
        return list == null ? 0 : list.size();
    }

    /**
     * Dispatches an event to every registered listener, ordered by priority.
     *
     * @param event the event instance
     */
    @SuppressWarnings("unchecked")
    public void post(Event<?> event) {
        if (event == null) {
            return;
        }
        CopyOnWriteArrayList<Registration> list = listeners.get(event.getClass());
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Registration> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparingInt((Registration registration) -> registration.priority().ordinal())
                .thenComparingLong(Registration::order));
        Class<?> expected = event.getListenerType();
        for (Registration registration : sorted) {
            Listener listener = registration.listener();
            if (!expected.isInstance(listener)) {
                Log.warn("Listener {} is registered for {} but not of type {}", listener.getClass().getSimpleName(),
                        event.getClass().getSimpleName(), expected.getSimpleName());
                continue;
            }
            try {
                ((Event<Listener>) event).call(listener);
            } catch (Throwable throwable) {
                Log.error("Exception while dispatching " + event.getClass().getSimpleName() + " to "
                        + listener.getClass().getSimpleName(), throwable);
            }
        }
    }
}
