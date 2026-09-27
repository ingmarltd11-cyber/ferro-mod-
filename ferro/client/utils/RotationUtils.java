package dev.ferro.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Rotation math and silent rotation support.
 *
 * <p>Silent rotations never change the client rotation that is shown in the world. The module
 * computes a yaw/pitch pair, sends it in a dedicated movement packet and leaves the player's
 * {@code getYRot()}/{@code getXRot()} untouched, so nothing rotates on screen and vanilla keeps
 * sending its own, unchanged, rotation afterwards.</p>
 */
public final class RotationUtils {

    private RotationUtils() {
    }

    /**
     * @param eyes   the eye position
     * @param target the point to look at
     * @return a two element array with yaw at index {@code 0} and pitch at index {@code 1}
     */
    public static float[] calculate(Vec3 eyes, Vec3 target) {
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, horizontal) * (180.0D / Math.PI));
        return new float[]{wrap(yaw), Mth.clamp(pitch, -90.0F, 90.0F)};
    }

    /**
     * @param from the entity that looks
     * @param to   the entity to look at, aimed at its chest height
     * @return a two element array with yaw and pitch
     */
    public static float[] calculate(Entity from, Entity to) {
        return calculate(from.getEyePosition(), to.getEyePosition().add(0.0D, -0.2D, 0.0D));
    }

    /**
     * @param eyes the eye position
     * @param to   the entity to look at
     * @return a two element array with yaw and pitch
     */
    public static float[] calculate(Vec3 eyes, Entity to) {
        return calculate(eyes, to.getEyePosition().add(0.0D, -0.2D, 0.0D));
    }

    /**
     * @param yaw the raw yaw
     * @return the yaw wrapped to {@code -180..180}
     */
    public static float wrap(float yaw) {
        return Mth.wrapDegrees(yaw);
    }

    /**
     * Applies the mouse-sensitivity gcd fix so the rotation looks like it came from real mouse input.
     *
     * @param player the local player
     * @param yaw    the target yaw
     * @param pitch  the target pitch
     * @return a two element array with the fixed yaw and pitch
     */
    public static float[] applyGcdFix(LocalPlayer player, float yaw, float pitch) {
        float fixedYaw = MathUtils.gcdFix(player.getYRot(), yaw);
        float fixedPitch = MathUtils.gcdFix(player.getXRot(), pitch);
        return new float[]{fixedYaw, Mth.clamp(fixedPitch, -90.0F, 90.0F)};
    }

    /**
     * Rotates the player towards an entity over time, which is visible on screen. This is the only aim the
     * client has: there is no silent rotation anywhere, so an aim change is always one the player can see.
     *
     * @param target the entity to face
     * @param speed  degrees per tick, use a large value for an instant snap
     */
    public static void face(Entity target, float speed) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || target == null) {
            return;
        }
        float[] rotation = calculate(player.getEyePosition(), target);
        float yaw = player.getYRot() + Mth.clamp(MathUtils.angleDifference(player.getYRot(), rotation[0]), -speed, speed);
        float pitch = player.getXRot() + Mth.clamp(MathUtils.angleDifference(player.getXRot(), rotation[1]), -speed, speed);
        player.setYRot(yaw);
        player.setXRot(pitch);
    }

    /**
     * @param target the entity to face
     * @return the yaw that faces the entity
     */
    public static float yawTo(Entity target) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || target == null) {
            return 0.0F;
        }
        return calculate(player.getEyePosition(), target)[0];
    }

    /**
     * @param target the entity to face
     * @return the pitch that faces the entity
     */
    public static float pitchTo(Entity target) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || target == null) {
            return 0.0F;
        }
        return calculate(player.getEyePosition(), target)[1];
    }

    /**
     * @param player the player, may be {@code null}
     * @param target the entity
     * @param fov    the maximum field of view in degrees
     * @return {@code true} when the entity is inside the player's view cone
     */
    public static boolean isInFov(LocalPlayer player, Entity target, double fov) {
        if (player == null || target == null) {
            return false;
        }
        float[] rotation = calculate(player.getEyePosition(), target);
        float yawDifference = Math.abs(MathUtils.angleDifference(player.getYRot(), rotation[0]));
        float pitchDifference = Math.abs(MathUtils.angleDifference(player.getXRot(), rotation[1]));
        return Math.hypot(yawDifference / 2.0D, pitchDifference / 2.0D) <= fov / 2.0D;
    }
}
