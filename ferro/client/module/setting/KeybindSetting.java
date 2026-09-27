package dev.ferro.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.Locale;

/**
 * A key or mouse button binding.
 *
 * <p>Keyboard bindings store the raw GLFW key code. Mouse buttons are stored as negative codes so
 * that one integer can express both, using {@link #mouseButton(int)}.</p>
 *
 * <p>A binding is either a {@code Toggle} (press to switch the module on, press again to switch it off)
 * or a {@code Hold} (the module runs while the key is down and stops when it is released).</p>
 */
public class KeybindSetting extends Setting<Integer> {

    /** Value that means "no binding". */
    public static final int NONE = -1;

    private boolean hold;

    /**
     * @param name         the label
     * @param defaultValue the default binding, or {@link #NONE}
     * @param description  optional description
     */
    public KeybindSetting(String name, int defaultValue, String... description) {
        super(name, defaultValue, description);
    }

    /**
     * Encodes a GLFW mouse button index as a keybind code.
     *
     * @param button the GLFW mouse button index
     * @return the encoded code
     */
    public static int mouseButton(int button) {
        return -(button + 2);
    }

    /**
     * @param code the keybind code
     * @return {@code true} when the code represents a mouse button
     */
    public static boolean isMouse(int code) {
        return code <= -2;
    }

    /**
     * @param code the keybind code
     * @return the GLFW mouse button index
     */
    public static int mouseIndex(int code) {
        return -code - 2;
    }

    /**
     * @param value the new binding
     */
    public void set(int value) {
        super.set(value);
    }

    /**
     * @return {@code true} when a binding is set
     */
    public boolean isBound() {
        return get() != NONE;
    }

    /**
     * @return {@code true} when the module runs only while the key is held down
     */
    public boolean isHold() {
        return hold;
    }

    /**
     * @param value whether the binding behaves as a hold instead of a toggle
     */
    public void setHold(boolean value) {
        this.hold = value;
    }

    /**
     * Switches between the hold and toggle behaviour.
     */
    public void toggleMode() {
        this.hold = !this.hold;
    }

    /**
     * @return the label of the current binding mode, {@code Hold} or {@code Toggle}
     */
    public String getModeName() {
        return hold ? "Hold" : "Toggle";
    }

    /**
     * @return a human readable label for the binding
     */
    public String getKeyName() {
        int code = get();
        if (code == NONE) {
            return "NONE";
        }
        if (isMouse(code)) {
            return "MOUSE" + (mouseIndex(code) + 1);
        }
        try {
            return InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString()
                    .toUpperCase(Locale.ROOT);
        } catch (Throwable throwable) {
            return "KEY" + code;
        }
    }

    @Override
    public String displayValue() {
        return getKeyName();
    }

    @Override
    public JsonElement toJson() {
        if (!hold) {
            return new JsonPrimitive(get());
        }
        JsonObject object = new JsonObject();
        object.addProperty("key", get());
        object.addProperty("hold", true);
        return object;
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("key")) {
                set(object.get("key").getAsInt());
            }
            hold = object.has("hold") && object.get("hold").getAsBoolean();
            return;
        }
        set(element.getAsInt());
        hold = false;
    }
}
