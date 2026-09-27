
package dev.ferro.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * A complete ClickGUI layout.
 *
 * <p>The screen owns almost nothing: it picks a layout, draws the shared backdrop and forwards every input
 * event. That is what makes the classic draggable frames and the modern two pane interface two
 * implementations of the same contract instead of two half merged screens.</p>
 */
public interface GuiLayout {

    /**
     * Called when the screen is initialised or resized.
     */
    default void init() {
    }

    /**
     * Draws the layout.
     *
     * @param graphics    the render context
     * @param mouseX      the mouse x position
     * @param mouseY      the mouse y position
     * @param partialTick the frame delta
     */
    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    /**
     * @param event   the mouse button event
     * @param doubled whether this was a double click
     * @return {@code true} when the click was consumed
     */
    default boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        return false;
    }

    /**
     * @param event the mouse button event
     * @return {@code true} when the release was consumed
     */
    default boolean mouseReleased(MouseButtonEvent event) {
        return false;
    }

    /**
     * @param event the mouse button event
     * @param dragX the x movement since the last event
     * @param dragY the y movement since the last event
     * @return {@code true} when the drag was consumed
     */
    default boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return false;
    }

    /**
     * @param mouseX  the mouse x position
     * @param mouseY  the mouse y position
     * @param scrollX the horizontal scroll amount
     * @param scrollY the vertical scroll amount
     * @return {@code true} when the scroll was consumed
     */
    default boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    /**
     * @param event the key event
     * @return {@code true} when the key was consumed
     */
    default boolean keyPressed(KeyEvent event) {
        return false;
    }

    /**
     * @param event the character event
     * @return {@code true} when the character was consumed
     */
    default boolean charTyped(CharacterEvent event) {
        return false;
    }

    /**
     * Persists whatever this layout wants to remember. Called when the screen closes, before the config is
     * written.
     */
    default void save() {
    }

    /**
     * @return the hint shown at the bottom of the screen
     */
    default String hint() {
        return "";
    }
}
