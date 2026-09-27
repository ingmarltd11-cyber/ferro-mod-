package dev.ferro.client.event.events;

import dev.ferro.client.event.Listener;

/**
 * Called once per frame for world anchored overlay geometry.
 */
public interface Render3DListener extends Listener {

    /**
     * @param event the event instance
     */
    void onRender3D(Render3DEvent event);
}
