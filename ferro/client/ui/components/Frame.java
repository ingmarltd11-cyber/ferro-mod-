
package dev.ferro.client.ui.components;

import com.google.gson.JsonObject;
import dev.ferro.client.Argon;
import dev.ferro.client.module.Category;
import dev.ferro.client.module.Module;
import dev.ferro.client.utils.MathUtils;
import dev.ferro.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * A draggable, collapsible column of {@link ModuleButton}s for one {@link Category}.
 *
 * <p>Position, open state and scroll offset survive a restart because they are written into the
 * {@code ui} section of the FERRO config file.</p>
 */
public class Frame {

    private static final double HEADER_HEIGHT = 15.0D;
    /** Width of a frame. */
    public static final double WIDTH = 120.0D;

    private final Category category;
    private final List<ModuleButton> buttons = new ArrayList<>();

    private double x;
    private double y;
    private boolean open = true;
    private double openAnimation = 1.0D;
    private double headerHover;
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private double scroll;
    private double scrollTarget;
    private double clipHeight;

    /**
     * @param category the category shown in this frame
     * @param x        the initial x position
     * @param y        the initial y position
     */
    public Frame(Category category, double x, double y) {
        this.category = category;
        this.x = x;
        this.y = y;
        if (Argon.get() != null) {
            for (Module module : Argon.get().getModuleManager().getModules(category)) {
                buttons.add(new ModuleButton(module));
            }
        }
    }

