package dev.ferro.client.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ferro.client.module.setting.KeybindSetting;
import dev.ferro.client.utils.Log;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.ferro.client.module.modules.combat.AimAssist;
import dev.ferro.client.module.modules.combat.AutoMace;
import dev.ferro.client.module.modules.combat.KillAura;
import dev.ferro.client.module.modules.combat.AutoTotem;
import dev.ferro.client.module.modules.combat.ShieldDrain;
import dev.ferro.client.module.modules.combat.TriggerBot;
import dev.ferro.client.module.modules.movement.InstantLunge;
import dev.ferro.client.module.modules.render.Freecam;
import dev.ferro.client.module.modules.client.ClickGui;
import dev.ferro.client.module.modules.client.Notifications;
import dev.ferro.client.module.modules.client.Config;
import dev.ferro.client.module.modules.client.Friends;
import dev.ferro.client.module.modules.client.Enemies;

/**
 * Registry of every module, plus the config file that stores their state.
 *
 * <p>The manager is the single owner of {@code config/ferro/config.json}. Module state lives under the
 * {@code modules} key and the ClickGUI layout under the {@code ui} key, so there is exactly one writer for
 * the file.</p>
 *
 * <p>Every gameplay module is parked in {@code removed-modules/} and is not registered, so this registry
 * only holds the client side modules the menu itself needs. Restoring one is a move back plus a single
 * {@code register(...)} call.</p>
 */
