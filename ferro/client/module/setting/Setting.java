package dev.ferro.client.module.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

/**
 * A single configurable value of a module.
 *
 * @param <T> the value type
 */
public abstract class Setting<T> {

    private final String name;
    private String description;
    private final T defaultValue;
    private T value;
    private BooleanSupplier visibility = () -> true;
    private java.util.function.Consumer<T> changeListener;

    /**
     * @param name         the label shown in the ClickGUI
     * @param defaultValue the value used when no config was loaded
     * @param description  optional description
     */
    protected Setting(String name, T defaultValue, String... description) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.description = description.length > 0 ? String.join(" ", description) : "";
    }

    /**
     * @return the setting name
     */
    public final String getName() {
        return name;
    }

    /**
     * @return the setting description, possibly empty
     */
    public final String getDescription() {
        return description;
    }

    /**
     * Replaces the description of a setting whose constructor cannot carry one, such as a
     * {@link ModeSetting} whose varargs are the modes themselves.
     *
     * @param description the new description
     * @return this setting, for fluent construction
     */
    public final Setting<T> described(String description) {
        this.description = description == null ? "" : description;
        return this;
    }

    /**
     * @return the default value
     */
    public final T getDefaultValue() {
        return defaultValue;
    }

    /**
     * @return the current value
     */
    public final T get() {
        return value;
    }

    /**
     * Sets the value and notifies the change listener. Subclasses override this to validate or
     * normalise what they store, and must call {@code super.set(...)}.
     *
     * @param value the new value
     */
    public void set(T value) {
        T previous = this.value;
        this.value = value;
        if (changeListener != null && !java.util.Objects.equals(previous, value)) {
            try {
                changeListener.accept(value);
            } catch (Throwable throwable) {
                dev.ferro.client.utils.Log.error("Change listener of setting " + name + " failed", throwable);
            }
        }
    }

    /**
     * Restores the default value.
     */
    public final void reset() {
        this.value = defaultValue;
    }

    /**
     * Makes this setting conditional: it is only shown and only relevant when the supplier
     * returns {@code true}. Used for settings that belong to a specific mode.
     *
     * @param supplier the visibility predicate
     * @return this setting, for fluent construction
     */
    public final Setting<T> visibleWhen(BooleanSupplier supplier) {
        this.visibility = supplier;
        return this;
    }

    /**
     * @return {@code true} when the ClickGUI should render this setting
     */
    public final boolean isVisible() {
        return visibility.getAsBoolean();
    }

    /**
     * Registers a callback that runs whenever the value actually changes. Used by action style
     * settings such as Config, Friends and Enemies.
     *
     * @param listener the callback
     * @return this setting, for fluent construction
     */
    public final Setting<T> onChange(java.util.function.Consumer<T> listener) {
        this.changeListener = listener;
        return this;
    }

    /**
     * @return the value formatted for the ClickGUI
     */
    public abstract String displayValue();

    /**
     * @return this setting as JSON
     */
    public abstract JsonElement toJson();

    /**
     * @param element the JSON value to restore
     */
    public abstract void fromJson(JsonElement element);

    @Override
    public final String toString() {
        return name + "=" + displayValue();
    }
}
