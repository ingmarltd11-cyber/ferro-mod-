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
import dev.ferro.client.utils.RotationUtils;
import dev.ferro.client.utils.WorldUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * KillAura — attacks the entities around you, with the target order, timing and rotation the settings ask
 * for.
 *
 * <p>Built the way the reference clients build it: the target list, the priority, the multi or switch
 * behaviour and the attack gate are separate decisions, so the module can be a 3 block duel aura or a
 * 6 block multi aura without a second code path.</p>
 *
 * <p>An attack leaves the client through {@code MultiPlayerGameMode#attack}, which is exactly what a left
 * click does: the server sees nothing the game would not have sent itself. The rotation turns your real
 * view, there is no silent mode and no packet mode.</p>
 */
public class KillAura extends Module implements TickListener {

    private final NumberSetting range = new NumberSetting("Range", 1.0D, 6.0D, 0.1D, 3.2D,
            "Attack range in blocks.");
    private final ModeSetting rotation = new ModeSetting("Rotation", "Camera", "Camera", "Off");
    private final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", 10.0D, 360.0D, 10.0D, 180.0D,
            "Degrees per tick the camera turns. 360 snaps instantly.");
    private final NumberSetting fov = new NumberSetting("Fov", 10.0D, 360.0D, 10.0D, 360.0D,
            "Only attack targets inside this view angle (degrees). 360 = everyone around you.");
    private final ModeSetting targets = new ModeSetting("Targets", "Players", "Players", "Mobs", "Both");
    private final ModeSetting targetMode = new ModeSetting("Target Mode", "Single", "Single", "Multi", "Switch");
    private final NumberSetting maxTargets = new NumberSetting("Max Targets", 1.0D, 5.0D, 1.0D, 2.0D,
            "How many entities a Multi aura hits per swing.");
    private final NumberSetting switchDelay = new NumberSetting("Switch Delay", 0.0D, 1000.0D, 50.0D, 250.0D,
            "Milliseconds before a Switch aura moves on to the next target.");
    private final ModeSetting priority = new ModeSetting("Priority", "Distance", "Distance", "Angle", "Health");
    private final BooleanSetting ignoreCooldown = new BooleanSetting("Ignore Cooldown", false,
            "Skip the vanilla attack cooldown check.");
    private final BooleanSetting onlyWeapons = new BooleanSetting("Only Weapons", false,
            "Only attack while holding a sword or an axe.");

    private long lastSwitch;
    private Entity currentTarget;

    /**
     * Creates the module.
     */
    public KillAura() {
        super("KillAura", Category.COMBAT, "Attacks the entities around you.");
        rotationSpeed.visibleWhen(() -> rotation.is("Camera"));
        maxTargets.visibleWhen(() -> targetMode.is("Multi"));
        switchDelay.visibleWhen(() -> targetMode.is("Switch"));
        rotation.described("Camera turns your real view towards the target. Off does not aim at all.");
        priority.described("How the target list is ordered: by distance, by the smallest angle to your "
                + "crosshair, or by the lowest health.");
        addSettings(range, rotation, rotationSpeed, fov, targets, targetMode, maxTargets, switchDelay,
                priority, ignoreCooldown, onlyWeapons);
    }

    @Override
    protected void onEnable() {
        listen(TickEvent.class);
        currentTarget = null;
    }

    @Override
    protected void onDisable() {
        unlisten();
        currentTarget = null;
    }

    @Override
    public void onTick(TickEvent event) {
        LocalPlayer player = mc().player;
        if (player == null || mc().level == null || mc().gameMode == null) {
            return;
        }
        if (player.isDeadOrDying() || player.isSpectator()) {
            return;
        }
        if (onlyWeapons.get() && !InventoryUtils.isWeapon(player.getMainHandItem())) {
            return;
        }

        List<Entity> candidates = collectTargets(player);
        if (candidates.isEmpty()) {
            currentTarget = null;
            return;
        }
        List<Entity> targets = select(candidates);
        if (targets.isEmpty()) {
            return;
        }
        aim(player, targets.get(0));

        if (!ignoreCooldown.get() && player.getAttackStrengthScale(0.5F) < 1.0F) {
            return;
        }
        for (Entity target : targets) {
            attack(player, target);
        }
    }

    /**
     * Aims at the primary target, according to the Rotation setting.
     *
     * @param player the local player
     * @param target the primary target
     */
    private void aim(LocalPlayer player, Entity target) {
        if (rotation.is("Camera")) {
            RotationUtils.face(target, rotationSpeed.getFloat());
        }
    }

    /**
     * @param player the local player
     * @return every attackable entity in range whose filter and view cone allow it
     */
    private List<Entity> collectTargets(LocalPlayer player) {
        List<Entity> result = new ArrayList<>();
        for (Entity entity : WorldUtils.getEntities(range.getDouble() + 1.0D)) {
            if (!allowed(entity) || !WorldUtils.isCombatTarget(entity, range.getDouble())) {
                continue;
            }
            if (fov.getDouble() < 360.0D && !RotationUtils.isInFov(player, entity, fov.getDouble())) {
                continue;
            }
            result.add(entity);
        }
        return result;
    }

    /**
     * @param entity the entity
     * @return {@code true} when the Targets setting allows it
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
     * Orders the candidates and picks the ones this swing hits.
     *
     * @param candidates every allowed entity, closest first
     * @return the entities to attack this tick
     */
    private List<Entity> select(List<Entity> candidates) {
        LocalPlayer player = mc().player;
        if (player == null) {
            return List.of();
        }
        Comparator<Entity> order = switch (priority.get()) {
            case "Angle" -> Comparator.comparingDouble(entity -> Math.abs(
                    MathUtils.angleDifference(player.getYRot(), RotationUtils.yawTo(entity))));
            case "Health" -> Comparator.comparingDouble(entity -> entity instanceof LivingEntity living
                    ? living.getHealth() : Double.MAX_VALUE);
            default -> Comparator.comparingDouble(entity -> entity.distanceTo(player));
        };
        List<Entity> sorted = new ArrayList<>(candidates);
        sorted.sort(order);
        return switch (targetMode.get()) {
            case "Multi" -> sorted.subList(0, (int) Math.min(sorted.size(), maxTargets.getDouble()));
            case "Switch" -> List.of(pickSwitchTarget(sorted));
            default -> List.of(sorted.get(0));
        };
    }

    /**
     * Keeps the current target until it dies, leaves the list or the switch delay runs out.
     *
     * @param sorted the ordered candidates
     * @return the entity to attack
     */
    private Entity pickSwitchTarget(List<Entity> sorted) {
        long now = System.currentTimeMillis();
        boolean invalid = currentTarget == null || !currentTarget.isAlive() || !sorted.contains(currentTarget);
        if (invalid || now - lastSwitch >= (long) switchDelay.getDouble()) {
            int index = sorted.indexOf(currentTarget);
            currentTarget = sorted.get((index + 1) % sorted.size());
            lastSwitch = now;
        }
        return currentTarget;
    }

    /**
     * Performs the attack through the game's own click path.
     *
     * @param player the local player
     * @param target the entity to hit
     */
    private void attack(LocalPlayer player, Entity target) {
        mc().gameMode.attack(player, target);
    }
}
