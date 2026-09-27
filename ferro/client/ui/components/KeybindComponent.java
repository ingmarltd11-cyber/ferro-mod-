
package dev.ferro.client.ui.components;

import dev.ferro.client.module.setting.KeybindSetting;
import dev.ferro.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Keybind field. Clicking it starts a capture: the next key press or mouse button that arrives is
 * stored, escape aborts and backspace clears the binding.
 */
public class KeybindComponent extends Component {

    private static final int KEY_ESCAPE = 256;
    private static final int KEY_BACKSPACE = 259;

    private final KeybindSetting setting;
    private boolean listening;

    /**
     * @param setting the setting
     */
    public KeybindComponent(KeybindSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Override
    public double getHeight() {
        return ROW_HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        updateHovered(mouseX, mouseY);
        RenderUtils.text(graphics, setting.getName(), getX() + 3.0D, getY() + 2.0D,
                wasHovered() || listening ? RenderUtils.HOVER : RenderUtils.TEXT);
        String label = listening ? "..." : setting.getKeyName();
        double boxWidth = Math.max(28.0D, RenderUtils.textWidth(label) + 8.0D);
        double boxX = getX() + getWidth() - boxWidth - 3.0D;
        int border = listening ? RenderUtils.HOVER : RenderUtils.OUTLINE;
        RenderUtils.panel(graphics, boxX, getY() + 1.0D, boxWidth, 10.0D, RenderUtils.BACKGROUND, border);
        RenderUtils.centeredText(graphics, label, boxX + boxWidth / 2.0D, getY() + 2.0D,
                listening ? RenderUtils.HOVER : RenderUtils.TEXT_DISABLED);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) {
            listening = false;
            return false;
        }
        if (listening) {
            setting.set(KeybindSetting.mouseButton(button));
            listening = false;
            return true;
        }
        listening = true;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (!listening) {
            return false;
        }
        if (key == KEY_ESCAPE) {
            listening = false;
            return true;
        }
        if (key == KEY_BACKSPACE) {
            setting.set(KeybindSetting.NONE);
            listening = false;
            return true;
        }
        setting.set(key);
        listening = false;
        return true;
    }

    @Override
    public boolean isListening() {
        return listening;
    }

    @Override
    public void stopListening() {
        listening = false;
    }
}
