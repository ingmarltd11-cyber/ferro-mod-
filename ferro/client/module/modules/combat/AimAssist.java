package dev.ferro.client.module.modules.combat;

import dev.ferro.client.event.events.TickEvent;
import dev.ferro.client.event.events.TickListener;
import dev.ferro.client.module.Category;
import dev.ferro.client.module.Module;
import dev.ferro.client.module.setting.BooleanSetting;
import dev.ferro.client.module.setting.ModeSetting;
import dev.ferro.client.module.setting.NumberSetting;
import dev.ferro.client.module.setting.StringSetting;
import dev.ferro.client.utils.MathUtils;
import dev.ferro.client.utils.RotationUtils;
import dev.ferro.client.utils.WorldUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * AimAssist — gently pulls your view towards the target you are fighting.
 *
 * <p>This is an assist, not an aimbot: it only ever turns the camera the player is already using, and it
 * builds no packets at all. The rotation it writes is the rotation the vanilla client reports in its own
 * movement packets, so on the wire there is nothing that a human moving a mouse would not produce.</p>
 *
 * <p>The aim maths follow the reference clients: the angle to the aim point is wrapped to the shortest way
 * around, scaled by {@code speed / 10} and eased according to {@code Mode} (linear, smoothstep or ease out).
 * Three human touches sit on top of that, all of them borrowed from Argon: {@code Chance} skips a percentage
 * of ticks, {@code Wait After Mouse Move} stops the assist for a moment when you aim yourself, and
 * {@code Stop On Target} releases the target once your crosshair is really on it.</p>
 */
public class AimAssist extends Module implements TickListener {

    /** Distance in degrees below which the crosshair counts as "on target". */
    private static final float ON_TARGET = 0.6F;

    private final ModeSetting mode = new ModeSetting("Mode", "Simple", "Simple", "Smoothstep", "EaseOut");
    private final ModeSetting targets = new ModeSetting("Target", "Players", "Players", "Mobs", "Both");
    private final BooleanSetting requireMouseDown = new BooleanSetting("Require Mouse Down", true,
            "Only aim while you hold the attack button.");
    private final BooleanSetting strafeIncrease = new BooleanSetting("Strafe Increase", false,
            "Aims faster while you are strafing left or right.");
    private final BooleanSetting checkBlockBreak = new BooleanSetting("Check Block Break", true,
            "Pauses the assist while you are breaking one of the listed blocks.");
    private final StringSetting breakBlocks = new StringSetting("Break Blocks", "obsidian, ancient_debris, "
            + "respawn_anchor, crying_obsidian", 96);
    private final BooleanSetting aimVertically = new BooleanSetting("Aim Vertically", true,
            "Whether the vertical axis is assisted at all.");
    private final NumberSetting verticalSpeed = new NumberSetting("Vertical Speed", 0.1D, 10.0D, 0.1D, 10.0D,
            "How much of the vertical distance is taken per tick. 10 snaps, 2 is a soft pull.");
    private final NumberSetting horizontalSpeed = new NumberSetting("Horizontal Speed", 0.1D, 10.0D, 0.1D, 10.0D,
            "How much of the horizontal distance is taken per tick. 10 snaps, 2 is a soft pull.");
    private final NumberSetting maxAngle = new NumberSetting("Max Angle", 1.0D, 360.0D, 1.0D, 360.0D,
            "Only targets inside this angle around your crosshair are assisted.");
    private final NumberSetting distance = new NumberSetting("Distance", 1.0D, 12.0D, 0.5D, 8.0D,
            "Maximum target distance in blocks.");
    private final BooleanSetting limitToItems = new BooleanSetting("Limit To Items", false,
            "Only aim while holding one of the listed items.");
    private final StringSetting items = new StringSetting("Items", "sword, axe, mace", 64);
    private final ModeSetting targetArea = new ModeSetting("Target Area", "Center", "Center", "Head", "Body",
            "Feet");
    private final ModeSetting targetMode = new ModeSetting("Target Mode", "Yaw", "Yaw", "Pitch", "Both");
    private final BooleanSetting stopOnTarget = new BooleanSetting("Stop On Target", true,
            "Lets go once your crosshair is on the target, which is also the quietest behaviour.");
    private final NumberSetting chance = new NumberSetting("Chance", 0.0D, 100.0D, 5.0D, 100.0D, "%");
    private final NumberSetting waitAfterMove = new NumberSetting("Wait After Mouse Move", 0.0D, 500.0D, 10.0D,
            0.0D, "Milliseconds the assist stays off after you move the mouse yourself.");

