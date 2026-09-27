
package dev.ferro.client.ui.classic;

import dev.ferro.client.Argon;
import dev.ferro.client.module.Category;
import dev.ferro.client.ui.GuiLayout;
import dev.ferro.client.ui.components.Frame;
import dev.ferro.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The original FERRO interface: one draggable, collapsible frame per category.
 */
public class ClassicLayout implements GuiLayout {

    private static final double FRAME_SPACING = 8.0D;

    private final List<Frame> frames = new ArrayList<>();

    @Override
    public void init() {
        if (frames.isEmpty()) {
            double x = 8.0D;
            for (Category category : Category.values()) {
                Frame frame = new Frame(category, x, 16.0D);
                if (Argon.get() != null) {
                    frame.load(Argon.get().getModuleManager().getUiSection());
                }
                frames.add(frame);
                x += Frame.WIDTH + FRAME_SPACING;
            }
            return;
        }
        for (Frame frame : frames) {
            frame.stopListening();
            if (Argon.get() != null) {
                frame.load(Argon.get().getModuleManager().getUiSection());
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (Frame frame : frames) {
            frame.render(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        for (Frame frame : frames) {
            if (frame.mouseClicked(event.x(), event.y(), event.button())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (Frame frame : frames) {
            if (frame.mouseReleased(event.x(), event.y(), event.button())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        for (Frame frame : frames) {
            if (frame.mouseDragged(event.x(), event.y())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (Frame frame : frames) {
            if (frame.mouseScrolled(mouseX, mouseY, scrollY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        for (Frame frame : frames) {
            if (frame.keyPressed(event.key(), event.scancode(), event.modifiers())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (event.codepointAsString().isEmpty()) {
            return false;
        }
        char character = event.codepointAsString().charAt(0);
        for (Frame frame : frames) {
            if (frame.charTyped(character)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void save() {
        if (Argon.get() == null) {
            return;
        }
        for (Frame frame : frames) {
            frame.save(Argon.get().getModuleManager().getUiSection());
        }
    }

    @Override
    public String hint() {
        return "left click toggles  |  right click opens settings  |  drag the header to move";
    }
}