    /**
     * @return the category of this frame
     */
    public Category getCategory() {
        return category;
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
     * @return whether the frame is expanded
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * @return the height of the whole frame
     */
    public double getHeight() {
        return HEADER_HEIGHT + openAnimation * contentHeight();
    }

    /**
     * @return the total height of all module rows
     */
    private double contentHeight() {
        double height = 0.0D;
        for (ModuleButton button : buttons) {
            height += button.getHeight();
        }
        return height + 2.0D;
    }

    /**
     * @return the viewport height available for the module list
     */
    private double viewportHeight() {
        return Math.max(HEADER_HEIGHT, RenderUtils.screenHeight() - y - HEADER_HEIGHT - 6.0D);
    }

    /**
     * @return {@code true} when the module list needs clipping
     */
    private boolean needsClipping() {
        return contentHeight() > viewportHeight();
    }

    /**
     * Draws the frame.
     *
     * @param graphics the render context
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        openAnimation = MathUtils.lerp(openAnimation, open ? 1.0D : 0.0D, 0.35D);
        scroll = MathUtils.lerp(scroll, scrollTarget, 0.35D);
        boolean headerHovered = mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + HEADER_HEIGHT;
        headerHover = MathUtils.lerp(headerHover, headerHovered ? 1.0D : 0.0D, 0.35D);

        double height = getHeight() + 1.0D;
        RenderUtils.shadow(graphics, x, y, WIDTH, height, 10, 2, 110);
        RenderUtils.roundedPanel(graphics, x, y, WIDTH, height, RenderUtils.BACKGROUND, RenderUtils.OUTLINE, 10);
        int headerColour = RenderUtils.lerpColour(RenderUtils.FRAME, RenderUtils.HOVER, (float) (headerHover * 0.6D));
        RenderUtils.rounded(graphics, x + 2.0D, y + 2.0D, WIDTH - 4.0D, HEADER_HEIGHT - 1.0D, headerColour, 8);
        RenderUtils.text(graphics, category.getDisplayName().toUpperCase(java.util.Locale.ROOT), x + 5.0D, y + 4.0D,
                RenderUtils.TEXT);
        RenderUtils.text(graphics, open ? "-" : "+", x + WIDTH - 9.0D, y + 4.0D, RenderUtils.TEXT_DISABLED);

        if (openAnimation <= 0.01D) {
            return;
        }
        boolean clip = needsClipping();
        clipHeight = Math.min(contentHeight(), viewportHeight());
        if (clip) {
            RenderUtils.scissor(graphics, x, y + HEADER_HEIGHT, WIDTH, clipHeight * openAnimation);
        }
        double cursor = y + HEADER_HEIGHT - scroll;
        for (ModuleButton button : buttons) {
            button.setBounds(x + 1.0D, cursor, WIDTH - 2.0D);
            button.render(graphics, mouseX, mouseY);
            cursor += button.getHeight();
        }
        if (clip) {
            RenderUtils.resetScissor(graphics);
            if (contentHeight() > clipHeight) {
                double barHeight = Math.max(12.0D, clipHeight * (clipHeight / contentHeight()));
                double maxScroll = contentHeight() - clipHeight;
                double barY = y + HEADER_HEIGHT + (maxScroll <= 0.0D ? 0.0D : (scroll / maxScroll) * (clipHeight - barHeight));
                RenderUtils.rect(graphics, x + WIDTH - 2.0D, barY, 1.5D, barHeight, RenderUtils.ACCENT);
            }
        }
    }

    /**
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param button the mouse button
     * @return {@code true} when the click was consumed
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean inside = mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + getHeight();
        if (!inside) {
            return false;
        }
        if (mouseY <= y + HEADER_HEIGHT) {
            if (button == 0) {
                dragging = true;
                dragOffsetX = mouseX - x;
                dragOffsetY = mouseY - y;
                return true;
            }
            if (button == 1) {
                open = !open;
                return true;
            }
        }
        if (!open) {
            return true;
        }
        for (int index = buttons.size() - 1; index >= 0; index--) {
            if (buttons.get(index).mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return true;
    }

    /**
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param button the mouse button
     * @return {@code true} when the release was consumed
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        for (ModuleButton button2 : buttons) {
            if (button2.mouseReleased(mouseX, mouseY, button)) {
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
        if (dragging) {
            x = MathUtils.clamp(mouseX - dragOffsetX, 0.0D, RenderUtils.screenWidth() - WIDTH);
            y = MathUtils.clamp(mouseY - dragOffsetY, 0.0D, RenderUtils.screenHeight() - HEADER_HEIGHT);
            return true;
        }
        if (!open) {
            return false;
        }
        for (ModuleButton button : buttons) {
            if (button.mouseDragged(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param amount the scroll amount
     * @return {@code true} when the scroll was consumed
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        boolean inside = mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + getHeight();
        if (!inside || !open) {
            return false;
        }
        double maxScroll = Math.max(0.0D, contentHeight() - viewportHeight());
        scrollTarget = MathUtils.clamp(scrollTarget - amount * 12.0D, 0.0D, maxScroll);
        return true;
    }

    /**
     * @param key       the GLFW key code
     * @param scancode  the platform scancode
     * @param modifiers the modifier bitmask
     * @return {@code true} when the key was consumed
     */
    public boolean keyPressed(int key, int scancode, int modifiers) {
        for (ModuleButton button : buttons) {
            if (button.keyPressed(key, scancode, modifiers)) {
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
        for (ModuleButton button : buttons) {
            if (button.charTyped(character)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@code true} when a child component is capturing keyboard input
     */
    public boolean isListening() {
        for (ModuleButton button : buttons) {
            if (button.isListening()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stops every child component from capturing input.
     */
    public void stopListening() {
        for (ModuleButton button : buttons) {
            button.stopListening();
        }
    }

    /**
     * Writes the layout into the shared UI JSON object.
     *
     * @param ui the UI section of the config file
     */
    public void save(JsonObject ui) {
        JsonObject object = new JsonObject();
        object.addProperty("x", x);
        object.addProperty("y", y);
        object.addProperty("open", open);
        ui.add(category.name(), object);
    }

    /**
     * Reads the layout from the shared UI JSON object.
     *
     * @param ui the UI section of the config file
     */
    public void load(JsonObject ui) {
        if (ui == null || !ui.has(category.name())) {
            return;
        }
        JsonObject object = ui.getAsJsonObject(category.name());
        if (object.has("x")) {
            x = object.get("x").getAsDouble();
        }
        if (object.has("y")) {
            y = object.get("y").getAsDouble();
        }
        if (object.has("open")) {
            open = object.get("open").getAsBoolean();
        }
        x = MathUtils.clamp(x, 0.0D, Math.max(0.0D, RenderUtils.screenWidth() - WIDTH));
        y = MathUtils.clamp(y, 0.0D, Math.max(0.0D, RenderUtils.screenHeight() - HEADER_HEIGHT));
    }
}