    private float appliedYaw;
    private float appliedPitch;
    private long movedAt;

    /**
     * Creates the module.
     */
    public AimAssist() {
        super("AimAssist", Category.COMBAT, "Pulls your aim towards the player you are fighting.");
        breakBlocks.visibleWhen(checkBlockBreak::get);
        items.visibleWhen(limitToItems::get);
        breakBlocks.described("Blocks that pause the assist while you break them. Leave it empty to pause on "
                + "every block.");
        items.described("Items that allow the assist, matched by name. Leave it empty to allow everything.");
        mode.described("Simple lerps straight to the target, Smoothstep eases both ends, EaseOut starts fast "
                + "and softens at the end.");
        addSettings(mode, targets, requireMouseDown, strafeIncrease, checkBlockBreak, breakBlocks,
                aimVertically, verticalSpeed, horizontalSpeed, maxAngle, distance, limitToItems, items,
                targetArea, targetMode, stopOnTarget, chance, waitAfterMove);
    }

    @Override
    protected void onEnable() {
        listen(TickEvent.class);
        movedAt = 0L;
        LocalPlayer player = mc().player;
        if (player != null) {
            appliedYaw = player.getYRot();
            appliedPitch = player.getXRot();
        }
    }

    @Override
    protected void onDisable() {
        unlisten();
    }

    @Override
    public void onTick(TickEvent event) {
        LocalPlayer player = mc().player;
        if (player == null || mc().level == null || mc().screen != null) {
            return;
        }
        if (player.isDeadOrDying() || player.isSpectator()) {
            return;
        }

        // Anything that changed the rotation behind our back is the user moving the mouse: remember when,
        // then take the current rotation as the new base.
        if (Math.abs(MathUtils.angleDifference(appliedYaw, player.getYRot())) > 0.05F
                || Math.abs(player.getXRot() - appliedPitch) > 0.05F) {
            movedAt = System.currentTimeMillis();
        }
        appliedYaw = player.getYRot();
        appliedPitch = player.getXRot();

        if (waitAfterMove.getDouble() > 0.0D
                && System.currentTimeMillis() - movedAt < (long) waitAfterMove.getDouble()) {
            return;
        }
        if (requireMouseDown.get() && !mc().options.keyAttack.isDown()) {
            return;
        }
        if (limitToItems.get() && !holdsListedItem(player)) {
            return;
        }
        if (checkBlockBreak.get() && isBreakingListedBlock()) {
            return;
        }

        Entity target = findTarget(player);
        if (target == null) {
            return;
        }
        Vec3 aimPoint = aimPoint(target);
        float[] wanted = RotationUtils.calculate(player.getEyePosition(), aimPoint);
        if (stopOnTarget.get() && mc().hitResult instanceof EntityHitResult hit && hit.getEntity() == target
                && angleTo(player, wanted) < ON_TARGET) {
            return;
        }
        if (chance.getDouble() < 100.0D && MathUtils.random(0.0D, 100.0D) > chance.getDouble()) {
            return;
        }

        boolean horizontal = targetMode.is("Yaw") || targetMode.is("Both");
        boolean vertical = (targetMode.is("Pitch") || targetMode.is("Both")) && aimVertically.get();
        boolean strafing = strafeIncrease.get()
                && (mc().options.keyLeft.isDown() || mc().options.keyRight.isDown());

        float yaw = player.getYRot();
        float pitch = player.getXRot();
        if (horizontal) {
            yaw = approach(yaw, wanted[0], horizontalSpeed.getFloat(), strafing);
        }
        if (vertical) {
            pitch = approach(pitch, wanted[1], verticalSpeed.getFloat(), strafing);
        }
        player.setYRot(yaw);
        player.setXRot((float) MathUtils.clamp(pitch, -90.0D, 90.0D));
        appliedYaw = player.getYRot();
        appliedPitch = player.getXRot();
    }

