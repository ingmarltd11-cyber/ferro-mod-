package dev.ferro.client.module.modules.combat;

import dev.ferro.client.event.events.TickEvent;
import dev.ferro.client.event.events.TickListener;
import dev.ferro.client.module.Category;
import dev.ferro.client.module.Module;
import dev.ferro.client.module.setting.BooleanSetting;
import dev.ferro.client.module.setting.NumberSetting;
import dev.ferro.client.utils.WorldUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Shield Drain — taps a blocking player's shield to eat its durability during mace fights.
 *
 * <p>The module stands on its own: it finds the shielder (the closest one, or exactly the player under your
 * crosshair), checks that they are really holding a shield up, and then sends the attack clicks itself. It
 * never swaps items, never aims and never builds a packet of its own — every click runs through
 * {@code MultiPlayerGameMode#attack}, which is the same packet a left click produces, so the drain is just
 * you clicking very fast. Aim at the target yourself; with {@code Only On Crosshair} the module drains
 * exactly the player you are looking at.</p>
 *
 * <p>{@code Clicks Per Tick} is the dial that decides how blatant this is: the tick runs twenty times a
 * second, so 2 clicks is 40 attacks per second and 10 is 200.</p>
 */
public class ShieldDrain extends Module implements TickListener {

    private final NumberSetting horizontalDistance = new NumberSetting("Horizontal Target Dist", 0.0D, 6.0D, 0.1D,
            5.0D, "Searches for shielders within this horizontal distance (blocks).");
    private final NumberSetting maxRange = new NumberSetting("Max Range", 0.0D, 6.0D, 0.1D, 4.5D,
            "Ignores targets beyond this distance (blocks).");
    private final NumberSetting minFallDistance = new NumberSetting("Min Fall Distance", 0.0D, 20.0D, 0.5D, 0.0D,
            "Drains only after falling this far (blocks). 0 = always.");
    private final BooleanSetting onlyOnCrosshair = new BooleanSetting("Only On Crosshair", false,
            "Drains only the player under your crosshair.");
    private final BooleanSetting requireMace = new BooleanSetting("Require Mace", true,
            "Runs only while holding a mace. Disables auto-swap.");
    private final BooleanSetting pauseOnGround = new BooleanSetting("Pause On Ground", false,
            "Stops draining when you touch the ground.");
    private final NumberSetting clicksPerTick = new NumberSetting("Clicks Per Tick", 1.0D, 10.0D, 1.0D, 2.0D,
            "Sends this many attacks per tick. 3: 60 CPS, 10: 200 CPS, blatant.");

    /**
     * Creates the module.
     */
    public ShieldDrain() {
        super("Shield Drain", Category.COMBAT,
                "Repeatedly hits a target's shield to drain its durability during mace combat.");
        addSettings(horizontalDistance, maxRange, minFallDistance, onlyOnCrosshair, requireMace, pauseOnGround,
                clicksPerTick);
    }

    @Override
    protected void onEnable() {
        listen(TickEvent.class);
    }

    @Override
    protected void onDisable() {
        unlisten();
    }

    @Override
    public void onTick(TickEvent event) {
        LocalPlayer player = mc().player;
        if (player == null || mc().gameMode == null) {
            return;
        }
        if (player.isDeadOrDying() || player.isSpectator()) {
            return;
        }
        if (pauseOnGround.get() && player.onGround()) {
            return;
        }
        if (player.fallDistance < minFallDistance.getDouble()) {
            return;
        }
        if (requireMace.get() && !player.getMainHandItem().is(Items.MACE)) {
            return;
        }

        Entity target = findTarget(player);
        if (target == null) {
            return;
        }
        int clicks = (int) Math.max(1.0D, clicksPerTick.getDouble());
        for (int click = 0; click < clicks; click++) {
            // The vanilla click path: the packet on the wire is the one a left click produces.
            mc().gameMode.attack(player, target);
        }
    }

    /**
     * @param player the local player
     * @return the shield to drain, or {@code null}
     */
    private Entity findTarget(LocalPlayer player) {
        if (onlyOnCrosshair.get()) {
            HitResult hit = mc().hitResult;
            if (hit instanceof EntityHitResult entityHit && allowed(player, entityHit.getEntity())) {
                return entityHit.getEntity();
            }
            return null;
        }
        // The entity list comes back sorted by distance, so the first shielder is the closest one.
        for (Entity entity : WorldUtils.getEntities(horizontalDistance.getDouble() + 1.0D)) {
            if (allowed(player, entity)) {
                return entity;
            }
        }
        return null;
    }

    /**
     * @param player the local player
     * @param entity the candidate
     * @return {@code true} when the entity is a shielder inside both distances
     */
    private boolean allowed(LocalPlayer player, Entity entity) {
        if (!isShielder(entity) || !WorldUtils.isCombatTarget(entity, maxRange.getDouble())) {
            return false;
        }
        double horizontal = Math.hypot(entity.getX() - player.getX(), entity.getZ() - player.getZ());
        return horizontal <= horizontalDistance.getDouble();
    }

    /**
     * @param entity the entity
     * @return {@code true} when the entity is holding a shield up right now
     */
    private boolean isShielder(Entity entity) {
        return entity instanceof LivingEntity living && living.isBlocking()
                && living.getUseItem().is(Items.SHIELD);
    }
}
