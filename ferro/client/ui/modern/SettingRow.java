package dev.ferro.client.ui.modern;

import dev.ferro.client.module.setting.BooleanSetting;
import dev.ferro.client.module.setting.KeybindSetting;
import dev.ferro.client.module.setting.ModeSetting;
import dev.ferro.client.module.setting.NumberSetting;
import dev.ferro.client.module.setting.Setting;
import dev.ferro.client.module.setting.StringSetting;
import dev.ferro.client.utils.MathUtils;
import dev.ferro.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

/**
 * One setting drawn as a row in the modern interface.
 *
 * <p>The row is split in two columns: the label and its description on the left, the control on the right.
 * Every control has the same shape language: a filled switch, a slider with a boxed value, a dropdown with a
 * chevron, or a segmented Hold/Toggle pair with the key field next to it.</p>
 *
 * <p>The row is not a vanilla widget: {@link ModuleCard} lays it out and forwards the raw input events, the
 * same approach the classic layout uses, so both interfaces can share the settings without sharing rendering
 * code.</p>
 */
public abstract class SettingRow {

    /** Height of a single row. */
    protected static final double ROW_HEIGHT = 28.0D;
    /** Height of a control. */
    protected static final double CONTROL_HEIGHT = 14.0D;
    /** Width of the switch control. */
    protected static final double SWITCH_WIDTH = 26.0D;
    /** Width of the slider track. */
    protected static final double SLIDER_WIDTH = 100.0D;
    /** Width of the boxed value next to a slider. */
    protected static final double VALUE_WIDTH = 46.0D;
    /** Width of a dropdown. */
    protected static final double DROPDOWN_WIDTH = 130.0D;
    /** Width of a text field. */
    protected static final double TEXT_WIDTH = 130.0D;
    /** Width of the key field of a keybind row. */
    protected static final double KEY_WIDTH = 80.0D;
    /** Width of one half of the Hold/Toggle segment. */
    protected static final double SEGMENT_WIDTH = 34.0D;

    private static final int KEY_ESCAPE = 256;
    private static final int KEY_BACKSPACE = 259;

    private final Setting<?> setting;
    private double x;
    private double y;
    private double width;
    private boolean hovered;

    /**
     * @param setting the setting this row edits
     */
    protected SettingRow(Setting<?> setting) {
        this.setting = setting;
    }

    /**
     * Creates the matching row for a setting type.
     *
     * @param setting the setting
     * @return the row, never {@code null}
     */
    public static SettingRow of(Setting<?> setting) {
        if (setting instanceof KeybindSetting keybind) {
            return new KeybindRow(keybind);
        }
        if (setting instanceof BooleanSetting toggle) {
            return new SwitchRow(toggle);
        }
        if (setting instanceof NumberSetting number) {
            return new SliderRow(number);
        }
        if (setting instanceof ModeSetting mode) {
            return new DropdownRow(mode);
        }
        if (setting instanceof StringSetting text) {
            return new TextRow(text);
        }
        if (setting instanceof dev.ferro.client.module.setting.MinMaxSetting range) {
            return new MinMaxRow(range);
        }
        return new TextRow(new StringSetting(setting.getName(), setting.displayValue(), 64));
    }

    /**
     * @return the setting behind this row
     */
    public final Setting<?> getSetting() {
        return setting;
    }

