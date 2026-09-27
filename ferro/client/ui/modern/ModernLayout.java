package dev.ferro.client.ui.modern;

import com.google.gson.JsonObject;
import dev.ferro.client.Argon;
import dev.ferro.client.module.Category;
import dev.ferro.client.module.Module;
import dev.ferro.client.module.modules.client.ClickGui;
import dev.ferro.client.module.modules.client.Config;
import dev.ferro.client.module.modules.client.Enemies;
import dev.ferro.client.module.modules.client.Friends;
import dev.ferro.client.module.modules.client.Notifications;
import dev.ferro.client.module.setting.KeybindSetting;
import dev.ferro.client.module.setting.Setting;
import dev.ferro.client.ui.GuiLayout;
import dev.ferro.client.ui.Icons;
import dev.ferro.client.ui.Theme;
import dev.ferro.client.utils.MathUtils;
import dev.ferro.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The modern interface: a floating window with a sidebar on the left and the page content on the right.
 *
 * <p>The window never fills the screen: it is sized to {@code min(860, screen - 60)} by
 * {@code min(560, screen - 60)} and centred, so the world stays visible around it. The sidebar carries the
 * wordmark, the module categories with their counters and the GENERAL pages (Settings, Themes, Configs,
 * Socials, Keybinds). The content pane shows the page title with its counter, a search box, the sort and
 * view chips and the module cards.</p>
 *
 * <p>Clicking a card opens its settings page, which keeps the page header and puts a Back button above a
 * panel with the bind row and every setting of that module. The switch on a card toggles the module without
 * leaving the list.</p>
 *
 * <p>Input is handled through the hit boxes that drawing itself registers, so what is on screen and what is
 * clickable can never drift apart, and scrolled away content is neither drawn nor clickable.</p>
 */
public class ModernLayout implements GuiLayout {

    /** Sort orders for the module list. */
    private enum Sort {
        /** Registration order of the module manager. */
        DEFAULT,
        /** Alphabetical by name. */
        NAME,
        /** Enabled modules first. */
        ENABLED
    }

    /** List or grid arrangement of the cards. */
    private enum View {
        /** Full width cards, one per row. */
        LIST,
        /** Two cards per row. */
        GRID
    }

    /** The pages in the GENERAL section of the sidebar. */
    private enum General {
        /** Client preferences: the ClickGui and Notifications modules. */
        SETTINGS("Settings", Icons.Icon.SLIDERS),
        /** The theme gallery. */
        THEMES("Themes", Icons.Icon.PALETTE),
        /** Config profiles and the Config module. */
        CONFIGS("Configs", Icons.Icon.FOLDER),
        /** Friends and enemies. */
        SOCIALS("Socials", Icons.Icon.PEOPLE),
        /** Every module bind in one list. */
        KEYBINDS("Keybinds", Icons.Icon.KEYBOARD);

        private final String displayName;
        private final Icons.Icon icon;

        General(String displayName, Icons.Icon icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        /**
         * @return the label shown in the sidebar and as the page title
         */
        String displayName() {
            return displayName;
        }

        /**
         * @return the glyph shown in the sidebar
         */
        Icons.Icon icon() {
            return icon;
        }

        /**
         * @param name the enum constant name
         * @return the page, or {@code null} when the name is unknown
         */
        static General byName(String name) {
            for (General page : values()) {
                if (page.name().equals(name)) {
                    return page;
                }
            }
            return null;
        }
    }

    /**
     * One interactive region, registered while drawing.
     *
     * @param key    the action key, for example {@code card:KillAura}
     * @param x      the left edge
     * @param y      the top edge
     * @param width  the width
     * @param height the height
     */
    private record Hit(String key, double x, double y, double width, double height) {

        /**
         * @param mouseX the mouse x position
         * @param mouseY the mouse y position
         * @return {@code true} when the point is inside this region
         */
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private static final double SIDEBAR_WIDTH = 176.0D;
    private static final double WINDOW_MARGIN = 30.0D;
    private static final double MAX_WINDOW_WIDTH = 860.0D;
    private static final double MAX_WINDOW_HEIGHT = 560.0D;
    private static final double HEADER_HEIGHT = 62.0D;
    private static final double ITEM_HEIGHT = 22.0D;
    private static final double ITEM_GAP = 2.0D;
    private static final double CARD_GAP = 6.0D;
    private static final double PADDING = 12.0D;
    private static final double ICON_SIZE = 12.0D;
    private static final double SWITCH_WIDTH = 26.0D;
    private static final double SWITCH_HEIGHT = 12.0D;
    private static final double THEME_CARD_HEIGHT = 54.0D;
    private static final double THEME_GAP = 8.0D;
    private static final double PROFILE_ROW_HEIGHT = 24.0D;
    private static final double KEYBIND_ROW_HEIGHT = 26.0D;
    /** Corner radius of the window. */
    private static final int WINDOW_RADIUS = 18;
    /** Corner radius of sidebar rows and chips. */
    private static final int ITEM_RADIUS = 8;
    /** Corner radius of panels and cards. */
    private static final int PANEL_RADIUS = 12;
    /** Space kept free under the content for the footer line. */
    private static final double FOOTER_HEIGHT = 20.0D;

    private static final int KEY_ESCAPE = 256;
    private static final int KEY_BACKSPACE = 259;

    private final List<ModuleCard> cards = new ArrayList<>();
    private final List<ModuleCard> visible = new ArrayList<>();
    private final List<SettingRow> detailRows = new ArrayList<>();
    private final List<String> profiles = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, Module> modulesByKey = new LinkedHashMap<>();
    private final Map<String, Double> fades = new LinkedHashMap<>();
    private final StringBuilder search = new StringBuilder();

    private Category category = Category.COMBAT;
    private General general;
    private Module detail;
    private KeybindSetting listeningBind;
    private boolean searchFocused;

    private Sort sort = Sort.DEFAULT;
    private View view = View.LIST;

    private double scroll;
    private double scrollTarget;
    private double contentHeight;
    private double viewportHeight;
    private double detailScroll;
    private double detailScrollTarget;
    private double detailContentHeight;
    private double detailViewportHeight;

    // Window rectangle, recomputed every frame.
    private double windowX;
    private double windowY;
    private double windowWidth;
    private double windowHeight;
    /** Opening animation, {@code 0} on the first frame and {@code 1} once the window sits still. */
    private double open;

    /**
     * Creates the layout. The registry is read in {@link #init()} so that the client is ready.
     */
    public ModernLayout() {
    }

