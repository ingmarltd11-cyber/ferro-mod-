
package dev.ferro.client.module.modules.client;

import dev.ferro.client.module.Category;
import dev.ferro.client.module.Module;
import dev.ferro.client.module.setting.BooleanSetting;
import dev.ferro.client.module.setting.ModeSetting;
import dev.ferro.client.ui.ClickGuiScreen;
import dev.ferro.client.ui.Theme;

/**
 * Opens the FERRO ClickGUI and owns the interface preferences: which layout to use and which theme.
 *
 * <p>Both live here, in ordinary settings, so they are saved and loaded by the normal config system and the
 * interface has exactly one source of truth for them.</p>
 */
public class ClickGui extends Module {

    private final BooleanSetting modernLayout = new BooleanSetting("Modern Layout", true,
            "The sidebar interface. Turn off for the classic draggable frames.");
    private final ModeSetting theme = new ModeSetting("Theme", "Ferro", Theme.names());
    private final BooleanSetting pauseGame = new BooleanSetting("Pause Game", false,
            "Whether the world pauses while the menu is open, in single player.");

    /**
     * Creates the module.
     */
    public ClickGui() {
        super("ClickGui", Category.CLIENT, "The FERRO module menu.");
        getKeybind().set(ClickGuiScreen.DEFAULT_KEY);
        theme.onChange(name -> Theme.byName(name).apply());
        addSettings(modernLayout, theme, pauseGame);
    }

    @Override
    protected void onEnable() {
        if (mc().player == null || mc().level == null) {
            // Enabled by the config while the client is still starting: there is no world and no window to
            // draw into yet. Opening a screen that early is not allowed (setScreen is client thread only) and
            // it breaks other mods with it, so the menu just stays armed and switches itself off again.
            setEnabled(false);
            return;
        }
        // Screen handling belongs on the client thread, so it is scheduled rather than run inline.
        mc().execute(() -> mc().setScreen(new ClickGuiScreen()));
    }

    @Override
    protected void onDisable() {
        if (mc().screen instanceof ClickGuiScreen) {
            mc().setScreen(null);
        }
    }

    /**
     * Selects a theme by display name. The theme gallery calls into this, so the picker and the setting stay
     * one source of truth.
     *
     * @param name the display name of a theme
     */
    public void setTheme(String name) {
        theme.set(name);
    }

    /**
     * @return {@code true} when the sidebar interface should be used
     */
    public boolean isModernLayout() {
        return modernLayout.get();
    }

    /**
     * @return the display name of the selected theme
     */
    public String getThemeName() {
        return theme.get();
    }

    /**
     * @return whether the game should pause while the menu is open
     */
    public boolean shouldPause() {
        return pauseGame.get();
    }
}
