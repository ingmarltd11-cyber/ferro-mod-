package dev.ferro.client.module;

import com.google.gson.JsonObject;
import dev.ferro.client.Argon;
import dev.ferro.client.event.Event;
import dev.ferro.client.event.EventManager;
import dev.ferro.client.event.Listener;
import dev.ferro.client.module.setting.KeybindSetting;
import dev.ferro.client.module.setting.Setting;
import dev.ferro.client.utils.EncryptedString;
import dev.ferro.client.utils.Log;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class of every FERRO module.
 *
 * <p>A module owns a name, a category, a list of {@link Setting settings}, one dedicated
 * {@link KeybindSetting} and an enabled state. Enabling and disabling is where the module
 * subscribes to the events it needs; the default {@link #onEnable()} and {@link #onDisable()}
 * implementations are empty so simple modules only override what they use.</p>
 *
 * <p>All user visible strings are wrapped in {@link EncryptedString} so that module names and
 * descriptions do not appear as plain text in the compiled jar.</p>
 */
public abstract class Module implements Listener {

    private final EncryptedString name;
    private final EncryptedString description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final KeybindSetting keybind;

    private boolean enabled;
    private boolean visibleInGui = true;

    /**
     * @param name        the module name
     * @param category    the category the module belongs to
     * @param description a short description shown in the ClickGUI
     */
    protected Module(String name, Category category, String description) {
        this.name = EncryptedString.of(name);
        this.category = category;
        this.description = EncryptedString.of(description);
        this.keybind = new KeybindSetting("Keybind", KeybindSetting.NONE);
    }

    /**
     * @return the shared Minecraft instance
     */
    protected static Minecraft mc() {
        return Minecraft.getInstance();
    }

    /**
     * @return the global event bus
     */
    protected static EventManager eventManager() {
        return EventManager.getInstance();
    }

    /**
     * @return the client instance
     */
    protected static Argon ferro() {
        return Argon.get();
    }

    /**
     * Registers settings. Called from the constructor of a concrete module; the order of the
     * arguments is the order used by the ClickGUI.
     *
     * @param newSettings the settings to add, {@code null} entries are ignored
     */
    protected final void addSettings(Setting<?>... newSettings) {
        for (Setting<?> setting : newSettings) {
            if (setting != null) {
                settings.add(setting);
            }
        }
    }

    /**
     * Subscribes to an event class with the default priority.
     *
     * @param eventType the event class, for example {@code TickEvent.class}
     */
    protected final void listen(Class<? extends Event<?>> eventType) {
        eventManager().add(eventType, this);
    }

    /**
     * Removes every event subscription of this module.
     */
    protected final void unlisten() {
        eventManager().remove(this);
    }

    /**
     * Toggles the module.
     */
    public final void toggle() {
        setEnabled(!enabled);
    }

    /**
     * Enables or disables the module, running the matching lifecycle hook.
     *
     * @param value the new state
     */
    public final void setEnabled(boolean value) {
        if (this.enabled == value) {
            return;
        }
        this.enabled = value;
        try {
            if (value) {
                onEnable();
            } else {
                onDisable();
            }
        } catch (Throwable throwable) {
            Log.error("Exception in " + getName() + (value ? " onEnable" : " onDisable"), throwable);
            this.enabled = false;
            unlisten();
        }
    }

    /**
     * Called when the module is switched on. Register event listeners here.
     */
    protected void onEnable() {
    }

    /**
     * Called when the module is switched off. Unregister event listeners here.
     */
    protected void onDisable() {
    }

    /**
     * @return {@code true} when the module is currently running
     */
    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * @return the module name
     */
    public final String getName() {
        return name.getValue();
    }

    /**
     * @return the module description
     */
    public final String getDescription() {
        return description.getValue();
    }

    /**
     * @return the category of this module
     */
    public final Category getCategory() {
        return category;
    }

    /**
     * @return an unmodifiable view of the settings, in registration order
     */
    public final List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    /**
     * @return the dedicated keybind setting of this module
     */
    public final KeybindSetting getKeybind() {
        return keybind;
    }

    /**
     * @return {@code true} when the module should be listed in the ClickGUI
     */
    public final boolean isVisibleInGui() {
        return visibleInGui;
    }

    /**
     * @param visibleInGui whether the module should be listed in the ClickGUI
     */
    public final void setVisibleInGui(boolean visibleInGui) {
        this.visibleInGui = visibleInGui;
    }

    /**
     * Serialises the enabled state and every setting.
     *
     * @return a JSON object for the config file
     */
    public final JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("enabled", enabled);
        object.addProperty("keybind", keybind.get());
        JsonObject settingsObject = new JsonObject();
        for (Setting<?> setting : settings) {
            settingsObject.add(setting.getName(), setting.toJson());
        }
        object.add("settings", settingsObject);
        return object;
    }

    /**
     * Restores the state saved by {@link #toJson()}. Enabled state is applied last so that
     * modules can read their restored settings inside {@link #onEnable()}.
     *
     * @param object the JSON object from the config file
     */
    public final void loadJson(JsonObject object) {
        if (object == null) {
            return;
        }
        if (object.has("keybind")) {
            keybind.set(object.get("keybind").getAsInt());
        }
        if (object.has("settings") && object.get("settings").isJsonObject()) {
            JsonObject settingsObject = object.getAsJsonObject("settings");
            for (Setting<?> setting : settings) {
                if (settingsObject.has(setting.getName())) {
                    try {
                        setting.fromJson(settingsObject.get(setting.getName()));
                    } catch (Throwable throwable) {
                        Log.warn("Failed to load setting {}.{}: {}", getName(), setting.getName(),
                                throwable.getMessage());
                    }
                }
            }
        }
        if (object.has("enabled")) {
            setEnabled(object.get("enabled").getAsBoolean());
        }
    }
}