    /**
     * Moves one angle towards another, eased the way the selected mode asks for.
     *
     * @param from   the current angle
     * @param to     the wanted angle
     * @param speed  the speed setting, {@code 0.1..10}
     * @param strafe whether the strafing bonus applies
     * @return the new angle
     */
    private float approach(float from, float to, float speed, boolean strafe) {
        double alpha = MathUtils.clamp(speed / 10.0D, 0.0D, 1.0D);
        if (strafe) {
            alpha = Math.min(1.0D, alpha * 1.5D);
        }
        alpha = switch (mode.get()) {
            case "Smoothstep" -> alpha * alpha * (3.0D - 2.0D * alpha);
            case "EaseOut" -> 1.0D - (1.0D - alpha) * (1.0D - alpha);
            default -> alpha;
        };
        return from + MathUtils.angleDifference(from, to) * (float) alpha;
    }

    /**
     * @param player the local player
     * @return the target closest to the crosshair inside the distance and angle limits, or {@code null}
     */
    private Entity findTarget(LocalPlayer player) {
        Entity best = null;
        double bestAngle = Double.MAX_VALUE;
        for (Entity entity : WorldUtils.getEntities(distance.getDouble() + 1.0D)) {
            if (!allowed(entity) || !WorldUtils.isCombatTarget(entity, distance.getDouble())) {
                continue;
            }
            float[] wanted = RotationUtils.calculate(player.getEyePosition(), aimPoint(entity));
            double angle = angleTo(player, wanted);
            if (angle > maxAngle.getDouble()) {
                continue;
            }
            if (angle < bestAngle) {
                bestAngle = angle;
                best = entity;
            }
        }
        return best;
    }

    /**
     * @param entity the entity
     * @return {@code true} when the Target setting allows it
     */
    private boolean allowed(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        return switch (targets.get()) {
            case "Players" -> entity instanceof Player;
            case "Mobs" -> !(entity instanceof Player);
            default -> true;
        };
    }

    /**
     * @param player the local player
     * @param wanted the wanted rotation
     * @return the angle in degrees between the current view and the wanted rotation
     */
    private float angleTo(LocalPlayer player, float[] wanted) {
        Vec3 current = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
        Vec3 wantedVector = Vec3.directionFromRotation(wanted[1], wanted[0]);
        double dot = MathUtils.clamp(current.dot(wantedVector), -1.0D, 1.0D);
        return (float) Math.toDegrees(Math.acos(dot));
    }

    /**
     * @param target the target
     * @return the point on the target the Target Area asks for
     */
    private Vec3 aimPoint(Entity target) {
        AABB box = target.getBoundingBox();
        return switch (targetArea.get()) {
            case "Head" -> new Vec3(target.getX(), box.maxY - 0.15D, target.getZ());
            case "Body" -> new Vec3(target.getX(), box.minY + (box.maxY - box.minY) * 0.75D, target.getZ());
            case "Feet" -> new Vec3(target.getX(), box.minY + 0.2D, target.getZ());
            default -> box.getCenter();
        };
    }

    /**
     * @param player the local player
     * @return {@code true} when the held item matches the Items list
     */
    private boolean holdsListedItem(LocalPlayer player) {
        String filter = items.get().trim().toLowerCase(Locale.ROOT);
        if (filter.isEmpty()) {
            return true;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return false;
        }
        String name = stack.getItem().getDescriptionId().toLowerCase(Locale.ROOT);
        for (String part : filter.split(",")) {
            String needle = part.trim();
            if (!needle.isEmpty() && name.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@code true} when the player is breaking a block that appears in the Break Blocks list
     */
    private boolean isBreakingListedBlock() {
        if (mc().gameMode == null || !mc().gameMode.isDestroying()) {
            return false;
        }
        String filter = breakBlocks.get().trim().toLowerCase(Locale.ROOT);
        if (filter.isEmpty()) {
            return true;
        }
        if (mc().level == null || !(mc().hitResult instanceof BlockHitResult blockHit)) {
            return false;
        }
        String name = mc().level.getBlockState(blockHit.getBlockPos()).getBlock().getDescriptionId()
                .toLowerCase(Locale.ROOT);
        for (String part : filter.split(",")) {
            String needle = part.trim();
            if (!needle.isEmpty() && name.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