    @Override
    public void init() {
        Argon client = Argon.get();
        if (client == null) {
            return;
        }
        JsonObject ui = client.getModuleManager().getUiSection();
        if (ui != null && ui.has("modern") && ui.get("modern").isJsonObject()) {
            JsonObject modern = ui.getAsJsonObject("modern");
            if (modern.has("category")) {
                try {
                    category = Category.valueOf(modern.get("category").getAsString());
                } catch (IllegalArgumentException ignored) {
                    category = Category.COMBAT;
                }
            }
            if (modern.has("general")) {
                general = General.byName(modern.get("general").getAsString());
            }
            if (modern.has("sort")) {
                try {
                    sort = Sort.valueOf(modern.get("sort").getAsString());
                } catch (IllegalArgumentException ignored) {
                    sort = Sort.DEFAULT;
                }
            }
            if (modern.has("view")) {
                try {
                    view = View.valueOf(modern.get("view").getAsString());
                } catch (IllegalArgumentException ignored) {
                    view = View.LIST;
                }
            }
        }
        rebuild();
    }

    /**
     * Rebuilds the cards and caches for the selected page and resets the scroll.
     */
    private void rebuild() {
        cards.clear();
        modulesByKey.clear();
        detail = null;
        detailRows.clear();
        listeningBind = null;
        detailScroll = 0.0D;
        detailScrollTarget = 0.0D;
        detailContentHeight = 0.0D;
        Argon client = Argon.get();
        if (client != null) {
            for (Module module : client.getModuleManager().getModules()) {
                modulesByKey.put(key(module), module);
            }
            for (Module module : pageModules()) {
                cards.add(new ModuleCard(module));
            }
        }
        refreshProfiles();
        scroll = 0.0D;
        scrollTarget = 0.0D;
    }

    /**
     * @return the modules shown as cards on the selected page
     */
    private List<Module> pageModules() {
        Argon client = Argon.get();
        if (client == null) {
            return List.of();
        }
        if (general == null) {
            return client.getModuleManager().getModules(category);
        }
        return switch (general) {
            case SETTINGS -> present(client.getModuleManager().get(ClickGui.class),
                    client.getModuleManager().get(Notifications.class));
            case CONFIGS -> present(client.getModuleManager().get(Config.class));
            case SOCIALS -> present(client.getModuleManager().get(Friends.class),
                    client.getModuleManager().get(Enemies.class));
            case THEMES, KEYBINDS -> List.of();
        };
    }

    /**
     * @param modules the modules, some of which may be missing
     * @return the modules that exist
     */
    private static List<Module> present(Module... modules) {
        List<Module> result = new ArrayList<>(modules.length);
        for (Module module : modules) {
            if (module != null && module.isVisibleInGui()) {
                result.add(module);
            }
        }
        return result;
    }

    /**
     * @param module the module
     * @return the key this module is addressed by in hit boxes and the registry
     */
    private static String key(Module module) {
        return module.getClass().getSimpleName();
    }

    /**
     * @return the ClickGui module, or {@code null} before the client is initialised
     */
    private static ClickGui clickGui() {
        Argon client = Argon.get();
        return client == null ? null : client.getModuleManager().get(ClickGui.class);
    }

    /**
     * Refreshes the cached profile names. The list comes from disk, so it is read when the page changes
     * instead of every frame.
     */
    private void refreshProfiles() {
        profiles.clear();
        profiles.addAll(Config.listProfiles());
    }

    /**
     * Opens the settings page of a module.
     *
     * @param module the module
     */
    private void openDetail(Module module) {
        detail = module;
        detailRows.clear();
        detailRows.add(new SettingRow.KeybindRow(module.getKeybind()));
        for (Setting<?> setting : module.getSettings()) {
            detailRows.add(SettingRow.of(setting));
        }
        detailScroll = 0.0D;
        detailScrollTarget = 0.0D;
    }

    // ------------------------------------------------------------------ geometry

    /**
     * Recomputes the window rectangle from the current screen size.
     */
    private void updateWindow() {
        windowWidth = Math.min(MAX_WINDOW_WIDTH, Math.max(320.0D, RenderUtils.screenWidth() - WINDOW_MARGIN * 2.0D));
        windowHeight = Math.min(MAX_WINDOW_HEIGHT, Math.max(240.0D, RenderUtils.screenHeight() - WINDOW_MARGIN * 2.0D));
        windowX = Math.floor((RenderUtils.screenWidth() - windowWidth) / 2.0D);
        windowY = Math.floor((RenderUtils.screenHeight() - windowHeight) / 2.0D);
    }

    /**
     * @return the left edge of the content pane
     */
    private double contentX() {
        return windowX + SIDEBAR_WIDTH + 1.0D;
    }

    /**
     * @return the width of the content pane
     */
    private double contentWidth() {
        return windowWidth - SIDEBAR_WIDTH - 1.0D;
    }

    /**
     * @return the top edge of the content area
     */
    private double contentTop() {
        return windowY + HEADER_HEIGHT;
    }

    /**
     * @return the bottom edge of the content area
     */
    private double contentBottom() {
        return windowY + windowHeight - FOOTER_HEIGHT;
    }

    /**
     * @param index the sidebar index
     * @return the y position of a category row
     */
    private double categoryRow(int index) {
        return windowY + 68.0D + index * (ITEM_HEIGHT + ITEM_GAP);
    }

    /**
     * @return the y position of the GENERAL label
     */
    private double generalLabelY() {
        return categoryRow(Category.values().length) + 12.0D;
    }

    /**
     * @param index the sidebar index
     * @return the y position of a general page row
     */
    private double generalRow(int index) {
        return generalLabelY() + 16.0D + index * (ITEM_HEIGHT + ITEM_GAP);
    }

    /**
     * @return the top edge of the settings body on the detail page
     */
    private double detailBodyTop() {
        return detailPanelTop() + 44.0D;
    }

    /**
     * @return the bottom edge of the settings body on the detail page
     */
    private double detailBodyBottom() {
        return contentBottom() - 10.0D;
    }

    /**
     * @return the top edge of the detail panel
     */
    private double detailPanelTop() {
        return contentTop() + 26.0D;
    }

    // ------------------------------------------------------------------ input helpers

    /**
     * Registers a hit box.
     *
     * @param key    the action key
     * @param x      the left edge
     * @param y      the top edge
     * @param width  the width
     * @param height the height
     */
    private void hit(String key, double x, double y, double width, double height) {
        hits.add(new Hit(key, x, y, width, height));
    }

    /**
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @return the key of the topmost hit box under the mouse, or {@code null}
     */
    private String hitAt(double mouseX, double mouseY) {
        for (int index = hits.size() - 1; index >= 0; index--) {
            Hit hit = hits.get(index);
            if (hit.contains(mouseX, mouseY)) {
                return hit.key();
            }
        }
        return null;
    }

