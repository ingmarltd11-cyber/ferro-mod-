package dev.ferro.client.module.modules.combat;

import dev.ferro.client.event.events.TickEvent;
import dev.ferro.client.event.events.TickListener;
import dev.ferro.client.module.Category;
import dev.ferro.client.module.Module;
import dev.ferro.client.module.setting.BooleanSetting;
import dev.ferro.client.module.setting.ModeSetting;
import dev.ferro.client.module.setting.NumberSetting;
import dev.ferro.client.utils.InventoryUtils;
import dev.ferro.client.utils.MathUtils;
import dev.ferro.client.utils.WorldUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * TriggerBot — hits the entity your crosshair is already on, after a human-ish pause.
 *
 * <p>Nothing is rotated and nothing is spoofed: this module only presses the attack for you, and it does
 * that through {@code MultiPlayerGameMode#attack}, the exact path a left click takes. The delay starts when
 * the crosshair lands on a target, so the rhythm is per target instead of per tick, and {@code Delay
 * Variance} adds a random offset so the hits do not line up with a fixed interval.</p>
 *
 * <p>The gating options mirror what the big clients check before clicking: sprinting, the item in your hand,
 * whether the hit would crit, whether the weapon is charged, and how often to hit at all.</p>
 */
public class TriggerBot extends Module implements TickListener {

    private final NumberSetting delay = new NumberSetting("Delay", 0.0D, 500.0D, 10.0D, 100.0D,
            "Milliseconds between the crosshair landing on a target and the hit.");
    private final NumberSetting variance = new NumberSetting("Delay Variance", 0.0D, 250.0D, 10.0D, 50.0D,
            "Random extra wait, so the hits do not land on a fixed rhythm.");
    private final NumberSetting range = new NumberSetting("Range", 1.0D, 6.0D, 0.1D, 3.0D,
            "Maximum distance to the target.");
    private final ModeSetting targets = new ModeSetting("Targets", "Players", "Players", "Mobs", "Both");
    private final BooleanSetting onlySprinting = new BooleanSetting("Only While Sprinting", false,
            "Only hit while you are sprinting.");
    private final BooleanSetting onlySword = new BooleanSetting("Only On Sword", false,
            "Only hit while holding a sword.");
    private final BooleanSetting critOnly = new BooleanSetting("Crit Only", false,
            "Only hit when the hit would crit.");
    private final BooleanSetting stopSprintForCrit = new BooleanSetting("Stop Sprinting For Crits", true,
            "Lets go of sprint so the next hit crits again.");
    private final NumberSetting hitChance = new NumberSetting("Hit Chance", 1.0D, 100.0D, 1.0D, 100.0D, "%");
    private final BooleanSetting requireCharge = new BooleanSetting("Require Full Charge", true,
            "Wait for a fully charged weapon, exactly like a patient player.");

    private Entity lastTarget;
    private long landedAt;

    /**
     * Creates the module.
     */
    public TriggerBot() {
        super("TriggerBot", Category.COMBAT, "Attacks the entity under your crosshair.");
        stopSprintForCrit.visibleWhen(critOnly::get);
        targets.described("Which entities the trigger reacts to.");
        addSettings(delay, variance, range, targets, onlySprinting, onlySword, critOnly, stopSprintForCrit,
                hitChance, requireCharge);
    }

    @Override
    protected void onEnable() {
        listen(TickEvent.class);
        lastTarget = null;
    }

    @Override
    protected void onDisable() {
        unlisten();
        lastTarget = null;
    }

    @Override
    public void onTick(TickEvent event) {
        LocalPlayer player = mc().player;
        if (player == null || mc().gameMode == null || mc().screen != null) {
            return;
        }
        if (player.isDeadOrDying() || player.isSpectator()) {
            return;
        }

        Entity target = crosshairTarget();
        if (target == null || !allowed(target) || !WorldUtils.isCombatTarget(target, range.getDouble())) {
            lastTarget = null;
            return;
        }
        if (onlySprinting.get() && !player.isSprinting()) {
            return;
        }
        if (onlySword.get() && !InventoryUtils.isSword(player.getMainHandItem())) {
            return;
        }
        if (critOnly.get() && !handleCriticals(player)) {
            return;
        }
        if (requireCharge.get() && player.getAttackStrengthScale(0.5F) < 1.0F) {
            return;
        }

        long now = System.currentTimeMillis();
        if (lastTarget != target) {
            lastTarget = target;
            landedAt = now;
            return;
        }
        long wait = (long) (delay.getDouble() + MathUtils.random(0.0D, variance.getDouble()));
        if (now - landedAt < wait) {
            return;
        }
        if (MathUtils.random(0.0D, 100.0D) > hitChance.getDouble()) {
            landedAt = now;
            return;
        }
        landedAt = now;
        // The vanilla click path: the packet on the wire is the one a left click produces.
        mc().gameMode.attack(player, target);
    }

    /**
     * Handles the crit gate the way LiquidBounce's smart criticals do: while you are sprinting a hit can
     * never crit, so the module lets go of sprint and tries again on the next tick.
     *
     * @param player the local player
     * @return {@code true} when this tick may hit
     */
    private boolean handleCriticals(LocalPlayer player) {
        if (player.isSprinting()) {
            if (stopSprintForCrit.get()) {
                player.setSprinting(false);
            }
            return false;
        }
        return player.fallDistance > 0.0F && !player.onGround() && !player.onClimbable()
                && !player.isInWater() && !player.isPassenger() && !player.hasEffect(MobEffects.BLINDNESS);
    }

    /**
     * @param entity the entity under the crosshair
     * @return {@code true} when the Targets setting allows it
     */
    private boolean allowed(Entity entity) {
        return switch (targets.get()) {
            case "Players" -> entity instanceof Player;
            case "Mobs" -> !(entity instanceof Player);
            default -> true;
        };
    }

    /**
     * @return the entity the player is looking at, or {@code null}
     */
    private Entity crosshairTarget() {
        HitResult hit = mc().hitResult;
        return hit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
    }
}
