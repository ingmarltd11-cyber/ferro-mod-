
package dev.ferro.client.module.modules.client;

import dev.ferro.client.event.events.Render2DEvent;
import dev.ferro.client.event.events.Render2DListener;
import dev.ferro.client.module.Category;
import dev.ferro.client.module.Module;
import dev.ferro.client.module.setting.ModeSetting;
import dev.ferro.client.module.setting.NumberSetting;
import dev.ferro.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Renders the toast queue produced by {@code Notifications.add}: module toggles, config saves and
 * similar events.
 */
public class Notifications extends Module implements Render2DListener {

    private final ModeSetting position = new ModeSetting("Position", "Top Right", "Top Right", "Top Left",
            "Bottom Right", "Bottom Left");
    private final NumberSetting duration = new NumberSetting("Duration", 500.0D, 8000.0D, 250.0D, 3500.0D,
            "How long a notification stays visible in milliseconds.");
    private final NumberSetting scale = new NumberSetting("Scale", 0.5D, 1.5D, 0.05D, 1.0D);

    /**
     * Creates the module.
     */
    public Notifications() {
        super("Notifications", Category.CLIENT, "Toast messages for module and config events.");
        addSettings(position, duration, scale);
    }

    @Override
    protected void onEnable() {
        listen(Render2DEvent.class);
    }

    @Override
    protected void onDisable() {
        unlisten();
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        List<dev.ferro.client.utils.Notifications.Note> notes = dev.ferro.client.utils.Notifications.getNotes();
        if (notes.isEmpty()) {
            return;
        }
        GuiGraphics graphics = event.getGraphics();
        boolean right = position.is("Top Right") || position.is("Bottom Right");
        boolean bottom = position.is("Bottom Right") || position.is("Bottom Left");
        double y = bottom ? RenderUtils.screenHeight() - 20.0D : 6.0D;
        double step = bottom ? -14.0D : 14.0D;
        for (int index = notes.size() - 1; index >= 0; index--) {
            dev.ferro.client.utils.Notifications.Note note = notes.get(index);
            long remaining = note.duration() - note.age();
            float fade = remaining < 500L ? Math.max(0.0F, remaining / 500.0F) : 1.0F;
            String text = note.message();
            double width = RenderUtils.textWidth(text) + 10.0D;
            double x = right ? RenderUtils.screenWidth() - width - 4.0D : 4.0D;
            RenderUtils.rect(graphics, x, y, width, 12.0D,
                    RenderUtils.withAlpha(RenderUtils.PANEL, (int) (0xB0 * fade)));
            RenderUtils.rect(graphics, x, y, 1.5D, 12.0D, RenderUtils.withAlpha(note.colour(), (int) (255 * fade)));
            RenderUtils.text(graphics, text, x + 5.0D, y + 2.0D, RenderUtils.withAlpha(RenderUtils.TEXT, (int) (255 * fade)));
            y += step;
        }
    }
}
