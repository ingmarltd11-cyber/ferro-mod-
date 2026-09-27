
package dev.ferro.client.ui.components;

import dev.ferro.client.module.setting.BooleanSetting;
import dev.ferro.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Checkbox for a {@link BooleanSetting}: filled red when enabled, grey when disabled.
 */
public class BooleanComponent extends Component {

    private final BooleanSetting setting;
    private double checkAnimation;

    /**
     * @param setting the setting
     */
    public BooleanComponent(BooleanSetting setting) {
        super(setting);
        this.setting = setting;
        this.checkAnimation = setting.get() ? 1.0D : 0.0D;
    }

    @Override
    public double getHeight() {
        return ROW_HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        updateHovered(mouseX, mouseY);
        checkAnimation = approach(checkAnimation, setting.get() ? 1.0D : 0.0D);
        int textColour = setting.get() ? RenderUtils.TEXT : RenderUtils.TEXT_DISABLED;
        if (wasHovered()) {
            textColour = RenderUtils.HOVER;
        }
        RenderUtils.text(graphics, setting.getName(), getX() + 3.0D, getY() + 2.0D, textColour);
        double boxSize = 7.0D;
        double boxX = getX() + getWidth() - boxSize - 4.0D;
        double boxY = getY() + 3.0D;
        RenderUtils.rect(graphics, boxX, boxY, boxSize, boxSize, RenderUtils.BACKGROUND);
        RenderUtils.outline(graphics, boxX, boxY, boxSize, boxSize,
                setting.get() ? RenderUtils.ACCENT : RenderUtils.OUTLINE, 1.0D);
        if (checkAnimation > 0.01D) {
            double inset = 2.0D * (1.0D - checkAnimation);
            int colour = RenderUtils.withAlpha(RenderUtils.ACCENT, (int) (255 * checkAnimation));
            RenderUtils.rect(graphics, boxX + 1.0D + inset, boxY + 1.0D + inset,
                    boxSize - 2.0D - inset * 2.0D, boxSize - 2.0D - inset * 2.0D, colour);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            setting.toggle();
            return true;
        }
        return false;
    }
}
