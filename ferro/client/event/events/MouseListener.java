package dev.ferro.client.event.events;

import dev.ferro.client.event.Listener;

/**
 * Called on every mouse button press and release.
 */
public interface MouseListener extends Listener {

    /**
     * @param event the event instance
     */
    void onMouse(MouseEvent event);
}