    /**
     * @param x     the left edge
     * @param y     the top edge
     * @param width the row width
     */
    public final void setBounds(double x, double y, double width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    /**
     * @return the height of the row
     */
    public double getHeight() {
        return ROW_HEIGHT;
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
     * @return the right edge of the control column
     */
    protected final double controlRight() {
        return x + width - 6.0D;
    }

    /**
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @return {@code true} when the mouse is inside this row
     */
    protected final boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + getHeight();
    }

    /**
     * Draws the label, the description and the control.
     *
     * @param graphics the render context
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        hovered = isHovered(mouseX, mouseY);
        if (hovered) {
            RenderUtils.rounded(graphics, x, y, width, getHeight(), RenderUtils.withAlpha(RenderUtils.FRAME, 55), 6);
        }
        RenderUtils.text(graphics, setting.getName(), x + 6.0D, y + 3.0D,
                hovered ? RenderUtils.HOVER : RenderUtils.TEXT);
        String description = setting.getDescription();
        if (!description.isEmpty()) {
            RenderUtils.text(graphics, description, x + 6.0D, y + 15.0D, RenderUtils.TEXT_DISABLED);
        }
        renderControl(graphics, mouseX, mouseY);
    }

    /**
     * Draws the control of this row.
     *
     * @param graphics the render context
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     */
    protected abstract void renderControl(GuiGraphics graphics, int mouseX, int mouseY);

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
     * @return {@code true} while this row captures keyboard input
     */
    public boolean isListening() {
        return false;
    }

    /**
     * Stops any capture in progress.
     */
    public void stopListening() {
    }

    // ------------------------------------------------------------------ shared controls

    /**
     * Draws a switch.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param width    the width
     * @param height   the height
     * @param on       whether the switch is on
     */
    protected static void switchControl(GuiGraphics graphics, double x, double y, double width, double height,
                                        boolean on) {
        if (on) {
            RenderUtils.shadow(graphics, x, y, width, height, (int) (height / 2.0D), 2, 90);
            RenderUtils.roundedGradientH(graphics, x, y, width, height, RenderUtils.ACCENT, RenderUtils.ACCENT_2,
                    (int) (height / 2.0D));
        } else {
            RenderUtils.rounded(graphics, x, y, width, height, RenderUtils.withAlpha(RenderUtils.OUTLINE, 235),
                    (int) (height / 2.0D));
        }
        double knob = height - 4.0D;
        double knobX = on ? x + width - knob - 2.0D : x + 2.0D;
        // The off knob is neutral so a theme with a warm muted colour does not make every switch look enabled.
        RenderUtils.rounded(graphics, knobX, y + 2.0D, knob, knob,
                on ? 0xFFFFFFFF : RenderUtils.withAlpha(RenderUtils.TEXT, 190), (int) (knob / 2.0D));
    }

    /**
     * Draws a bordered box with centred text, used for values.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param width    the width
     * @param height   the height
     * @param text     the text
     */
    protected static void boxedText(GuiGraphics graphics, double x, double y, double width, double height,
                                    String text) {
        RenderUtils.roundedPanel(graphics, x, y, width, height, RenderUtils.SURFACE, RenderUtils.OUTLINE, 7);
        RenderUtils.centeredText(graphics, text, x + width / 2.0D, y + (height - RenderUtils.textHeight()) / 2.0D + 1.0D,
                RenderUtils.TEXT);
    }

    /**
     * Draws a slider track with a filled portion and a handle.
     *
     * @param graphics   the render context
     * @param x          the left edge
     * @param y          the top edge
     * @param width      the width
     * @param percentage the filled fraction, {@code 0..1}
     * @param hovered    whether the handle is hovered
     */
    protected static void sliderControl(GuiGraphics graphics, double x, double y, double width, float percentage,
                                        boolean hovered) {
        double centre = y + 1.0D;
        RenderUtils.rounded(graphics, x, centre - 1.5D, width, 3.0D, RenderUtils.withAlpha(RenderUtils.OUTLINE, 200), 1);
        double filled = width * MathUtils.clamp(percentage, 0.0F, 1.0F);
        if (filled > 0.0D) {
            // The filled part sweeps towards the secondary accent as the slider fills up.
            int end = RenderUtils.lerpColour(RenderUtils.ACCENT, RenderUtils.ACCENT_2,
                    (float) MathUtils.clamp(filled / Math.max(1.0D, width), 0.0D, 1.0D));
            RenderUtils.roundedGradientH(graphics, x, centre - 1.5D, filled, 3.0D, RenderUtils.ACCENT, end, 1);
        }
        double handleSize = hovered ? 10.0D : 8.0D;
        RenderUtils.rounded(graphics, x + filled - handleSize / 2.0D, centre - handleSize / 2.0D, handleSize,
                handleSize, hovered ? RenderUtils.HOVER : RenderUtils.ACCENT, (int) (handleSize / 2.0D));
    }

    /**
     * Draws a dropdown button.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param width    the width
     * @param height   the height
     * @param text     the current value
     * @param hovered  whether the mouse is over the button
     */
    protected static void dropdownControl(GuiGraphics graphics, double x, double y, double width, double height,
                                          String text, boolean hovered) {
        RenderUtils.roundedPanel(graphics, x, y, width, height, RenderUtils.SURFACE,
                hovered ? RenderUtils.HOVER : RenderUtils.OUTLINE, 7);
        String label = trim(text, width - 24.0D);
        RenderUtils.text(graphics, label, x + 6.0D, y + (height - RenderUtils.textHeight()) / 2.0D + 1.0D,
                RenderUtils.TEXT);
        RenderUtils.text(graphics, "v", x + width - 10.0D, y + (height - RenderUtils.textHeight()) / 2.0D + 1.0D,
                hovered ? RenderUtils.HOVER : RenderUtils.TEXT_DISABLED);
    }

    /**
     * Truncates a label so it fits a control.
     *
     * @param text  the label
     * @param width the available width
     * @return the label, shortened with an ellipsis when needed
     */
    protected static String trim(String text, double width) {
        if (RenderUtils.textWidth(text) <= width) {
            return text;
        }
        String result = text;
        while (result.length() > 1 && RenderUtils.textWidth(result + "..") > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "..";
    }

    /**
     * Formats a number for the value box.
     *
     * @param value   the value
     * @param integer whether the setting only produces whole numbers
     * @return the formatted value
     */
    protected static String format(double value, boolean integer) {
        if (integer) {
            return Integer.toString((int) Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    // ------------------------------------------------------------------ row types

    /**
     * A boolean row: label on the left, switch on the right.
     */
    protected static final class SwitchRow extends SettingRow {

        private final BooleanSetting setting;

        /**
         * @param setting the boolean setting
         */
        SwitchRow(BooleanSetting setting) {
            super(setting);
            this.setting = setting;
        }

        @Override
        protected void renderControl(GuiGraphics graphics, int mouseX, int mouseY) {
            switchControl(graphics, controlRight() - SWITCH_WIDTH, getY() + 8.0D, SWITCH_WIDTH, 12.0D,
                    setting.get());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            double left = controlRight() - SWITCH_WIDTH;
            if (mouseX >= left - 3.0D && mouseX <= controlRight() + 3.0D && mouseY >= getY() + 5.0D
                    && mouseY <= getY() + 23.0D) {
                setting.set(!setting.get());
                return true;
            }
            return false;
        }
    }

    /**
     * A range row: one slider with two handles, and a boxed {@code low – high} value next to it.
     */
    protected static final class MinMaxRow extends SettingRow {

        private final dev.ferro.client.module.setting.MinMaxSetting setting;
        private boolean draggingLow;
        private boolean draggingHigh;

        /**
         * @param setting the range setting
         */
        MinMaxRow(dev.ferro.client.module.setting.MinMaxSetting setting) {
            super(setting);
            this.setting = setting;
        }

        /**
         * @return the left edge of the slider track
         */
        private double trackLeft() {
            return controlRight() - VALUE_WIDTH - 8.0D - SLIDER_WIDTH;
        }

        /**
         * @param value the value
         * @return the x position of the handle for that value
         */
        private double handleX(double value) {
            double span = Math.max(0.0001D, setting.getMax() - setting.getMin());
            double percentage = MathUtils.clamp((value - setting.getMin()) / span, 0.0D, 1.0D);
            return trackLeft() + SLIDER_WIDTH * percentage;
        }

        @Override
        protected void renderControl(GuiGraphics graphics, int mouseX, int mouseY) {
            double left = trackLeft();
            double centre = getY() + 14.0D;
            boolean hovered = mouseX >= left - 6.0D && mouseX <= left + SLIDER_WIDTH + 6.0D
                    && mouseY >= getY() + 4.0D && mouseY <= getY() + 22.0D;
            RenderUtils.rounded(graphics, left, centre - 1.5D, SLIDER_WIDTH, 3.0D,
                    RenderUtils.withAlpha(RenderUtils.OUTLINE, 200), 1);
            double lowX = handleX(setting.getLow());
            double highX = handleX(setting.getHigh());
            if (highX - lowX > 0.5D) {
                RenderUtils.roundedGradientH(graphics, lowX, centre - 1.5D, highX - lowX, 3.0D, RenderUtils.ACCENT,
                        RenderUtils.ACCENT_2, 1);
            }
            double size = hovered || draggingLow || draggingHigh ? 10.0D : 8.0D;
            RenderUtils.rounded(graphics, lowX - size / 2.0D, centre - size / 2.0D, size, size, RenderUtils.ACCENT, 4);
            RenderUtils.rounded(graphics, highX - size / 2.0D, centre - size / 2.0D, size, size, RenderUtils.ACCENT_2,
                    4);
            boxedText(graphics, controlRight() - VALUE_WIDTH, getY() + 6.0D, VALUE_WIDTH, CONTROL_HEIGHT,
                    setting.displayValue());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            double left = trackLeft();
            if (mouseX < left - 6.0D || mouseX > left + SLIDER_WIDTH + 6.0D || mouseY < getY() + 2.0D
                    || mouseY > getY() + 24.0D) {
                return false;
            }
            // Whichever end is closer is the one that follows the mouse.
            if (Math.abs(mouseX - handleX(setting.getLow())) <= Math.abs(mouseX - handleX(setting.getHigh()))) {
                draggingLow = true;
                apply(mouseX, true);
            } else {
                draggingHigh = true;
                apply(mouseX, false);
            }
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY) {
            if (!draggingLow && !draggingHigh) {
                return false;
            }
            apply(mouseX, draggingLow);
            return true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (!draggingLow && !draggingHigh) {
                return false;
            }
            draggingLow = false;
            draggingHigh = false;
            return true;
        }

        /**
         * @param mouseX the mouse x position
         * @param low    whether the low end is being dragged
         */
        private void apply(double mouseX, boolean low) {
            double percentage = MathUtils.clamp((mouseX - trackLeft()) / SLIDER_WIDTH, 0.0D, 1.0D);
            double value = setting.getMin() + (setting.getMax() - setting.getMin()) * percentage;
            if (low) {
                setting.setLow(value);
            } else {
                setting.setHigh(value);
            }
        }
    }

    /**
     * A number row: label on the left, slider with a boxed value on the right.
     */
    protected static final class SliderRow extends SettingRow {

        private final NumberSetting setting;
        private boolean dragging;

        /**
         * @param setting the number setting
         */
        SliderRow(NumberSetting setting) {
            super(setting);
            this.setting = setting;
        }

        /**
         * @return the left edge of the slider track
         */
        private double trackLeft() {
            return controlRight() - VALUE_WIDTH - 8.0D - SLIDER_WIDTH;
        }

        @Override
        protected void renderControl(GuiGraphics graphics, int mouseX, int mouseY) {
            double trackLeft = trackLeft();
            double trackY = getY() + 13.0D;
            boolean hovered = mouseX >= trackLeft - 6.0D && mouseX <= trackLeft + SLIDER_WIDTH + 6.0D
                    && mouseY >= getY() + 4.0D && mouseY <= getY() + 22.0D;
            sliderControl(graphics, trackLeft, trackY, SLIDER_WIDTH, setting.getPercentage(), hovered || dragging);
            boxedText(graphics, controlRight() - VALUE_WIDTH, getY() + 6.0D, VALUE_WIDTH, CONTROL_HEIGHT,
                    format(setting.getDouble(), setting.isInteger()));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            double trackLeft = trackLeft();
            if (mouseX >= trackLeft - 6.0D && mouseX <= trackLeft + SLIDER_WIDTH + 6.0D && mouseY >= getY() + 2.0D
                    && mouseY <= getY() + 24.0D) {
                dragging = true;
                apply(mouseX);
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY) {
            if (!dragging) {
                return false;
            }
            apply(mouseX);
            return true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (!dragging) {
                return false;
            }
            dragging = false;
            return true;
        }

        /**
         * @param mouseX the mouse x position
         */
        private void apply(double mouseX) {
            double percentage = (mouseX - trackLeft()) / SLIDER_WIDTH;
            setting.setPercentage(percentage);
        }
    }

    /**
     * A mode row: label on the left, dropdown with a chevron on the right.
     */
    protected static final class DropdownRow extends SettingRow {

        private final ModeSetting setting;

        /**
         * @param setting the mode setting
         */
        DropdownRow(ModeSetting setting) {
            super(setting);
            this.setting = setting;
        }

        @Override
        protected void renderControl(GuiGraphics graphics, int mouseX, int mouseY) {
            double left = controlRight() - DROPDOWN_WIDTH;
            boolean hovered = mouseX >= left && mouseX <= controlRight() && mouseY >= getY() + 5.0D
                    && mouseY <= getY() + 23.0D;
            dropdownControl(graphics, left, getY() + 7.0D, DROPDOWN_WIDTH, CONTROL_HEIGHT, setting.get(), hovered);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            double left = controlRight() - DROPDOWN_WIDTH;
            if (mouseX >= left && mouseX <= controlRight() && mouseY >= getY() + 5.0D && mouseY <= getY() + 23.0D) {
                if (button == 1) {
                    setting.previous();
                } else {
                    setting.next();
                }
                return true;
            }
            return false;
        }
    }

    /**
     * A keybind row: label on the left, Hold/Toggle segments and the key field on the right.
     */
    protected static final class KeybindRow extends SettingRow {

        private final KeybindSetting setting;
        private boolean listening;

        /**
         * @param setting the keybind setting
         */
        KeybindRow(KeybindSetting setting) {
            super(setting);
            this.setting = setting;
        }

        @Override
        protected void renderControl(GuiGraphics graphics, int mouseX, int mouseY) {
            double keyLeft = controlRight() - KEY_WIDTH;
            double toggleLeft = keyLeft - 6.0D - SEGMENT_WIDTH;
            double holdLeft = toggleLeft - SEGMENT_WIDTH;
            renderSegment(graphics, holdLeft, "Hold", setting.isHold(), mouseX, mouseY);
            renderSegment(graphics, toggleLeft, "Toggle", !setting.isHold(), mouseX, mouseY);
            boolean hovered = mouseX >= keyLeft && mouseX <= controlRight() && mouseY >= getY() + 6.0D
                    && mouseY <= getY() + 22.0D;
            String label = listening ? "..." : setting.getKeyName();
            RenderUtils.roundedPanel(graphics, keyLeft, getY() + 7.0D, KEY_WIDTH, CONTROL_HEIGHT,
                    RenderUtils.SURFACE, listening || hovered ? RenderUtils.HOVER : RenderUtils.OUTLINE, 7);
            RenderUtils.centeredText(graphics, trim(label, KEY_WIDTH - 8.0D), keyLeft + KEY_WIDTH / 2.0D,
                    getY() + 10.0D, listening ? RenderUtils.HOVER : RenderUtils.TEXT);
        }

        /**
         * Draws one half of the Hold/Toggle segment.
         *
         * @param graphics the render context
         * @param x        the left edge
         * @param label    the label
         * @param active   whether this half is selected
         * @param mouseX   the mouse x position
         * @param mouseY   the mouse y position
         */
        private void renderSegment(GuiGraphics graphics, double x, String label, boolean active, int mouseX,
                                   int mouseY) {
            boolean hovered = mouseX >= x && mouseX <= x + SEGMENT_WIDTH && mouseY >= getY() + 7.0D
                    && mouseY <= getY() + 21.0D;
            int fill = active ? RenderUtils.withAlpha(RenderUtils.ACCENT, 90) : RenderUtils.SURFACE;
            RenderUtils.roundedPanel(graphics, x, getY() + 7.0D, SEGMENT_WIDTH, CONTROL_HEIGHT, fill,
                    active || hovered ? RenderUtils.HOVER : RenderUtils.OUTLINE, 7);
            RenderUtils.centeredText(graphics, label, x + SEGMENT_WIDTH / 2.0D, getY() + 10.0D,
                    active ? RenderUtils.TEXT : RenderUtils.TEXT_DISABLED);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            double keyLeft = controlRight() - KEY_WIDTH;
            double toggleLeft = keyLeft - 6.0D - SEGMENT_WIDTH;
            double holdLeft = toggleLeft - SEGMENT_WIDTH;
            if (mouseX >= holdLeft && mouseX <= holdLeft + SEGMENT_WIDTH && mouseY >= getY() + 7.0D
                    && mouseY <= getY() + 21.0D) {
                setting.setHold(true);
                return true;
            }
            if (mouseX >= toggleLeft && mouseX <= toggleLeft + SEGMENT_WIDTH && mouseY >= getY() + 7.0D
                    && mouseY <= getY() + 21.0D) {
                setting.setHold(false);
                return true;
            }
            if (mouseX >= keyLeft && mouseX <= controlRight() && mouseY >= getY() + 7.0D
                    && mouseY <= getY() + 21.0D) {
                if (listening) {
                    setting.set(KeybindSetting.mouseButton(button));
                    listening = false;
                } else if (button == 1) {
                    setting.set(KeybindSetting.NONE);
                } else {
                    listening = true;
                }
                return true;
            }
            return false;
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

    /**
     * A text row: label on the left, editable field on the right.
     */
    protected static final class TextRow extends SettingRow {

        private final StringSetting setting;
        private boolean focused;

        /**
         * @param setting the string setting
         */
        TextRow(StringSetting setting) {
            super(setting);
            this.setting = setting;
        }

        @Override
        protected void renderControl(GuiGraphics graphics, int mouseX, int mouseY) {
            double left = controlRight() - TEXT_WIDTH;
            boolean hovered = mouseX >= left && mouseX <= controlRight() && mouseY >= getY() + 6.0D
                    && mouseY <= getY() + 22.0D;
            RenderUtils.roundedPanel(graphics, left, getY() + 7.0D, TEXT_WIDTH, CONTROL_HEIGHT, RenderUtils.SURFACE,
                    focused || hovered ? RenderUtils.HOVER : RenderUtils.OUTLINE, 7);
            String value = setting.get().isEmpty() ? "-" : setting.get();
            RenderUtils.text(graphics, trim(value, TEXT_WIDTH - 12.0D), left + 5.0D, getY() + 10.0D,
                    setting.get().isEmpty() ? RenderUtils.TEXT_DISABLED : RenderUtils.TEXT);
            if (focused) {
                RenderUtils.rect(graphics, left + 4.0D, getY() + 8.0D, TEXT_WIDTH - 8.0D, 12.0D,
                        RenderUtils.withAlpha(RenderUtils.ACCENT, 40));
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            double left = controlRight() - TEXT_WIDTH;
            if (mouseX >= left && mouseX <= controlRight() && mouseY >= getY() + 6.0D && mouseY <= getY() + 22.0D) {
                focused = true;
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
            if (key == KEY_ESCAPE) {
                focused = false;
                return true;
            }
            if (key == KEY_BACKSPACE) {
                String value = setting.get();
                if (!value.isEmpty()) {
                    setting.set(value.substring(0, value.length() - 1));
                }
                return true;
            }
            return true;
        }

        @Override
        public boolean charTyped(char character) {
            if (!focused || character < ' ' || character == 127) {
                return false;
            }
            setting.set(setting.get() + character);
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
}
