package dev.ferro.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * An on/off switch, rendered as a checkbox in the ClickGUI.
 */
public class BooleanSetting extends Setting<Boolean> {

    /**
     * @param name         the label
     * @param defaultValue the default state
     * @param description  optional description
     */
    public BooleanSetting(String name, boolean defaultValue, String... description) {
        super(name, defaultValue, description);
    }

    /**
     * @param value the new state
     */
    public void set(boolean value) {
        super.set(value);
    }

    /**
     * Flips the current state.
     */
    public void toggle() {
        super.set(!get());
    }

    @Override
    public String displayValue() {
        return get() ? "On" : "Off";
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement element) {
        set(element.getAsBoolean());
    }
}
