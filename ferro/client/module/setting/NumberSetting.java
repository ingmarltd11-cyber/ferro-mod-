package dev.ferro.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.ferro.client.utils.MathUtils;

/**
 * A bounded numeric value rendered as a slider.
 */
public class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;
    private final boolean integer;

    /**
     * @param name         the label
     * @param min          the minimum value
     * @param max          the maximum value
     * @param step         the slider increment, use {@code 0.1} for percentages and {@code 1} for counts
     * @param defaultValue the default value
     * @param description  optional description
     */
    public NumberSetting(String name, double min, double max, double step, double defaultValue, String... description) {
        super(name, MathUtils.clamp(defaultValue, min, max), description);
        this.min = min;
        this.max = max;
        this.step = step <= 0.0D ? 0.1D : step;
        this.integer = this.step >= 1.0D;
    }

    /**
     * @return the minimum value
     */
    public double getMin() {
        return min;
    }

    /**
     * @return the maximum value
     */
    public double getMax() {
        return max;
    }

    /**
     * @return the slider increment
     */
    public double getStep() {
        return step;
    }

    /**
     * @return {@code true} when the slider only produces whole numbers
     */
    public boolean isInteger() {
        return integer;
    }

    /**
     * @param value the new raw value, clamped and snapped to the step size
     */
    public void set(double value) {
        super.set(MathUtils.roundToStep(MathUtils.clamp(value, min, max), step, min));
    }

    /**
     * @return the value as {@code double}
     */
    public double getDouble() {
        return get();
    }

    /**
     * @return the value as {@code float}
     */
    public float getFloat() {
        return get().floatValue();
    }

    /**
     * @return the value as rounded {@code int}
     */
    public int getInt() {
        return (int) Math.round(get());
    }

    /**
     * @return the value relative to the range, between {@code 0} and {@code 1}
     */
    public float getPercentage() {
        if (max - min <= 0.0D) {
            return 0.0F;
        }
        return (float) ((get() - min) / (max - min));
    }

    /**
     * @param percentage a value between {@code 0} and {@code 1}
     */
    public void setPercentage(double percentage) {
        set(min + MathUtils.clamp(percentage, 0.0D, 1.0D) * (max - min));
    }

    @Override
    public String displayValue() {
        if (integer) {
            return Integer.toString(getInt());
        }
        return String.format(java.util.Locale.ROOT, "%.2f", get()).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement element) {
        set(element.getAsDouble());
    }
}
