package dev.ferro.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.List;

/**
 * A fixed set of named options, rendered as a dropdown with previous/next arrows.
 */
public class ModeSetting extends Setting<String> {

    private final List<String> modes;

    /**
     * @param name         the label
     * @param defaultValue the default mode, must be part of {@code modes}
     * @param modes        the available modes
     */
    public ModeSetting(String name, String defaultValue, String... modes) {
        super(name, modes.length > 0 && Arrays.asList(modes).contains(defaultValue) ? defaultValue : modes[0],
                new String[0]);
        this.modes = List.of(modes);
    }

    /**
     * @return the available modes, in display order
     */
    public List<String> getModes() {
        return modes;
    }

    /**
     * @return the index of the current mode
     */
    public int getIndex() {
        return Math.max(0, modes.indexOf(get()));
    }

    /**
     * @param index the new mode index, wrapped into range
     */
    public void setIndex(int index) {
        if (modes.isEmpty()) {
            return;
        }
        int wrapped = ((index % modes.size()) + modes.size()) % modes.size();
        set(modes.get(wrapped));
    }

    /**
     * Selects the next mode.
     */
    public void next() {
        setIndex(getIndex() + 1);
    }

    /**
     * Selects the previous mode.
     */
    public void previous() {
        setIndex(getIndex() - 1);
    }

    /**
     * @param mode the mode to test
     * @return {@code true} when the given mode is currently selected, case insensitive
     */
    public boolean is(String mode) {
        return get().equalsIgnoreCase(mode);
    }

    @Override
    public void set(String value) {
        if (value != null && modes.contains(value)) {
            super.set(value);
        }
    }

    @Override
    public String displayValue() {
        return get();
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement element) {
        String value = element.getAsString();
        if (modes.contains(value)) {
            set(value);
        }
    }
}
