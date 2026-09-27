package dev.ferro.client.mixin;

import dev.ferro.client.utils.CameraState;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets modules detach the camera from the player, which is how freecam and spectator work.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    /** Vanilla position setter. */
    @Shadow
    protected abstract void setPosition(Vec3 position);

    /** Vanilla rotation setter. */
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    /**
     * @param level      the level being rendered
     * @param entity     the camera entity
     * @param detached   whether the camera is detached
     * @param mirrored   whether the camera is mirrored
     * @param partialTick the frame delta
     * @param callback   mixin callback
     */
    @Inject(method = "setup", at = @At("TAIL"))
    private void ferro$setup(net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity entity,
                             boolean detached, boolean mirrored, float partialTick, CallbackInfo callback) {
        if (!CameraState.isActive()) {
            return;
        }
        // Interpolated with the frame delta, so a 20 Hz freecam still renders smoothly.
        setPosition(CameraState.getInterpolatedPosition(partialTick));
        setRotation(CameraState.getInterpolatedYaw(partialTick), CameraState.getInterpolatedPitch(partialTick));
    }
}
