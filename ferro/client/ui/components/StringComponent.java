
package dev.ferro.client.ui.components;

import dev.ferro.client.module.setting.StringSetting;
import dev.ferro.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Text field for a {@link StringSetting}. Click to focus, type to append, backspace to delete, enter
 * or escape to unfocus.
 */
public class StringComponent extends Component {

    private static final int KEY_ESCAPE = 256;
    private static final int KEY_ENTER = 257;
    private static final int KEY_BACKSPACE = 259;

    private final StringSetting setting;
    private boolean focused;
    private String buffer;

    /**
     * @param setting the setting
     */
    public StringComponent(StringSetting setting) {
        super(setting);
        this.setting = setting;
        this.buffer = setting.get();
    }

    @Override
    public double getHeight() {
        return ROW_HEIGHT + 4.0D;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        updateHovered(mouseX, mouseY);
        RenderUtils.text(graphics, setting.getName(), getX() + 3.0D, getY() + 1.0D,
                wasHovered() || focused ? RenderUtils.HOVER : RenderUtils.TEXT);
        String shown = focused ? buffer + "_" : setting.get();
        if (RenderUtils.textWidth(shown) > getWidth() - 6.0D) {
            while (shown.length() > 1 && RenderUtils.textWidth("..." + shown) > getWidth() - 6.0D) {
                shown = shown.substring(1);
            }
            shown = "..." + shown;
        }
        RenderUtils.text(graphics, shown, getX() + 3.0D, getY() + 11.0D, RenderUtils.TEXT_DISABLED);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY)) {
            focused = true;
            buffer = setting.get();
            return true;
        }
        focused = false;
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (!focused) {
            return false;
        }
        if (key == KEY_ESCAPE || key == KEY_ENTER) {
            focused = false;
            return true;
        }
        if (key == KEY_BACKSPACE) {
            if (!buffer.isEmpty()) {
                buffer = buffer.substring(0, buffer.length() - 1);
            }
            setting.set(buffer);
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char character) {
        if (!focused) {
            return false;
        }
        if (buffer.length() >= setting.getMaxLength()) {
            return true;
        }
        buffer = buffer + character;
        setting.set(buffer);
        return true;
    }

    @Override
    public boolean isListening() {
        return focused;
    }

    @Override
    public void stopListening() {
        focused = false;
    }
}
