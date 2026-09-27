
package dev.ferro.client.ui.components;

import dev.ferro.client.module.setting.BooleanSetting;
import dev.ferro.client.module.setting.KeybindSetting;
import dev.ferro.client.module.setting.ModeSetting;
import dev.ferro.client.module.setting.NumberSetting;
import dev.ferro.client.module.setting.Setting;
import dev.ferro.client.module.setting.StringSetting;
import dev.ferro.client.utils.MathUtils;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Base class of every ClickGUI setting widget.
 *
 * <p>Components are not vanilla widgets: the whole ClickGUI does its own hit testing so that it does
 * not depend on the widget API, which changes frequently. A component is laid out by its parent
 * ({@link ModuleButton}) with {@link #setBounds} and reports its own height through
 * {@link #getHeight()}.</p>
 */
public abstract class Component {

    /** Height of a single line component. */
    protected static final double ROW_HEIGHT = 13.0D;

    /** Speed of the hover, expand and slider animations, as a factor per frame. */
    protected static final double ANIMATION_SPEED = 0.35D;

    private final Setting<?> setting;
    private double x;
    private double y;
    private double width;
    private boolean hovered;

    /**
     * @param setting the setting this component edits
     */
    protected Component(Setting<?> setting) {
        this.setting = setting;
    }

    /**
     * Creates the matching component for a setting type.
     *
     * @param setting the setting
     * @return the component, never {@code null}
     */
    public static Component of(Setting<?> setting) {
        if (setting instanceof BooleanSetting booleanSetting) {
            return new BooleanComponent(booleanSetting);
        }
        if (setting instanceof NumberSetting numberSetting) {
            return new NumberComponent(numberSetting);
        }
        if (setting instanceof ModeSetting modeSetting) {
            return new ModeComponent(modeSetting);
        }
        if (setting instanceof KeybindSetting keybindSetting) {
            return new KeybindComponent(keybindSetting);
        }
        if (setting instanceof StringSetting stringSetting) {
            return new StringComponent(stringSetting);
        }
        return new StringComponent(new StringSetting(setting.getName(), setting.displayValue(), 64));
    }

    /**
     * @return the setting behind this component
     */
    public Setting<?> getSetting() {
        return setting;
    }

    /**
     * @param x     the left edge
     * @param y     the top edge
     * @param width the width
     */
    public final void setBounds(double x, double y, double width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    /**
     * @return the left edge
     */
    protected final double getX() {
        return x;
    }

    /**
     * @return the top edge
     */
    protected final double getY() {
        return y;
    }

    /**
     * @return the width
     */
    protected final double getWidth() {
        return width;
    }

    /**
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @return {@code true} when the mouse is inside this component
     */
    protected final boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + getHeight();
    }

    /**
     * @return whether the mouse was inside this component during the last render
     */
    protected final boolean wasHovered() {
        return hovered;
    }

    /**
     * Updates the hover state, called from {@link #render}.
     *
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     */
    protected final void updateHovered(double mouseX, double mouseY) {
        this.hovered = isHovered(mouseX, mouseY);
    }

    /**
     * Eases a value towards a target. Frame rate dependent by design: the ClickGUI runs at the game
     * frame rate and a fixed factor keeps the code trivial.
     *
     * @param current the current value
     * @param target  the target value
     * @return the new value
     */
    protected static double approach(double current, double target) {
        return MathUtils.lerp(current, target, ANIMATION_SPEED);
    }

    /**
     * @return the height of this component in pixels
     */
    public abstract double getHeight();

    /**
     * Draws this component.
     *
     * @param graphics the render context
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     */
    public abstract void render(GuiGraphics graphics, int mouseX, int mouseY);

    /**
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param button the mouse button
     * @return {@code true} when the click was consumed
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    /**
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param button the mouse button
     * @return {@code true} when the release was consumed
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    /**
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @return {@code true} when the drag was consumed
     */
    public boolean mouseDragged(double mouseX, double mouseY) {
        return false;
    }

    /**
     * @param key       the GLFW key code
     * @param scancode  the platform scancode
     * @param modifiers the modifier bitmask
     * @return {@code true} when the key was consumed
     */
    public boolean keyPressed(int key, int scancode, int modifiers) {
        return false;
    }

    /**
     * @param character the typed character
     * @return {@code true} when the character was consumed
     */
    public boolean charTyped(char character) {
        return false;
    }

    /**
     * @return {@code true} while this component wants exclusive keyboard input, for example while
     *         waiting for a keybind
     */
    public boolean isListening() {
        return false;
    }

    /**
     * Stops any capture in progress.
     */
    public void stopListening() {
    }
}
