package dev.ferro.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.ferro.client.utils.MathUtils;

/**
 * A setting with two ends: a low and a high value inside one allowed range.
 *
 * <p>Used where "somewhere between X and Y" is the honest description, such as the swap back delay of Instant
 * Lunge. The pair is serialised as {@code low,high}, and it is drawn as a slider with two handles.</p>
 */
public class MinMaxSetting extends Setting<double[]> {

    private final double min;
    private final double max;
    private final double step;

    /**
     * @param name         the label
     * @param min          the smallest allowed value
     * @param max          the largest allowed value
     * @param step         the step between two values, used for snapping and for formatting
     * @param defaultLow   the default low end
     * @param defaultHigh  the default high end
     * @param description  optional description
     */
    public MinMaxSetting(String name, double min, double max, double step, double defaultLow, double defaultHigh,
                         String... description) {
        super(name, new double[]{snap(defaultLow, min, max, step), snap(defaultHigh, min, max, step)}, description);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    /**
     * @param value the value
     * @param min   the smallest allowed value
     * @param max   the largest allowed value
     * @param step  the step between two values
     * @return the value clamped to the range and snapped to the step
     */
    private static double snap(double value, double min, double max, double step) {
        double clamped = MathUtils.clamp(value, min, max);
        if (step <= 0.0D) {
            return clamped;
        }
        return min + Math.round((clamped - min) / step) * step;
    }

    /**
     * @return the smallest allowed value
     */
    public double getMin() {
        return min;
    }

    /**
     * @return the largest allowed value
     */
    public double getMax() {
        return max;
    }

    /**
     * @return the step between two values
     */
    public double getStep() {
        return step;
    }

    /**
     * @return the low end of the range
     */
    public double getLow() {
        return get()[0];
    }

    /**
     * @return the high end of the range
     */
    public double getHigh() {
        return get()[1];
    }

    /**
     * @return {@code true} when the setting only produces whole numbers
     */
    public boolean isInteger() {
        return step >= 1.0D;
    }

    /**
     * @param value the new low end, clamped and kept at or below the high end
     */
    public void setLow(double value) {
        set(new double[]{Math.min(snap(value, min, max, step), getHigh()), getHigh()});
    }

    /**
     * @param value the new high end, clamped and kept at or above the low end
     */
    public void setHigh(double value) {
        set(new double[]{getLow(), Math.max(snap(value, min, max, step), getLow())});
    }

    /**
     * @return a random value inside the range, which is what a delay between two ends means in practice
     */
    public double random() {
        return MathUtils.random(getLow(), getHigh());
    }

    @Override
    public String displayValue() {
        return format(getLow()) + " \u2013 " + format(getHigh());
    }

    /**
     * @param value the value
     * @return the value formatted for the value box
     */
    private String format(double value) {
        if (isInteger()) {
            return String.valueOf((int) Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getLow() + "," + getHigh());
    }

    @Override
    public void fromJson(JsonElement element) {
        String raw = element.getAsString();
        String[] parts = raw.split(",");
        if (parts.length != 2) {
            return;
        }
        try {
            set(new double[]{snap(Double.parseDouble(parts[0].trim()), min, max, step),
                    snap(Double.parseDouble(parts[1].trim()), min, max, step)});
        } catch (NumberFormatException ignored) {
            // A malformed value simply keeps the default.
        }
    }
}
