package dev.ferro.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A free text value, typed directly in the ClickGUI.
 */
public class StringSetting extends Setting<String> {

    private final int maxLength;

    /**
     * @param name         the label
     * @param defaultValue the default text
     * @param maxLength    the maximum amount of characters
     * @param description  optional description
     */
    public StringSetting(String name, String defaultValue, int maxLength, String... description) {
        super(name, defaultValue, description);
        this.maxLength = Math.max(1, maxLength);
    }

    /**
     * @return the maximum amount of characters
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * @param value the new text, truncated to the maximum length
     */
    public void set(String value) {
        super.set(value.length() > maxLength ? value.substring(0, maxLength) : value);
    }

    @Override
    public String displayValue() {
        return get().isEmpty() ? "-" : get();
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement element) {
        set(element.getAsString());
    }
}
