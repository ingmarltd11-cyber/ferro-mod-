
package dev.ferro.client.ui.components;

import dev.ferro.client.module.Module;
import dev.ferro.client.module.setting.Setting;
import dev.ferro.client.utils.Notifications;
import dev.ferro.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * One module row in a {@link Frame}: the name on the left, a toggle pill on the right.
 *
 * <p>A left click toggles the module, a right click expands the settings. The keybind field is always
 * the first setting row, followed by the module's own settings in registration order.</p>
 */
public class ModuleButton {

    private static final double ROW_HEIGHT = 14.0D;

    private final Module module;
    private final List<Component> components = new ArrayList<>();

    private double x;
    private double y;
    private double width;
    private boolean expanded;
    private double expandAnimation;
    private double hoverAnimation;

    /**
     * @param module the module this button controls
     */
    public ModuleButton(Module module) {
        this.module = module;
        this.components.add(new KeybindComponent(module.getKeybind()));
        for (Setting<?> setting : module.getSettings()) {
            this.components.add(Component.of(setting));
        }
    }

    /**
     * @return the module behind this button
     */
    public Module getModule() {
        return module;
    }

    /**
     * @return {@code true} when the settings panel is expanded
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * @param value whether the settings panel should be expanded
     */
    public void setExpanded(boolean value) {
        this.expanded = value;
    }

    /**
     * @param x     the left edge
     * @param y     the top edge
     * @param width the width
     */
    public void setBounds(double x, double y, double width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    /**
     * @return the top edge of this button
     */
    public double getY() {
        return y;
    }

    /**
     * @return the height of the visible settings content
     */
    private double contentHeight() {
        double height = 0.0D;
        for (Component component : components) {
            if (component.getSetting().isVisible()) {
                height += component.getHeight() + 1.0D;
            }
        }
        return height;
    }

    /**
     * @return the current height, including the animated settings panel
     */
    public double getHeight() {
        return ROW_HEIGHT + expandAnimation * contentHeight();
    }

    /**
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @return {@code true} when the mouse is over the module row itself
     */
    private boolean isRowHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + ROW_HEIGHT;
    }

    /**
     * @return {@code true} when any child component is capturing keyboard input
     */
    public boolean isListening() {
        for (Component component : components) {
            if (component.isListening()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Draws the button and, when expanded, its settings.
     *
     * @param graphics the render context
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        expandAnimation = approach(expandAnimation, expanded ? 1.0D : 0.0D);
        hoverAnimation = approach(hoverAnimation, isRowHovered(mouseX, mouseY) ? 1.0D : 0.0D);

        int background = RenderUtils.lerpColour(RenderUtils.BACKGROUND, RenderUtils.FRAME, (float) hoverAnimation);
        RenderUtils.rounded(graphics, x, y, width, ROW_HEIGHT, background, 6);
        if (module.isEnabled()) {
            RenderUtils.rounded(graphics, x + 1.0D, y + 2.0D, 2.5D, ROW_HEIGHT - 4.0D, RenderUtils.ACCENT, 1);
        }
        int textColour = module.isEnabled() ? RenderUtils.TEXT : RenderUtils.TEXT_DISABLED;
        if (hoverAnimation > 0.5D) {
            textColour = RenderUtils.HOVER;
        }
        RenderUtils.text(graphics, module.getName(), x + 5.0D, y + 3.0D, textColour);

        double pillWidth = 14.0D;
        double pillHeight = 7.0D;
        double pillX = x + width - pillWidth - 4.0D;
        double pillY = y + 4.0D;
        int pillColour = module.isEnabled() ? RenderUtils.ACCENT : RenderUtils.withAlpha(RenderUtils.OUTLINE, 235);
        RenderUtils.rounded(graphics, pillX, pillY, pillWidth, pillHeight, RenderUtils.withAlpha(pillColour, 150),
                (int) (pillHeight / 2.0D));
        double knobX = module.isEnabled() ? pillX + pillWidth - 6.0D : pillX + 2.0D;
        RenderUtils.rounded(graphics, knobX, pillY + 1.0D, 4.0D, pillHeight - 2.0D,
                module.isEnabled() ? 0xFFFFFFFF : pillColour, 2);
        if (expanded && expandAnimation < 0.99D || !expanded && expandAnimation > 0.01D) {
            RenderUtils.rounded(graphics, x + 2.0D, y + ROW_HEIGHT, width - 4.0D, 1.0D, RenderUtils.OUTLINE, 1);
        }
        if (expandAnimation <= 0.01D) {
            return;
        }

        double clipHeight = expandAnimation * contentHeight();
        RenderUtils.scissor(graphics, x, y + ROW_HEIGHT, width, clipHeight);
        double cursor = y + ROW_HEIGHT + 1.0D;
        for (Component component : components) {
            if (!component.getSetting().isVisible()) {
                continue;
            }
            component.setBounds(x + 2.0D, cursor, width - 4.0D);
            component.render(graphics, mouseX, mouseY);
            cursor += component.getHeight() + 1.0D;
        }
        RenderUtils.resetScissor(graphics);
    }

    /**
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param button the mouse button
     * @return {@code true} when the click was consumed
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (expanded) {
            for (Component component : components) {
                if (!component.getSetting().isVisible()) {
                    continue;
                }
                if (component.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        if (!isRowHovered(mouseX, mouseY)) {
            return false;
        }
        if (button == 1) {
            expanded = !expanded;
            return true;
        }
        if (button == 0) {
            module.toggle();
            Notifications.toggle(module.getName(), module.isEnabled());
            return true;
        }
        return false;
    }

    /**
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param button the mouse button
     * @return {@code true} when the release was consumed
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Component component : components) {
            if (component.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @return {@code true} when the drag was consumed
     */
    public boolean mouseDragged(double mouseX, double mouseY) {
        if (!expanded) {
            return false;
        }
        for (Component component : components) {
            if (!component.getSetting().isVisible()) {
                continue;
            }
            if (component.mouseDragged(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param key       the GLFW key code
     * @param scancode  the platform scancode
     * @param modifiers the modifier bitmask
     * @return {@code true} when the key was consumed
     */
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (!expanded) {
            return false;
        }
        for (Component component : components) {
            if (component.keyPressed(key, scancode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param character the typed character
     * @return {@code true} when the character was consumed
     */
    public boolean charTyped(char character) {
        if (!expanded) {
            return false;
        }
        for (Component component : components) {
            if (component.charTyped(character)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stops every component from capturing input.
     */
    public void stopListening() {
        for (Component component : components) {
            component.stopListening();
        }
    }

    /**
     * Eases a value towards a target.
     *
     * @param current the current value
     * @param target  the target value
     * @return the new value
     */
    private static double approach(double current, double target) {
        return dev.ferro.client.utils.MathUtils.lerp(current, target, 0.35D);
    }
}