    /**
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param x      the left edge
     * @param y      the top edge
     * @param width  the width
     * @param height the height
     * @return {@code true} when the point is inside the rectangle
     */
    private static boolean inside(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    /**
     * @param key    the animation key
     * @param active whether the element is hovered or selected
     * @return the faded hover value, {@code 0..1}
     */
    private double fade(String key, boolean active) {
        double value = MathUtils.lerp(fades.getOrDefault(key, 0.0D), active ? 1.0D : 0.0D, 0.15D);
        fades.put(key, value);
        return value;
    }

    /**
     * Settles a scroll value towards its target and clamps it.
     *
     * @param current the current value
     * @param target  the target value
     * @param content the content height
     * @param view    the viewport height
     * @return the settled value
     */
    private static double settle(double current, double target, double content, double view) {
        double max = Math.max(0.0D, content - view);
        double clampedTarget = MathUtils.clamp(target, 0.0D, max);
        // A soft, exponential glide instead of a snap, which is what makes scrolling feel smooth.
        return MathUtils.clamp(MathUtils.lerp(current, clampedTarget, 0.18D), 0.0D, max);
    }

    /**
     * @param value the raw target
     * @param content the content height
     * @param view    the viewport height
     * @return the clamped target
     */
    private static double clampTarget(double value, double content, double view) {
        return MathUtils.clamp(value, 0.0D, Math.max(0.0D, content - view));
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateWindow();
        hits.clear();
        updateVisible();

        // Eased rather than linear, and the whole window slides the last few pixels into place: that single
        // animation is what makes opening the menu feel like a window and not like a frame that appears.
        open = MathUtils.lerp(open, 1.0D, 0.16D);
        double slide = (1.0D - open) * 8.0D;
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, (float) slide);

        RenderUtils.shadow(graphics, windowX, windowY, windowWidth, windowHeight, WINDOW_RADIUS, 4, 160);
        RenderUtils.roundedPanel(graphics, windowX, windowY, windowWidth, windowHeight, RenderUtils.BACKGROUND,
                RenderUtils.OUTLINE, WINDOW_RADIUS);
        // The accent sweep along the top edge, from the primary into the secondary accent of the theme.
        RenderUtils.gradientH(graphics, windowX + WINDOW_RADIUS, windowY + 1.0D,
                windowWidth - WINDOW_RADIUS * 2.0D, 1.5D, RenderUtils.ACCENT, RenderUtils.ACCENT_2);
        RenderUtils.rounded(graphics, windowX + 1.0D, windowY + 2.0D, SIDEBAR_WIDTH - 2.0D,
                windowHeight - 4.0D, RenderUtils.SIDEBAR, WINDOW_RADIUS - 2);
        RenderUtils.rounded(graphics, windowX + SIDEBAR_WIDTH + 3.0D, windowY + 10.0D, 1.0D,
                windowHeight - 20.0D, RenderUtils.withAlpha(RenderUtils.OUTLINE, 130), 1);

        renderSidebar(graphics, mouseX, mouseY);
        renderHeader(graphics, mouseX, mouseY);

        if (detail != null) {
            renderDetail(graphics, mouseX, mouseY);
        } else if (general == General.THEMES) {
            renderThemes(graphics, mouseX, mouseY);
        } else if (general == General.KEYBINDS) {
            renderKeybinds(graphics, mouseX, mouseY);
        } else {
            renderCards(graphics, mouseX, mouseY);
        }

        renderScrollbar(graphics);
        RenderUtils.centeredText(graphics, footerText(), contentX() + contentWidth() / 2.0D,
                windowY + windowHeight - 14.0D, RenderUtils.withAlpha(RenderUtils.TEXT_DISABLED, 150));
        graphics.pose().popMatrix();
    }

    /**
     * Filters and sorts the cards for the selected page.
     */
    private void updateVisible() {
        visible.clear();
        for (ModuleCard card : cards) {
            if (matches(card.getModule())) {
                visible.add(card);
            }
        }
        switch (sort) {
            case NAME -> visible.sort(Comparator.comparing(card -> card.getModule().getName().toLowerCase(Locale.ROOT)));
            case ENABLED -> visible.sort((a, b) -> Boolean.compare(b.getModule().isEnabled(), a.getModule().isEnabled()));
            default -> {
            }
        }
    }

    /**
     * @param module the module
     * @return {@code true} when the module name or description contains the search text
     */
    private boolean matches(Module module) {
        String query = search.toString().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return true;
        }
        return module.getName().toLowerCase(Locale.ROOT).contains(query)
                || module.getDescription().toLowerCase(Locale.ROOT).contains(query);
    }

    /**
     * Draws the sidebar: the wordmark, the module categories and the GENERAL pages.
     *
     * @param graphics the render context
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     */
    private void renderSidebar(GuiGraphics graphics, int mouseX, int mouseY) {
        double logoY = windowY + 12.0D;
        Icons.draw(graphics, Icons.Icon.CROWN, windowX + 15.0D, logoY - 1.0D, 15.0D, RenderUtils.ACCENT);
        RenderUtils.text(graphics, Argon.NAME, windowX + 36.0D, logoY, RenderUtils.TEXT);
        RenderUtils.text(graphics, "Client", windowX + 40.0D + RenderUtils.textWidth(Argon.NAME), logoY,
                RenderUtils.ACCENT_2);
        RenderUtils.text(graphics, "RELEASE " + Argon.VERSION, windowX + 15.0D, logoY + 17.0D,
                RenderUtils.TEXT_DISABLED);
        RenderUtils.gradientH(graphics, windowX + 15.0D, logoY + 31.0D, 44.0D, 2.0D, RenderUtils.ACCENT,
                RenderUtils.ACCENT_2);

        RenderUtils.text(graphics, "MODULES", windowX + 15.0D, windowY + 55.0D, RenderUtils.TEXT_DISABLED);
        Category[] categories = Category.values();
        for (int index = 0; index < categories.length; index++) {
            Category entry = categories[index];
            double y = categoryRow(index);
            double rowX = windowX + 6.0D;
            double rowWidth = SIDEBAR_WIDTH - 12.0D;
            boolean active = general == null && category == entry;
            boolean hovered = inside(mouseX, mouseY, rowX, y, rowWidth, ITEM_HEIGHT);
            hit("nav:category:" + entry.name(), rowX, y, rowWidth, ITEM_HEIGHT);
            sidebarItem(graphics, icon(entry), entry.getDisplayName(), String.valueOf(count(entry)), y, active,
                    fade("category:" + entry.name(), hovered || active));
        }

        RenderUtils.text(graphics, "GENERAL", windowX + 15.0D, generalLabelY(), RenderUtils.TEXT_DISABLED);
        General[] pages = General.values();
        for (int index = 0; index < pages.length; index++) {
            General page = pages[index];
            double y = generalRow(index);
            double rowX = windowX + 6.0D;
            double rowWidth = SIDEBAR_WIDTH - 12.0D;
            boolean active = general == page;
            boolean hovered = inside(mouseX, mouseY, rowX, y, rowWidth, ITEM_HEIGHT);
            hit("nav:general:" + page.name(), rowX, y, rowWidth, ITEM_HEIGHT);
            sidebarItem(graphics, page.icon(), page.displayName(), "", y, active,
                    fade("general:" + page.name(), hovered || active));
        }
    }

