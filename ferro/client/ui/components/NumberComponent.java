
package dev.ferro.client.ui.components;

import dev.ferro.client.module.setting.NumberSetting;
import dev.ferro.client.utils.MathUtils;
import dev.ferro.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Slider for a {@link NumberSetting}. The rendered value eases towards the real value so that
 * dragging does not look like it snaps.
 */
public class NumberComponent extends Component {

    private final NumberSetting setting;
    private double renderedPercentage;

    /**
     * @param setting the setting
     */
    public NumberComponent(NumberSetting setting) {
        super(setting);
        this.setting = setting;
        this.renderedPercentage = setting.getPercentage();
    }

    @Override
    public double getHeight() {
        return ROW_HEIGHT + 5.0D;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        updateHovered(mouseX, mouseY);
        renderedPercentage = approach(renderedPercentage, setting.getPercentage());
        int textColour = wasHovered() ? RenderUtils.HOVER : RenderUtils.TEXT;
        RenderUtils.text(graphics, setting.getName(), getX() + 3.0D, getY() + 1.0D, textColour);
        String value = setting.displayValue();
        RenderUtils.text(graphics, value, getX() + getWidth() - RenderUtils.textWidth(value) - 4.0D, getY() + 1.0D,
                RenderUtils.ACCENT);
        double barY = getY() + ROW_HEIGHT - 1.0D;
        double barWidth = getWidth() - 8.0D;
        double barX = getX() + 4.0D;
        RenderUtils.rect(graphics, barX, barY, barWidth, 2.0D, RenderUtils.OUTLINE);
        RenderUtils.rect(graphics, barX, barY, barWidth * MathUtils.clamp(renderedPercentage, 0.0D, 1.0D), 2.0D,
                RenderUtils.ACCENT);
        double knobX = barX + barWidth * MathUtils.clamp(renderedPercentage, 0.0D, 1.0D);
        RenderUtils.rect(graphics, knobX - 1.5D, barY - 2.0D, 3.0D, 6.0D, RenderUtils.HOVER);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            apply(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY) {
        if (isHovered(mouseX, mouseY) || (mouseY >= getY() - 6.0D && mouseY <= getY() + getHeight() + 6.0D)) {
            apply(mouseX);
            return true;
        }
        return false;
    }

    /**
     * Maps a mouse x position onto the slider range.
     *
     * @param mouseX the mouse x position
     */
    private void apply(double mouseX) {
        double barX = getX() + 4.0D;
        double barWidth = getWidth() - 8.0D;
        if (barWidth <= 0.0D) {
            return;
        }
        setting.setPercentage((mouseX - barX) / barWidth);
    }
}