public final class ModuleManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** GLFW code of the escape key, ignored so that closing a screen never toggles a module. */
    private static final int KEY_ESCAPE = 256;

    private final Map<Class<? extends Module>, Module> modules = new LinkedHashMap<>();
    private JsonObject uiSection = new JsonObject();

    /**
     * Creates the registry and registers every built-in module.
     */
    public ModuleManager() {
        registerAll();
    }

    /**
     * Registers the client modules. Each module is constructed with its own settings; nothing is
     * enabled until a config or a keybind says so.
     *
     * <p>The gameplay modules are deliberately not registered yet: this registry is the shell they get
     * dropped back into, one {@code register(new ...)} line each. The five client modules are the ones the
     * interface needs, so the GUI can still open and manage configs, socials and its own settings.</p>
     *
     * <p>The mace modules are the first gameplay modules coming back, one at a time: AutoMace is in, the
     * rest of combat is still parked in {@code removed-modules/}.</p>
     */
    private void registerAll() {
        register(new ClickGui());
        register(new Notifications());
        register(new Config());
        register(new Friends());
        register(new Enemies());
        register(new AutoMace());
        register(new KillAura());
        register(new TriggerBot());
        register(new ShieldDrain());
        register(new AutoTotem());
        register(new AimAssist());
        register(new Freecam());
        register(new InstantLunge());
    }

    /**
     * Adds a module to the registry.
     *
     * @param module the module instance
     */
    public void register(Module module) {
        if (module == null) {
            return;
        }
        Module previous = modules.putIfAbsent(module.getClass(), module);
        if (previous != null) {
            Log.warn("Module {} is registered twice", module.getClass().getSimpleName());
        }
    }

    /**
     * @param type the module class
     * @param <T>  the module type
     * @return the registered instance, or {@code null} when unknown
     */
    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        return (T) modules.get(type);
    }

    /**
     * @param name the module name
     * @return the module with that name, or {@code null}
     */
    public Module getByName(String name) {
        for (Module module : modules.values()) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    /**
     * @return every module, in registration order
     */
    public List<Module> getModules() {
        return List.copyOf(modules.values());
    }

    /**
     * @param category the category to filter by
     * @return the modules of that category, in registration order
     */
    public List<Module> getModules(Category category) {
        return modules.values().stream()
                .filter(module -> module.getCategory() == category && module.isVisibleInGui())
                .collect(Collectors.toList());
    }

    /**
     * @return every module sorted alphabetically, used by the array list HUD
     */
    public List<Module> getSortedModules() {
        List<Module> sorted = new ArrayList<>(modules.values());
        sorted.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
        return sorted;
    }

    /**
     * @param name the module name
     * @return {@code true} when a module with that name is currently enabled
     */
    public boolean isEnabled(String name) {
        Module module = getByName(name);
        return module != null && module.isEnabled();
    }

    /**
     * @return the amount of registered modules
     */
    public int size() {
        return modules.size();
    }

    /**
     * @return the amount of enabled modules
     */
    public int enabledCount() {
        return (int) modules.values().stream().filter(Module::isEnabled).count();
    }

    /**
     * Toggles every module bound to the given key. Ignored while a screen is open so that typing in the
     * ClickGUI never toggles modules.
     *
     * @param key the GLFW key code
     */
    public void onKey(int key) {
        if (key == KEY_ESCAPE || key == KeybindSetting.NONE) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        for (Module module : modules.values()) {
            KeybindSetting bind = module.getKeybind();
            if (!bind.isBound() || KeybindSetting.isMouse(bind.get()) || bind.get() != key) {
                continue;
            }
            if (bind.isHold()) {
                module.setEnabled(true);
            } else {
                module.toggle();
            }
        }
    }

    /**
     * Handles the release half of hold bindings: a module bound with {@code Hold} switches off again.
     *
     * @param key the GLFW key code
     */
    public void onKeyRelease(int key) {
        if (key == KEY_ESCAPE || key == KeybindSetting.NONE) {
            return;
        }
        for (Module module : modules.values()) {
            KeybindSetting bind = module.getKeybind();
            if (!bind.isBound() || KeybindSetting.isMouse(bind.get()) || bind.get() != key || !bind.isHold()) {
                continue;
            }
            module.setEnabled(false);
        }
    }

    /**
     * Toggles every module bound to the given mouse button.
     *
     * @param button the GLFW mouse button index
     */
    public void onMouse(int button) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        int code = KeybindSetting.mouseButton(button);
        for (Module module : modules.values()) {
            KeybindSetting bind = module.getKeybind();
            if (bind.get() != code) {
                continue;
            }
            if (bind.isHold()) {
                module.setEnabled(true);
            } else {
                module.toggle();
            }
        }
    }

    /**
     * Handles the release half of hold bindings on a mouse button.
     *
     * @param button the GLFW mouse button index
     */
    public void onMouseRelease(int button) {
        int code = KeybindSetting.mouseButton(button);
        for (Module module : modules.values()) {
            KeybindSetting bind = module.getKeybind();
            if (bind.get() != code || !bind.isHold()) {
                continue;
            }
            module.setEnabled(false);
        }
    }

    /**
     * @return the config file path
     */
    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("ferro/config.json");
    }

    /**
     * @return the mutable JSON object that stores ClickGUI layout data
     */
    public JsonObject getUiSection() {
        return uiSection;
    }

    /**
     * Writes every module state and the UI section to disk.
     */
    public void save() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("modules_registered", modules.size());
        JsonObject moduleSection = new JsonObject();
        for (Module module : modules.values()) {
            moduleSection.add(module.getClass().getSimpleName(), module.toJson());
        }
        root.add("modules", moduleSection);
        root.add("ui", uiSection);
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
            Log.info("Saved {} modules to {}", modules.size(), path);
        } catch (IOException exception) {
            Log.error("Failed to save config", exception);
        }
    }

    /**
     * Reads module state and the UI section from disk. Missing files are ignored; every module keeps its
     * defaults.
     */
    public void load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            return;
        }
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            if (root.has("modules") && root.get("modules").isJsonObject()) {
                JsonObject moduleSection = root.getAsJsonObject("modules");
                for (Module module : modules.values()) {
                    String key = module.getClass().getSimpleName();
                    if (moduleSection.has(key) && moduleSection.get(key).isJsonObject()) {
                        module.loadJson(moduleSection.getAsJsonObject(key));
                    }
                }
            }
            if (root.has("ui") && root.get("ui").isJsonObject()) {
                uiSection = root.getAsJsonObject("ui");
            }
            Log.info("Loaded {} modules from {}", modules.size(), path);
        } catch (Throwable throwable) {
            Log.error("Failed to load config", throwable);
        }
    }

    /**
     * Disables every module, used before saving or reloading a config.
     */
    public void disableAll() {
        for (Module module : modules.values()) {
            module.setEnabled(false);
        }
    }

    /**
     * @return a snapshot of all module names mapped to their enabled state
     */
    public Map<String, Boolean> snapshot() {
        Map<String, Boolean> snapshot = new LinkedHashMap<>();
        for (Module module : modules.values()) {
            snapshot.put(module.getName(), module.isEnabled());
        }
        return snapshot;
    }
}