    /**
     * Draws one sidebar row.
     *
     * @param graphics the render context
     * @param icon     the glyph
     * @param label    the label
     * @param badge    the count shown on the right, empty for none
     * @param y        the top edge
     * @param active   whether the row is selected
     * @param fade     the animated hover value
     */
    private void sidebarItem(GuiGraphics graphics, Icons.Icon icon, String label, String badge, double y,
                             boolean active, double fade) {
        double left = windowX + 6.0D;
        double width = SIDEBAR_WIDTH - 12.0D;
        if (active) {
            RenderUtils.rounded(graphics, left, y, width, ITEM_HEIGHT,
                    RenderUtils.withAlpha(RenderUtils.ACCENT, 40 + (int) (20.0D * fade)), ITEM_RADIUS);
            RenderUtils.gradient(graphics, left, y + 5.0D, 2.5D, ITEM_HEIGHT - 10.0D, RenderUtils.ACCENT,
                    RenderUtils.ACCENT_2);
        } else if (fade > 0.02D) {
            RenderUtils.rounded(graphics, left, y, width, ITEM_HEIGHT,
                    RenderUtils.withAlpha(RenderUtils.FRAME, (int) (120.0D * fade)), ITEM_RADIUS);
        }

        double iconX = left + 8.0D;
        double iconY = y + (ITEM_HEIGHT - ICON_SIZE) / 2.0D;
        int iconColour = active ? RenderUtils.ACCENT
                : RenderUtils.withAlpha(RenderUtils.TEXT_DISABLED, 110 + (int) (145.0D * fade));
        Icons.draw(graphics, icon, iconX, iconY, ICON_SIZE, iconColour);

        int colour = active ? RenderUtils.TEXT
                : RenderUtils.lerpColour(RenderUtils.TEXT_DISABLED, RenderUtils.TEXT, (float) fade);
        RenderUtils.text(graphics, label, left + 26.0D, y + 7.0D, colour);
        if (!badge.isEmpty()) {
            double badgeWidth = RenderUtils.textWidth(badge) + 10.0D;
            RenderUtils.rounded(graphics, left + width - badgeWidth - 6.0D, y + 5.0D, badgeWidth, 12.0D,
                    RenderUtils.withAlpha(active ? RenderUtils.ACCENT : RenderUtils.OUTLINE, active ? 70 : 90), 6);
            RenderUtils.centeredText(graphics, badge, left + width - badgeWidth / 2.0D - 6.0D, y + 7.0D,
                    active ? RenderUtils.ACCENT : RenderUtils.TEXT_DISABLED);
        }
    }

    /**
     * Draws the page title, the counter line, the search box and the chips.
     *
     * @param graphics the render context
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     */
    private void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        String title = titleText();
        double titleX = contentX() + PADDING + 2.0D;
        RenderUtils.text(graphics, title, titleX, windowY + 12.0D, RenderUtils.TEXT);
        String suffix = titleSuffix();
        if (!suffix.isEmpty()) {
            RenderUtils.text(graphics, suffix, titleX + RenderUtils.textWidth(title), windowY + 12.0D,
                    RenderUtils.ACCENT_2);
        }
        RenderUtils.text(graphics, counterText(), titleX, windowY + 27.0D, RenderUtils.TEXT_DISABLED);
        RenderUtils.rounded(graphics, contentX() + PADDING, windowY + HEADER_HEIGHT - 4.0D,
                contentWidth() - PADDING * 2.0D - 8.0D, 1.0D, RenderUtils.withAlpha(RenderUtils.OUTLINE, 110), 1);

        if (!hasSearch()) {
            return;
        }
        renderSearch(graphics, mouseX, mouseY);
        if (detail == null) {
            renderToolbar(graphics, mouseX, mouseY);
        }
    }

    /**
     * Draws the search box in the top right of the content pane.
     *
     * @param graphics the render context
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     */
    private void renderSearch(GuiGraphics graphics, int mouseX, int mouseY) {
        double fieldWidth = 150.0D;
        double fieldHeight = 18.0D;
        double fieldX = contentX() + contentWidth() - PADDING - fieldWidth;
        double fieldY = windowY + 10.0D;
        boolean hovered = inside(mouseX, mouseY, fieldX, fieldY, fieldWidth, fieldHeight);
        RenderUtils.roundedPanel(graphics, fieldX, fieldY, fieldWidth, fieldHeight, RenderUtils.SURFACE,
                searchFocused || hovered ? RenderUtils.HOVER : RenderUtils.OUTLINE, (int) (fieldHeight / 2.0D));
        Icons.draw(graphics, Icons.Icon.SEARCH, fieldX + 6.0D, fieldY + 3.0D, 12.0D,
                RenderUtils.withAlpha(RenderUtils.TEXT_DISABLED, searchFocused || hovered ? 235 : 160));
        String text = search.length() == 0 ? "Search modules" : search.toString();
        double textX = fieldX + 22.0D;
        RenderUtils.text(graphics, text, textX, fieldY + 5.0D,
                search.length() == 0 ? RenderUtils.TEXT_DISABLED : RenderUtils.TEXT);
        if (searchFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            RenderUtils.rect(graphics, textX + RenderUtils.textWidth(text) + 1.0D, fieldY + 5.0D, 1.0D, 9.0D,
                    RenderUtils.ACCENT);
        }
        hit("search", fieldX, fieldY, fieldWidth, fieldHeight);
    }

    /**
     * Draws the sort chips and the list/grid toggle.
     *
     * @param graphics the render context
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     */
    private void renderToolbar(GuiGraphics graphics, int mouseX, int mouseY) {
        double rowY = windowY + 40.0D;
        double chipX = contentX() + PADDING;
        chipX = chip(graphics, chipX, rowY, "A-Z", sort == Sort.NAME, mouseX, mouseY, "sort:NAME");
        chip(graphics, chipX, rowY, "ON", sort == Sort.ENABLED, mouseX, mouseY, "sort:ENABLED");

        double right = contentX() + contentWidth() - PADDING;
        viewToggle(graphics, right - 17.0D, rowY, Icons.Icon.GRID, view == View.GRID, mouseX, mouseY, "view:GRID");
        viewToggle(graphics, right - 36.0D, rowY, Icons.Icon.LIST, view == View.LIST, mouseX, mouseY, "view:LIST");
    }

