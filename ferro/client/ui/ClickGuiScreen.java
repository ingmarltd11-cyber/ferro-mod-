
package dev.ferro.client.ui;

import dev.ferro.client.Argon;
import dev.ferro.client.module.modules.client.ClickGui;
import dev.ferro.client.ui.classic.ClassicLayout;
import dev.ferro.client.ui.modern.ModernLayout;
import dev.ferro.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The FERRO menu.
 *
 * <p>The screen is only a shell: it draws the shared backdrop, picks a layout based on the ClickGui settings
 * and forwards every event. The two layouts are {@link ClassicLayout} (draggable frames) and
 * {@link ModernLayout} (sidebar with module cards).</p>
 */
public class ClickGuiScreen extends Screen {

    /** GLFW key code of the right shift key, the default ClickGUI bind. */
    public static final int DEFAULT_KEY = 344;

    private final ClassicLayout classic = new ClassicLayout();
    private final ModernLayout modern = new ModernLayout();

    /**
     * Creates the screen.
     */
    public ClickGuiScreen() {
        super(Component.literal("FERRO"));
    }

    /**
     * @return the ClickGui module, or {@code null} when the client is not initialised yet
     */
    private ClickGui module() {
        Argon client = Argon.get();
        return client == null ? null : client.getModuleManager().get(ClickGui.class);
    }

    /**
     * @return the layout the user selected
     */
    private GuiLayout layout() {
        ClickGui module = module();
        return module == null || module.isModernLayout() ? modern : classic;
    }

    @Override
    protected void init() {
        layout().init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, RenderUtils.withAlpha(RenderUtils.BACKGROUND, 0x9A));
        layout().render(graphics, mouseX, mouseY, partialTick);
        String hint = layout().hint();
        if (!hint.isEmpty()) {
            RenderUtils.text(graphics, hint, 6.0D, height - 12.0D, RenderUtils.TEXT_DISABLED);
        }
        String watermark = Argon.NAME + " " + Argon.VERSION;
        RenderUtils.text(graphics, watermark, 6.0D, height - 22.0D, RenderUtils.ACCENT);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        return layout().mouseClicked(event, doubled) || super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return layout().mouseReleased(event) || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return layout().mouseDragged(event, dragX, dragY) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return layout().mouseScrolled(mouseX, mouseY, scrollX, scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return layout().keyPressed(event) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return layout().charTyped(event) || super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        ClickGui module = module();
        return module != null && module.shouldPause();
    }

    @Override
    public void onClose() {
        ClickGui module = module();
        if (module != null) {
            layout().save();
            if (Argon.get() != null) {
                Argon.get().getModuleManager().save();
            }
            if (module.isEnabled()) {
                module.setEnabled(false);
            }
        }
        super.onClose();
    }
}
