package dev.ferro.client.event;

/**
 * Marker interface for every class that wants to receive events.
 *
 * <p>Concrete listener contracts live in {@code dev.ferro.client.event.events} and each of them
 * declares exactly one callback method. A module implements the interfaces it is interested in and
 * registers itself with {@link EventManager#add}.</p>
 */
public interface Listener {
}