    /**
     * Draws one sort chip.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param label    the label
     * @param active   whether the chip is selected
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     * @param key      the action key
     * @return the left edge of the next chip
     */
    private double chip(GuiGraphics graphics, double x, double y, String label, boolean active, int mouseX,
                        int mouseY, String key) {
        double width = RenderUtils.textWidth(label) + 20.0D;
        double height = 16.0D;
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        RenderUtils.roundedPanel(graphics, x, y, width, height,
                active ? RenderUtils.withAlpha(RenderUtils.ACCENT, 80) : RenderUtils.SURFACE,
                active || hovered ? RenderUtils.HOVER : RenderUtils.OUTLINE, (int) (height / 2.0D));
        RenderUtils.centeredText(graphics, label, x + width / 2.0D, y + 5.0D,
                active ? RenderUtils.TEXT : RenderUtils.TEXT_DISABLED);
        hit(key, x, y, width, height);
        return x + width + 6.0D;
    }

    /**
     * Draws one half of the list/grid toggle.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param icon     the glyph
     * @param active   whether this view is selected
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     * @param key      the action key
     */
    private void viewToggle(GuiGraphics graphics, double x, double y, Icons.Icon icon, boolean active, int mouseX,
                            int mouseY, String key) {
        double width = 17.0D;
        double height = 16.0D;
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        RenderUtils.roundedPanel(graphics, x, y, width, height,
                active ? RenderUtils.withAlpha(RenderUtils.ACCENT, 80) : RenderUtils.SURFACE,
                active || hovered ? RenderUtils.HOVER : RenderUtils.OUTLINE, 5);
        Icons.draw(graphics, icon, x + 2.5D, y + 2.0D, 12.0D,
                active ? RenderUtils.TEXT : RenderUtils.TEXT_DISABLED);
        hit(key, x, y, width, height);
    }

    /**
     * Draws the module cards of the selected page.
     *
     * @param graphics the render context
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     */
    private void renderCards(GuiGraphics graphics, int mouseX, int mouseY) {
        double top = contentTop();
        viewportHeight = contentBottom() - top;
        scroll = settle(scroll, scrollTarget, contentHeight, viewportHeight);
        double startY = top - scroll;
        double bottom = top + viewportHeight;
        double left = contentX() + PADDING;
        double usable = contentWidth() - PADDING * 2.0D - 6.0D;

        RenderUtils.scissor(graphics, contentX() + 1.0D, top, contentWidth() - 4.0D, viewportHeight);

        double cursor = startY;
        if (view == View.LIST) {
            for (ModuleCard card : visible) {
                if (cursor + ModuleCard.HEIGHT >= top && cursor <= bottom) {
                    card.setBounds(left, cursor, usable);
                    card.render(graphics, mouseX, mouseY);
                    cardHit(card);
                }
                cursor += ModuleCard.HEIGHT + CARD_GAP;
            }
        } else {
            double cardWidth = Math.max(150.0D, (usable - CARD_GAP) / 2.0D);
            int column = 0;
            double rowY = startY;
            for (ModuleCard card : visible) {
                if (rowY + ModuleCard.HEIGHT >= top && rowY <= bottom) {
                    card.setBounds(left + column * (cardWidth + CARD_GAP), rowY, cardWidth);
                    card.render(graphics, mouseX, mouseY);
                    cardHit(card);
                }
                column++;
                if (column == 2) {
                    column = 0;
                    rowY += ModuleCard.HEIGHT + CARD_GAP;
                }
            }
            cursor = rowY + (column == 0 ? 0.0D : ModuleCard.HEIGHT + CARD_GAP);
        }
        contentHeight = cursor - startY;

        if (general == General.CONFIGS) {
            renderProfiles(graphics, mouseX, mouseY, cursor, bottom);
        }
        RenderUtils.resetScissor(graphics);

        if (visible.isEmpty() && !(general == General.CONFIGS && !profiles.isEmpty())) {
            String empty = search.length() == 0 ? "no modules here yet" : "no matches for \"" + search + "\"";
            RenderUtils.centeredText(graphics, empty, contentX() + contentWidth() / 2.0D, top + 24.0D,
                    RenderUtils.TEXT_DISABLED);
        }
    }

    /**
     * Registers the two hit boxes of a card: the body opens the settings page, the switch toggles.
     *
     * @param card the card
     */
    private void cardHit(ModuleCard card) {
        String key = key(card.getModule());
        hit("card:" + key, card.getX(), card.getY(), card.getWidth(), card.getHeight());
        hit("switch:" + key, card.getSwitchX(), card.getSwitchY(), card.getSwitchWidth(), card.getSwitchHeight());
    }

    /**
     * Draws the profile list of the Configs page.
     *
     * @param graphics the render context
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     * @param cursor   the current y position
     * @param bottom   the bottom of the content area
     * @return the new y position
     */
    private double renderProfiles(GuiGraphics graphics, int mouseX, int mouseY, double cursor, double bottom) {
        double left = contentX() + PADDING;
        double usable = contentWidth() - PADDING * 2.0D - 6.0D;
        cursor += 8.0D;
        RenderUtils.text(graphics, "PROFILES", left, cursor, RenderUtils.TEXT_DISABLED);
        cursor += 14.0D;
        for (String name : profiles) {
            if (cursor + PROFILE_ROW_HEIGHT >= contentTop() && cursor <= bottom) {
                RenderUtils.roundedPanel(graphics, left, cursor, usable, PROFILE_ROW_HEIGHT, RenderUtils.SURFACE,
                        RenderUtils.OUTLINE, 8);
                RenderUtils.text(graphics, name, left + 10.0D, cursor + 8.0D, RenderUtils.TEXT);
                double buttonWidth = 36.0D;
                double buttonY = cursor + 4.0D;
                double saveX = left + usable - 8.0D - buttonWidth;
                double loadX = saveX - 4.0D - buttonWidth;
                profileButton(graphics, loadX, buttonY, buttonWidth, "Load", mouseX, mouseY, "profile:load:" + name);
                profileButton(graphics, saveX, buttonY, buttonWidth, "Save", mouseX, mouseY, "profile:save:" + name);
            }
            cursor += PROFILE_ROW_HEIGHT + 4.0D;
        }
        return cursor;
    }

    /**
     * Draws one small profile button.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param width    the button width
     * @param label    the label
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     * @param key      the action key
     */
    private void profileButton(GuiGraphics graphics, double x, double y, double width, String label, int mouseX,
                               int mouseY, String key) {
        double height = 16.0D;
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        RenderUtils.roundedPanel(graphics, x, y, width, height,
                hovered ? RenderUtils.withAlpha(RenderUtils.ACCENT, 70) : RenderUtils.withAlpha(RenderUtils.OUTLINE, 90),
                hovered ? RenderUtils.HOVER : RenderUtils.OUTLINE, 6);
        RenderUtils.centeredText(graphics, label, x + width / 2.0D, y + 4.0D,
                hovered ? RenderUtils.TEXT : RenderUtils.TEXT_DISABLED);
        hit(key, x, y, width, height);
    }

