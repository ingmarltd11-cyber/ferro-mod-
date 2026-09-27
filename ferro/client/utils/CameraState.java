package dev.ferro.client.utils;

import net.minecraft.world.phys.Vec3;

/**
 * Shared camera override used by the freecam and spectator modules.
 *
 * <p>The module writes a position and rotation here every tick; {@code CameraMixin} applies it after
 * vanilla ran, so the player keeps standing still while the camera flies around.</p>
 *
 * <p>The previous tick is kept as well, and the mixin interpolates between the two with the frame delta.
 * Without that the camera would step twenty times a second and look like a slideshow.</p>
 */
public final class CameraState {

    private static boolean active;
    private static Vec3 position = Vec3.ZERO;
    private static Vec3 previousPosition = Vec3.ZERO;
    private static float yaw;
    private static float pitch;
    private static float previousYaw;
    private static float previousPitch;

    private CameraState() {
    }

    /**
     * @return {@code true} when a module wants to control the camera
     */
    public static boolean isActive() {
        return active;
    }

    /**
     * @param value whether a module wants to control the camera
     */
    public static void setActive(boolean value) {
        active = value;
    }

    /**
     * @return the override position
     */
    public static Vec3 getPosition() {
        return position;
    }

    /**
     * Moves the override to a new position, remembering the old one for interpolation.
     *
     * @param value the override position
     */
    public static void setPosition(Vec3 value) {
        previousPosition = position;
        position = value;
    }

    /**
     * @param partialTick the frame delta, {@code 0..1}
     * @return the position to render this frame
     */
    public static Vec3 getInterpolatedPosition(float partialTick) {
        return previousPosition.lerp(position, partialTick);
    }

    /**
     * @return the override yaw
     */
    public static float getYaw() {
        return yaw;
    }

    /**
     * Turns the override, remembering the old angles for interpolation.
     *
     * @param newYaw   the override yaw
     * @param newPitch the override pitch
     */
    public static void setRotation(float newYaw, float newPitch) {
        previousYaw = yaw;
        previousPitch = pitch;
        yaw = newYaw;
        pitch = newPitch;
    }

    /**
     * @param partialTick the frame delta, {@code 0..1}
     * @return the yaw to render this frame
     */
    public static float getInterpolatedYaw(float partialTick) {
        return previousYaw + (yaw - previousYaw) * partialTick;
    }

    /**
     * @return the override pitch
     */
    public static float getPitch() {
        return pitch;
    }

    /**
     * @param partialTick the frame delta, {@code 0..1}
     * @return the pitch to render this frame
     */
    public static float getInterpolatedPitch(float partialTick) {
        return previousPitch + (pitch - previousPitch) * partialTick;
    }

    /**
     * Clears the override.
     */
    public static void reset() {
        active = false;
        position = Vec3.ZERO;
        previousPosition = Vec3.ZERO;
        yaw = 0.0F;
        pitch = 0.0F;
        previousYaw = 0.0F;
        previousPitch = 0.0F;
    }
}
