package dev.ferro.client.utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Small numeric helpers shared by modules and the ClickGUI.
 */
public final class MathUtils {

    private MathUtils() {
    }

    /**
     * @param value the value to constrain
     * @param min   the lower bound
     * @param max   the upper bound
     * @return the clamped value
     */
    public static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

    /**
     * @param value the value to constrain
     * @param min   the lower bound
     * @param max   the upper bound
     * @return the clamped value
     */
    public static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    /**
     * @param value the value to constrain
     * @param min   the lower bound
     * @param max   the upper bound
     * @return the clamped value
     */
    public static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    /**
     * Snaps a value to the nearest multiple of {@code step} above {@code base}.
     *
     * @param value the raw value
     * @param step  the increment
     * @param base  the offset the steps are counted from
     * @return the snapped value
     */
    public static double roundToStep(double value, double step, double base) {
        if (step <= 0.0D) {
            return value;
        }
        return base + Math.round((value - base) / step) * step;
    }

    /**
     * @param from  the start value
     * @param to    the end value
     * @param delta interpolation factor, {@code 0} returns {@code from}
     * @return the interpolated value
     */
    public static double lerp(double from, double to, double delta) {
        return from + (to - from) * clamp(delta, 0.0D, 1.0D);
    }

    /**
     * @param from  the start value
     * @param to    the end value
     * @param delta interpolation factor
     * @return the interpolated value
     */
    public static float lerp(float from, float to, float delta) {
        return from + (to - from) * clamp(delta, 0.0F, 1.0F);
    }

    /**
     * @param min the inclusive lower bound
     * @param max the exclusive upper bound
     * @return a random double
     */
    public static double random(double min, double max) {
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    /**
     * @param min the inclusive lower bound
     * @param max the inclusive upper bound
     * @return a random int
     */
    public static int randomInt(int min, int max) {
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * @param bound the exclusive upper bound
     * @return a random int
     */
    public static int randomInt(int bound) {
        return ThreadLocalRandom.current().nextInt(Math.max(1, bound));
    }

    /**
     * Normalises an angle to the range {@code -180..180}.
     *
     * @param degrees the raw angle
     * @return the wrapped angle
     */
    public static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    /**
     * Shortest signed distance between two angles.
     *
     * @param from the first angle
     * @param to   the second angle
     * @return the difference in the range {@code -180..180}
     */
    public static float angleDifference(float from, float to) {
        return wrapDegrees(to - from);
    }

    /**
     * Greates common divisor, used to snap rotations to the same grid vanilla mouse input uses.
     *
     * @param a first value
     * @param b second value
     * @return the gcd
     */
    public static long gcd(long a, long b) {
        while (b != 0L) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return Math.abs(a);
    }

    /**
     * Rounds a rotation so that it lands on the same increments as real mouse movement. Rotations
     * produced by aiming modules are otherwise suspiciously smooth.
     *
     * @param previous the previous rotation of that axis
     * @param target   the requested rotation
     * @return the snapped rotation
     */
    public static float gcdFix(float previous, float target) {
        float sensitivity = (float) (Math.pow(0.2D * 3.0D, 3.0D) * 8.0D);
        float current = previous;
        float delta = target - current;
        float gcd = (float) gcd((long) (sensitivity * 10000.0F), (long) (delta * 10000.0F)) / 10000.0F;
        if (gcd < 0.001F) {
            return target;
        }
        return current + Math.round(delta / gcd) * gcd;
    }

    /**
     * @param value the value to round
     * @param places the amount of decimals
     * @return the rounded value
     */
    public static double round(double value, int places) {
        double factor = Math.pow(10.0D, places);
        return Math.round(value * factor) / factor;
    }
}
