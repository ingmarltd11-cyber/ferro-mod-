package dev.ferro.client.event.events;

import dev.ferro.client.event.Event;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Fired once per frame at the head of {@code Gui#render}. This is the main drawing hook: HUD,
 * ESP, ClickGUI overlays and every other screen space module draws here through {@link GuiGraphics}.
 */
public class Render2DEvent extends Event<Render2DListener> {

    private final GuiGraphics graphics;
    private final DeltaTracker deltaTracker;

    /**
     * @param graphics     the current GUI render context
     * @param deltaTracker frame delta information
     */
    public Render2DEvent(GuiGraphics graphics, DeltaTracker deltaTracker) {
        super(Render2DListener.class);
        this.graphics = graphics;
        this.deltaTracker = deltaTracker;
    }

    /**
     * @return the GUI render context
     */
    public GuiGraphics getGraphics() {
        return graphics;
    }

    /**
     * @return frame delta information for interpolation
     */
    public DeltaTracker getDeltaTracker() {
        return deltaTracker;
    }

    @Override
    public void call(Render2DListener listener) {
        listener.onRender2D(this);
    }
}
