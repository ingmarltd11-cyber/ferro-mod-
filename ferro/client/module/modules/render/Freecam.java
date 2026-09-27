package dev.ferro.client.module.modules.render;

import dev.ferro.client.event.events.TickEvent;
import dev.ferro.client.event.events.TickListener;
import dev.ferro.client.module.Category;
import dev.ferro.client.module.Module;
import dev.ferro.client.module.setting.BooleanSetting;
import dev.ferro.client.module.setting.NumberSetting;
import dev.ferro.client.utils.CameraState;
import dev.ferro.client.utils.MathUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Freecam — leaves your body behind and flies the camera around.
 *
 * <p>The module keeps the player standing still and moves the camera instead. Mouse input still goes to the
 * player, so the module reads the rotation delta every tick, adds it to the camera and puts the player's own
 * rotation back. Position and rotation are handed to {@code CameraMixin} through {@code CameraState}, which
 * interpolates between ticks so the flight is smooth instead of twenty steps a second.</p>
 *
 * <p>Nothing here touches the network: the player does not move, so the client sends less than it would
 * while walking, and no packet is built by this module. {@code Static View} also switches view bobbing and
 * the fov effect off while you fly, because both are applied to the world camera and make a detached camera
 * look broken.</p>
 */
public class Freecam extends Module implements TickListener {

    private final NumberSetting speed = new NumberSetting("Speed", 0.1D, 5.0D, 0.1D, 1.0D,
            "Blocks per tick the camera moves.");
    private final BooleanSetting freezePlayer = new BooleanSetting("Freeze Player", true,
            "Keeps your body where it is while the camera flies.");
    private final BooleanSetting staticView = new BooleanSetting("Static View", true,
            "Turns view bobbing and the fov effect off while the camera is detached.");
    private final BooleanSetting toggleOnDamage = new BooleanSetting("Toggle On Damage", false,
            "Leaves freecam as soon as you take damage.");
    private final BooleanSetting toggleOnDeath = new BooleanSetting("Toggle On Death", true,
            "Leaves freecam when you die.");

    private Vec3 cameraPos;
    private float cameraYaw;
    private float cameraPitch;
    private float frozenYaw;
    private float frozenPitch;
    private Boolean savedBobView;
    private Double savedFovScale;

    /**
     * Creates the module.
     */
    public Freecam() {
        super("Freecam", Category.RENDER, "Leaves your body behind and flies the camera.");
        addSettings(speed, freezePlayer, staticView, toggleOnDamage, toggleOnDeath);
    }

    @Override
    protected void onEnable() {
        listen(TickEvent.class);
        LocalPlayer player = mc().player;
        if (player == null) {
            return;
        }
        cameraPos = player.getEyePosition();
        cameraYaw = player.getYRot();
        cameraPitch = player.getXRot();
        frozenYaw = player.getYRot();
        frozenPitch = player.getXRot();
        if (staticView.get()) {
            savedBobView = mc().options.bobView().get();
            savedFovScale = mc().options.fovEffectScale().get();
            mc().options.bobView().set(false);
            mc().options.fovEffectScale().set(0.0D);
        }
        CameraState.setActive(true);
        CameraState.setPosition(cameraPos);
        CameraState.setRotation(cameraYaw, cameraPitch);
    }

    @Override
    protected void onDisable() {
        unlisten();
        if (savedBobView != null) {
            mc().options.bobView().set(savedBobView);
            savedBobView = null;
        }
        if (savedFovScale != null) {
            mc().options.fovEffectScale().set(savedFovScale);
            savedFovScale = null;
        }
        cameraPos = null;
        CameraState.reset();
    }

    @Override
    public void onTick(TickEvent event) {
        LocalPlayer player = mc().player;
        if (player == null || cameraPos == null) {
            return;
        }
        if (toggleOnDeath.get() && player.isDeadOrDying()) {
            setEnabled(false);
            return;
        }
        if (toggleOnDamage.get() && player.hurtTime > 0) {
            setEnabled(false);
            return;
        }

        // The mouse is still driving the player rotation: take the delta for the camera and put the body
        // back where it was, so the player never turns while the camera looks around.
        cameraYaw += MathUtils.angleDifference(frozenYaw, player.getYRot());
        cameraPitch = (float) MathUtils.clamp(
                cameraPitch + MathUtils.angleDifference(frozenPitch, player.getXRot()), -90.0D, 90.0D);
        if (freezePlayer.get()) {
            player.setYRot(frozenYaw);
            player.setXRot(frozenPitch);
            player.setDeltaMovement(0.0D, player.getDeltaMovement().y, 0.0D);
        }

        double radians = Math.toRadians(cameraYaw);
        Vec3 forward = new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians));
        Vec3 left = new Vec3(Math.cos(radians), 0.0D, Math.sin(radians));
        double step = speed.getDouble();
        Vec3 movement = Vec3.ZERO;
        if (mc().options.keyUp.isDown()) {
            movement = movement.add(forward.scale(step));
        }
        if (mc().options.keyDown.isDown()) {
            movement = movement.add(forward.scale(-step));
        }
        if (mc().options.keyLeft.isDown()) {
            movement = movement.add(left.scale(step));
        }
        if (mc().options.keyRight.isDown()) {
            movement = movement.add(left.scale(-step));
        }
        if (mc().options.keyJump.isDown()) {
            movement = movement.add(0.0D, step, 0.0D);
        }
        if (mc().options.keyShift.isDown()) {
            movement = movement.add(0.0D, -step, 0.0D);
        }

        cameraPos = cameraPos.add(movement);
        CameraState.setPosition(cameraPos);
        CameraState.setRotation(cameraYaw, cameraPitch);
    }
}