    /**
     * Draws the theme gallery.
     *
     * @param graphics the render context
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     */
    private void renderThemes(GuiGraphics graphics, int mouseX, int mouseY) {
        double top = contentTop();
        viewportHeight = contentBottom() - top;
        scroll = settle(scroll, scrollTarget, contentHeight, viewportHeight);
        double startY = top - scroll;
        double bottom = top + viewportHeight;
        double left = contentX() + PADDING;
        double usable = contentWidth() - PADDING * 2.0D - 6.0D;
        double cardWidth = Math.max(110.0D, (usable - THEME_GAP * 2.0D) / 3.0D);
        ClickGui module = clickGui();
        Theme selected = Theme.byName(module == null ? Theme.FERRO.getDisplayName() : module.getThemeName());

        RenderUtils.scissor(graphics, contentX() + 1.0D, top, contentWidth() - 4.0D, viewportHeight);
        double rowY = startY;
        int column = 0;
        for (Theme theme : Theme.values()) {
            double cardX = left + column * (cardWidth + THEME_GAP);
            if (rowY + THEME_CARD_HEIGHT >= top && rowY <= bottom) {
                renderThemeCard(graphics, theme, cardX, rowY, cardWidth, theme == selected, mouseX, mouseY);
                hit("theme:" + theme.getDisplayName(), cardX, rowY, cardWidth, THEME_CARD_HEIGHT);
            }
            column++;
            if (column == 3) {
                column = 0;
                rowY += THEME_CARD_HEIGHT + THEME_GAP;
            }
        }
        contentHeight = rowY + (column == 0 ? 0.0D : THEME_CARD_HEIGHT + THEME_GAP) - startY;
        RenderUtils.resetScissor(graphics);
    }

