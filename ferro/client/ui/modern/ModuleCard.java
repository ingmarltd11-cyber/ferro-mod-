package dev.ferro.client.ui.modern;

import dev.ferro.client.module.Module;
import dev.ferro.client.ui.Icons;
import dev.ferro.client.utils.MathUtils;
import dev.ferro.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A module shown as a card in the modern interface.
 *
 * <p>The card is a pure renderer: the header carries the name, the description, a chevron that opens the
 * settings page and the switch, and nothing else. All input is routed by {@link ModernLayout}, which records
 * the hit boxes while drawing, so the list view, the grid view and the settings page all show the exact same
 * card without a single line of input code living here.</p>
 */
public class ModuleCard {

    /** Height of the card. */
    public static final double HEIGHT = 32.0D;

    /** Corner radius of the card. */
    private static final int RADIUS = 14;
    /** Size of the switch. */
    private static final double SWITCH_WIDTH = 26.0D;
    /** Height of the switch. */
    private static final double SWITCH_HEIGHT = 12.0D;
    /** Inset of the switch from the right edge. */
    private static final double SWITCH_INSET = 12.0D;

    private final Module module;
    private double x;
    private double y;
    private double width;
    private double hoverAnimation;

    /**
     * @param module the module this card shows
     */
    public ModuleCard(Module module) {
        this.module = module;
    }

    /**
     * @return the module behind this card
     */
    public Module getModule() {
        return module;
    }

    /**
     * @param x     the left edge
     * @param y     the top edge
     * @param width the card width
     */
    public void setBounds(double x, double y, double width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    /**
     * @return the left edge
     */
    public double getX() {
        return x;
    }

    /**
     * @return the top edge
     */
    public double getY() {
        return y;
    }

    /**
     * @return the card width
     */
    public double getWidth() {
        return width;
    }

    /**
     * @return the card height
     */
    public double getHeight() {
        return HEIGHT;
    }

    /**
     * @return the left edge of the switch, so the layout can register its hit box
     */
    public double getSwitchX() {
        return x + width - SWITCH_INSET - SWITCH_WIDTH;
    }

    /**
     * @return the top edge of the switch
     */
    public double getSwitchY() {
        return y + (HEIGHT - SWITCH_HEIGHT) / 2.0D;
    }

    /**
     * @return the switch width
     */
    public double getSwitchWidth() {
        return SWITCH_WIDTH;
    }

    /**
     * @return the switch height
     */
    public double getSwitchHeight() {
        return SWITCH_HEIGHT;
    }

    /**
     * Draws the card.
     *
     * @param graphics the render context
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEIGHT;
        hoverAnimation = MathUtils.lerp(hoverAnimation, hovered ? 1.0D : 0.0D, 0.18D);
        // The card lifts a pixel while hovered, which reads as smooth without moving the layout around.
        double lift = hoverAnimation * 1.5D;
        double top = y - lift;

        if (hoverAnimation > 0.02D) {
            RenderUtils.shadow(graphics, x, top, width, HEIGHT, RADIUS, 2, (int) (110.0D * hoverAnimation));
        }
        int base = RenderUtils.lerpColour(RenderUtils.SURFACE, RenderUtils.FRAME, (float) (hoverAnimation * 0.7D));
        RenderUtils.roundedPanel(graphics, x, top, width, HEIGHT, base, RenderUtils.OUTLINE, RADIUS);
        RenderUtils.rounded(graphics, x + RADIUS, top + 1.0D, width - RADIUS * 2.0D, 1.0D,
                RenderUtils.withAlpha(RenderUtils.TEXT, (int) (18.0D + 22.0D * hoverAnimation)), 1);
        if (module.isEnabled()) {
            // The enabled marker sweeps from the primary accent into the secondary one, which is what ties
            // every theme to its two accents.
            RenderUtils.gradient(graphics, x + 1.0D, top + 8.0D, 3.0D, HEIGHT - 16.0D, RenderUtils.ACCENT,
                    RenderUtils.ACCENT_2);
            RenderUtils.rounded(graphics, x + 1.0D, top + 8.0D, 3.0D, 2.0D, RenderUtils.ACCENT, 1);
            RenderUtils.rounded(graphics, x + 1.0D, top + HEIGHT - 10.0D, 3.0D, 2.0D, RenderUtils.ACCENT_2, 1);
        }

        int nameColour = module.isEnabled() ? RenderUtils.TEXT : RenderUtils.TEXT_DISABLED;
        RenderUtils.text(graphics, module.getName(), x + 12.0D, top + 7.0D,
                RenderUtils.lerpColour(RenderUtils.TEXT, RenderUtils.HOVER, (float) hoverAnimation));
        String description = module.getDescription();
        if (!description.isEmpty()) {
            RenderUtils.text(graphics, SettingRow.trim(description, width - 74.0D), x + 12.0D, top + 19.0D,
                    RenderUtils.withAlpha(RenderUtils.TEXT_DISABLED, 150 + (int) (90.0D * hoverAnimation)));
        }

        double chevronX = getSwitchX() - 17.0D;
        Icons.draw(graphics, Icons.Icon.CHEVRON_RIGHT, chevronX, top + 10.0D, 12.0D,
                RenderUtils.withAlpha(RenderUtils.TEXT_DISABLED, 140 + (int) (110.0D * hoverAnimation)));
        SettingRow.switchControl(graphics, getSwitchX(), getSwitchY() - lift, SWITCH_WIDTH, SWITCH_HEIGHT,
                module.isEnabled());
    }
}
