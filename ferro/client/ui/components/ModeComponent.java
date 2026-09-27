
package dev.ferro.client.ui.components;

import dev.ferro.client.module.setting.ModeSetting;
import dev.ferro.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Dropdown for a {@link ModeSetting}. Left and right arrow zones select the previous and next mode,
 * clicking the label cycles forward.
 */
public class ModeComponent extends Component {

    private final ModeSetting setting;

    /**
     * @param setting the setting
     */
    public ModeComponent(ModeSetting setting) {
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
                wasHovered() ? RenderUtils.HOVER : RenderUtils.TEXT);
        String value = setting.get();
        double boxWidth = RenderUtils.textWidth(value) + 16.0D;
        double boxX = getX() + getWidth() - boxWidth - 3.0D;
        RenderUtils.panel(graphics, boxX, getY() + 1.0D, boxWidth, 10.0D, RenderUtils.BACKGROUND, RenderUtils.OUTLINE);
        RenderUtils.text(graphics, "<", boxX + 3.0D, getY() + 2.0D, RenderUtils.ACCENT);
        RenderUtils.text(graphics, ">", boxX + boxWidth - 7.0D, getY() + 2.0D, RenderUtils.ACCENT);
        RenderUtils.text(graphics, value, boxX + 9.0D, getY() + 2.0D, RenderUtils.TEXT_DISABLED);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) {
            return false;
        }
        String value = setting.get();
        double boxWidth = RenderUtils.textWidth(value) + 16.0D;
        double boxX = getX() + getWidth() - boxWidth - 3.0D;
        if (mouseX <= boxX + 5.0D) {
            setting.previous();
            return true;
        }
        if (mouseX >= boxX + boxWidth - 13.0D) {
            setting.next();
            return true;
        }
        setting.next();
        return true;
    }
}