    /**
     * Draws one theme card: name, four swatches and a tick when it is the active theme.
     *
     * @param graphics the render context
     * @param theme    the theme
     * @param x        the left edge
     * @param y        the top edge
     * @param width    the card width
     * @param selected whether this theme is active
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     */
    private void renderThemeCard(GuiGraphics graphics, Theme theme, double x, double y, double width,
                                 boolean selected, int mouseX, int mouseY) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, THEME_CARD_HEIGHT);
        double fade = fade("theme:" + theme.getDisplayName(), hovered || selected);
        int fill = selected ? RenderUtils.withAlpha(RenderUtils.ACCENT, 40)
                : RenderUtils.lerpColour(RenderUtils.SURFACE, RenderUtils.FRAME, (float) (fade * 0.8D));
        RenderUtils.roundedPanel(graphics, x, y, width, THEME_CARD_HEIGHT, fill,
                selected ? RenderUtils.ACCENT : hovered ? RenderUtils.HOVER : RenderUtils.OUTLINE, PANEL_RADIUS);
        RenderUtils.text(graphics, theme.getDisplayName(), x + 10.0D, y + 9.0D,
                selected ? RenderUtils.TEXT : RenderUtils.lerpColour(RenderUtils.TEXT_DISABLED, RenderUtils.TEXT,
                        (float) fade));
        if (selected) {
            Icons.draw(graphics, Icons.Icon.CHECK, x + width - 20.0D, y + 7.0D, 12.0D, RenderUtils.ACCENT_2);
        }
        int[] swatches = {theme.getBackground(), theme.getSurface(), theme.getAccent(), theme.getAccent2()};
        double swatchX = x + 10.0D;
        for (int colour : swatches) {
            RenderUtils.roundedPanel(graphics, swatchX, y + 30.0D, 18.0D, 12.0D, colour, RenderUtils.OUTLINE, 4);
            swatchX += 22.0D;
        }
    }

    /**
     * Draws the keybind page: every module with its bind, clickable to rebind.
     *
     * @param graphics the render context
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     */
    private void renderKeybinds(GuiGraphics graphics, int mouseX, int mouseY) {
        double top = contentTop();
        viewportHeight = contentBottom() - top;
        scroll = settle(scroll, scrollTarget, contentHeight, viewportHeight);
        double startY = top - scroll;
        double bottom = top + viewportHeight;
        double left = contentX() + PADDING;
        double usable = contentWidth() - PADDING * 2.0D - 6.0D;
        Argon client = Argon.get();
        if (client == null) {
            return;
        }
        List<Module> modules = client.getModuleManager().getSortedModules();

        RenderUtils.scissor(graphics, contentX() + 1.0D, top, contentWidth() - 4.0D, viewportHeight);
        double cursor = startY;
        for (Module module : modules) {
            if (cursor + KEYBIND_ROW_HEIGHT >= top && cursor <= bottom) {
                renderKeybindRow(graphics, module, left, cursor, usable, mouseX, mouseY);
            }
            cursor += KEYBIND_ROW_HEIGHT + 4.0D;
        }
        contentHeight = cursor - startY;
        RenderUtils.resetScissor(graphics);
    }

    /**
     * Draws one keybind row.
     *
     * @param graphics the render context
     * @param module   the module
     * @param x        the left edge
     * @param y        the top edge
     * @param width    the row width
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     */
    private void renderKeybindRow(GuiGraphics graphics, Module module, double x, double y, double width, int mouseX,
                                  int mouseY) {
        KeybindSetting bind = module.getKeybind();
        double fieldWidth = 80.0D;
        double fieldX = x + width - fieldWidth - 8.0D;
        double fieldY = y + 4.0D;
        boolean listening = listeningBind == bind;
        boolean hovered = inside(mouseX, mouseY, fieldX, fieldY, fieldWidth, 16.0D);
        RenderUtils.roundedPanel(graphics, x, y, width, KEYBIND_ROW_HEIGHT, RenderUtils.SURFACE, RenderUtils.OUTLINE,
                8);
        RenderUtils.text(graphics, module.getName(), x + 10.0D, y + 9.0D,
                module.isEnabled() ? RenderUtils.TEXT : RenderUtils.TEXT_DISABLED);
        RenderUtils.text(graphics, module.getCategory().getDisplayName(), x + 10.0D + RenderUtils.textWidth(module.getName()) + 8.0D,
                y + 9.0D, RenderUtils.withAlpha(RenderUtils.TEXT_DISABLED, 140));
        RenderUtils.roundedPanel(graphics, fieldX, fieldY, fieldWidth, 16.0D, RenderUtils.SURFACE,
                listening || hovered ? RenderUtils.HOVER : RenderUtils.OUTLINE, 7);
        RenderUtils.centeredText(graphics, listening ? "..." : bind.getKeyName(), fieldX + fieldWidth / 2.0D,
                fieldY + 4.0D, listening ? RenderUtils.HOVER : RenderUtils.TEXT);
        hit("bind:" + key(module), fieldX, fieldY, fieldWidth, 16.0D);
    }

    /**
     * Draws the settings page of the selected module.
     *
     * @param graphics the render context
     * @param mouseX   the mouse x position
     * @param mouseY   the mouse y position
     */
    private void renderDetail(GuiGraphics graphics, int mouseX, int mouseY) {
        double chipY = contentTop() + 2.0D;
        double chipWidth = RenderUtils.textWidth("Back") + 30.0D;
        boolean backHovered = inside(mouseX, mouseY, contentX() + PADDING, chipY, chipWidth, 16.0D);
        RenderUtils.roundedPanel(graphics, contentX() + PADDING, chipY, chipWidth, 16.0D,
                backHovered ? RenderUtils.withAlpha(RenderUtils.ACCENT, 60) : RenderUtils.SURFACE,
                backHovered ? RenderUtils.HOVER : RenderUtils.OUTLINE, 8);
        Icons.draw(graphics, Icons.Icon.BACK, contentX() + PADDING + 7.0D, chipY + 2.0D, 12.0D,
                backHovered ? RenderUtils.TEXT : RenderUtils.TEXT_DISABLED);
        RenderUtils.text(graphics, "Back", contentX() + PADDING + 22.0D, chipY + 4.0D,
                backHovered ? RenderUtils.TEXT : RenderUtils.TEXT_DISABLED);
        hit("back", contentX() + PADDING, chipY, chipWidth, 16.0D);

        double panelX = contentX() + PADDING;
        double panelWidth = contentWidth() - PADDING * 2.0D - 6.0D;
        double panelTop = detailPanelTop();
        double panelBottom = contentBottom() - 4.0D;
        RenderUtils.roundedPanel(graphics, panelX, panelTop, panelWidth, panelBottom - panelTop, RenderUtils.SURFACE,
                RenderUtils.OUTLINE, PANEL_RADIUS);
        RenderUtils.text(graphics, detail.getName(), panelX + 12.0D, panelTop + 9.0D, RenderUtils.TEXT);
        RenderUtils.text(graphics, SettingRow.trim(detail.getDescription(), panelWidth - 96.0D), panelX + 12.0D,
                panelTop + 22.0D, RenderUtils.TEXT_DISABLED);
        double switchX = panelX + panelWidth - 12.0D - SWITCH_WIDTH;
        SettingRow.switchControl(graphics, switchX, panelTop + 12.0D, SWITCH_WIDTH, SWITCH_HEIGHT,
                detail.isEnabled());
        hit("switch:" + key(detail), switchX, panelTop + 12.0D, SWITCH_WIDTH, SWITCH_HEIGHT);
        RenderUtils.rounded(graphics, panelX + 10.0D, panelTop + 40.0D, panelWidth - 20.0D, 1.0D,
                RenderUtils.withAlpha(RenderUtils.OUTLINE, 150), 1);

        double bodyTop = detailBodyTop();
        double bodyBottom = detailBodyBottom();
        detailViewportHeight = Math.max(1.0D, bodyBottom - bodyTop);
        detailScroll = settle(detailScroll, detailScrollTarget, detailContentHeight, detailViewportHeight);
        double cursor = bodyTop - detailScroll;
        RenderUtils.scissor(graphics, panelX + 2.0D, bodyTop, panelWidth - 4.0D, detailViewportHeight);
        for (SettingRow row : detailRows) {
            if (!row.getSetting().isVisible()) {
                continue;
            }
            row.setBounds(panelX + 10.0D, cursor, panelWidth - 20.0D);
            if (cursor + row.getHeight() >= bodyTop && cursor <= bodyBottom) {
                row.render(graphics, mouseX, mouseY);
            }
            cursor += row.getHeight();
        }
        RenderUtils.resetScissor(graphics);
        detailContentHeight = cursor + detailScroll - bodyTop;
    }

    /**
     * Draws the scrollbar of whichever area is scrollable.
     *
     * @param graphics the render context
     */
    private void renderScrollbar(GuiGraphics graphics) {
        if (detail != null) {
            scrollbar(graphics, detailBodyTop(), detailViewportHeight, detailContentHeight, detailScroll);
            return;
        }
        scrollbar(graphics, contentTop(), viewportHeight, contentHeight, scroll);
    }

    /**
     * Draws one scrollbar.
     *
     * @param graphics the render context
     * @param top      the top of the track
     * @param view     the viewport height
     * @param content  the content height
     * @param value    the current scroll value
     */
    private void scrollbar(GuiGraphics graphics, double top, double view, double content, double value) {
        double max = Math.max(0.0D, content - view);
        if (max <= 0.5D) {
            return;
        }
        double trackX = windowX + windowWidth - 7.0D;
        double trackTop = top + 2.0D;
        double trackHeight = view - 4.0D;
        double barHeight = Math.max(24.0D, trackHeight * (view / Math.max(1.0D, content)));
        double barY = trackTop + (value / max) * (trackHeight - barHeight);
        RenderUtils.rounded(graphics, trackX, trackTop, 3.0D, trackHeight,
                RenderUtils.withAlpha(RenderUtils.OUTLINE, 110), 1);
        RenderUtils.gradient(graphics, trackX, barY, 3.0D, barHeight, RenderUtils.ACCENT, RenderUtils.ACCENT_2);
    }

    /**
     * @return the title of the selected page, without the accent suffix
     */
    private String titleText() {
        if (general != null) {
            return general.displayName();
        }
        return category == null ? Category.COMBAT.getDisplayName() : category.getDisplayName();
    }

    /**
     * @return the accent part of the title, drawn in the secondary accent
     */
    private String titleSuffix() {
        return general == null ? " Modules" : "";
    }

    /**
     * @return the counter line under the title
     */
    private String counterText() {
        if (general == null) {
            int enabled = 0;
            for (ModuleCard card : cards) {
                if (card.getModule().isEnabled()) {
                    enabled++;
                }
            }
            return cards.size() + " modules  \u00b7  " + enabled + " enabled";
        }
        Argon client = Argon.get();
        return switch (general) {
            case SETTINGS -> "client preferences and notifications";
            case THEMES -> Theme.values().length + " themes  \u00b7  " + currentThemeName() + " selected";
            case CONFIGS -> profiles.size() + " profiles  \u00b7  the Config module saves and loads them";
            case SOCIALS -> client == null ? ""
                    : client.getFriendManager().getFriends().size() + " friends  \u00b7  "
                            + client.getFriendManager().getEnemies().size() + " enemies";
            case KEYBINDS -> boundCount() + " binds  \u00b7  click a field, then press a key";
        };
    }

    /**
     * @return the display name of the active theme
     */
    private String currentThemeName() {
        ClickGui module = clickGui();
        return module == null ? Theme.FERRO.getDisplayName() : module.getThemeName();
    }

    /**
     * @return the amount of modules with a bind
     */
    private int boundCount() {
        Argon client = Argon.get();
        if (client == null) {
            return 0;
        }
        int bound = 0;
        for (Module module : client.getModuleManager().getModules()) {
            if (module.getKeybind().isBound()) {
                bound++;
            }
        }
        return bound;
    }

    /**
     * @param category the category
     * @return the amount of modules in that category
     */
    private int count(Category category) {
        Argon client = Argon.get();
        return client == null ? 0 : client.getModuleManager().getModules(category).size();
    }

    /**
     * @param category the category
     * @return the glyph shown next to its sidebar row
     */
    private static Icons.Icon icon(Category category) {
        return switch (category) {
            case COMBAT -> Icons.Icon.SWORD;
            case MOVEMENT -> Icons.Icon.RUN;
            case RENDER -> Icons.Icon.EYE;
            case PLAYER -> Icons.Icon.PLAYER;
            case WORLD -> Icons.Icon.GLOBE;
            case MISC -> Icons.Icon.SPARK;
            case CLIENT -> Icons.Icon.GEAR;
        };
    }

    /**
     * @return {@code true} when the page has a search box
     */
    private boolean hasSearch() {
        return general == null || general == General.SETTINGS || general == General.CONFIGS
                || general == General.SOCIALS;
    }

    /**
     * @return the hint line shown at the bottom of the window
     */
    private String footerText() {
        if (detail != null) {
            return "click Back or press escape to return to the list";
        }
        return "click a card to open its settings  \u00b7  the switch toggles the module  \u00b7  right shift closes";
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        updateWindow();
        if (!inside(mouseX, mouseY, windowX, windowY, windowWidth, windowHeight)) {
            return false;
        }

        if (detail != null && mouseY >= detailBodyTop() && mouseY <= detailBodyBottom()) {
            for (int index = detailRows.size() - 1; index >= 0; index--) {
                if (detailRows.get(index).mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        String target = hitAt(mouseX, mouseY);
        if (target == null) {
            searchFocused = false;
            return true;
        }
        if (target.equals("back")) {
            detail = null;
            detailRows.clear();
            listeningBind = null;
            return true;
        }
        if (target.equals("search")) {
            searchFocused = true;
            return true;
        }
        if (target.startsWith("nav:category:")) {
            category = Category.valueOf(target.substring("nav:category:".length()));
            general = null;
            rebuild();
            return true;
        }
        if (target.startsWith("nav:general:")) {
            general = General.valueOf(target.substring("nav:general:".length()));
            rebuild();
            return true;
        }
        if (target.startsWith("card:")) {
            Module module = modulesByKey.get(target.substring("card:".length()));
            if (module != null) {
                openDetail(module);
            }
            return true;
        }
        if (target.startsWith("switch:")) {
            Module module = modulesByKey.get(target.substring("switch:".length()));
            if (module != null) {
                module.toggle();
                dev.ferro.client.utils.Notifications.toggle(module.getName(), module.isEnabled());
            }
            return true;
        }
        if (target.startsWith("sort:")) {
            Sort wanted = Sort.valueOf(target.substring("sort:".length()));
            sort = sort == wanted ? Sort.DEFAULT : wanted;
            scrollTarget = 0.0D;
            return true;
        }
        if (target.startsWith("view:")) {
            view = View.valueOf(target.substring("view:".length()));
            scrollTarget = 0.0D;
            return true;
        }
        if (target.startsWith("theme:")) {
            ClickGui module = clickGui();
            if (module != null) {
                module.setTheme(target.substring("theme:".length()));
            }
            return true;
        }
        if (target.startsWith("profile:load:")) {
            Config.loadProfile(target.substring("profile:load:".length()));
            refreshProfiles();
            return true;
        }
        if (target.startsWith("profile:save:")) {
            Config.saveProfile(target.substring("profile:save:".length()));
            refreshProfiles();
            return true;
        }
        if (target.startsWith("bind:")) {
            Module module = modulesByKey.get(target.substring("bind:".length()));
            if (module != null) {
                if (button == 1) {
                    module.getKeybind().set(KeybindSetting.NONE);
                } else {
                    listeningBind = module.getKeybind();
                }
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (detail == null) {
            return false;
        }
        for (SettingRow row : detailRows) {
            if (row.mouseReleased(event.x(), event.y(), event.button())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (detail == null) {
            return false;
        }
        for (SettingRow row : detailRows) {
            if (row.mouseDragged(event.x(), event.y())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        updateWindow();
        if (!inside(mouseX, mouseY, windowX, windowY, windowWidth, windowHeight)) {
            return false;
        }
        if (detail != null) {
            detailScrollTarget -= scrollY * 14.0D;
            detailScrollTarget = clampTarget(detailScrollTarget, detailContentHeight, detailViewportHeight);
            return true;
        }
        scrollTarget -= scrollY * 14.0D;
        scrollTarget = clampTarget(scrollTarget, contentHeight, viewportHeight);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (listeningBind != null) {
            if (key == KEY_ESCAPE) {
                listeningBind = null;
                return true;
            }
            if (key == KEY_BACKSPACE) {
                listeningBind.set(KeybindSetting.NONE);
                listeningBind = null;
                return true;
            }
            listeningBind.set(key);
            listeningBind = null;
            return true;
        }
        if (detail != null) {
            for (int index = detailRows.size() - 1; index >= 0; index--) {
                if (detailRows.get(index).keyPressed(key, event.scancode(), event.modifiers())) {
                    return true;
                }
            }
        }
        if (searchFocused) {
            if (key == KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
            if (key == KEY_BACKSPACE) {
                if (search.length() > 0) {
                    search.deleteCharAt(search.length() - 1);
                }
                scrollTarget = 0.0D;
                return true;
            }
            return true;
        }
        if (key == KEY_ESCAPE && detail != null) {
            detail = null;
            detailRows.clear();
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        String text = event.codepointAsString();
        if (text.isEmpty()) {
            return false;
        }
        char character = text.charAt(0);
        if (detail != null) {
            for (int index = detailRows.size() - 1; index >= 0; index--) {
                if (detailRows.get(index).charTyped(character)) {
                    return true;
                }
            }
        }
        if (searchFocused) {
            if (character >= ' ' && search.length() < 32) {
                search.append(character);
                scrollTarget = 0.0D;
            }
            return true;
        }
        return false;
    }

    @Override
    public void save() {
        Argon client = Argon.get();
        if (client == null) {
            return;
        }
        JsonObject ui = client.getModuleManager().getUiSection();
        JsonObject modern = new JsonObject();
        if (general == null) {
            modern.addProperty("category", category.name());
        } else {
            modern.addProperty("general", general.name());
        }
        modern.addProperty("sort", sort.name());
        modern.addProperty("view", view.name());
        ui.add("modern", modern);
    }

    @Override
    public String hint() {
        return "";
    }
}
