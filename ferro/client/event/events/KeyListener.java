package dev.ferro.client.event.events;

import dev.ferro.client.event.Listener;

/**
 * Called on every key press, release and repeat of the game window.
 */
public interface KeyListener extends Listener {

    /**
     * @param event the event instance
     */
    void onKey(KeyEvent event);
}
