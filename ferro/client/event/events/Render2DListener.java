package dev.ferro.client.event.events;

import dev.ferro.client.event.Listener;

/**
 * Called once per frame during the HUD/overlay pass.
 */
public interface Render2DListener extends Listener {

    /**
     * @param event the event instance
     */
    void onRender2D(Render2DEvent event);
}
