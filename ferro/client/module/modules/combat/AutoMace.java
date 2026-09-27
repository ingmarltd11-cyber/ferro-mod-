package dev.ferro.client.module.modules.combat;

import dev.ferro.client.event.events.TickEvent;
import dev.ferro.client.event.events.TickListener;
import dev.ferro.client.module.Category;
import dev.ferro.client.module.Module;
import dev.ferro.client.module.setting.BooleanSetting;
import dev.ferro.client.module.setting.ModeSetting;
import dev.ferro.client.module.setting.NumberSetting;
import dev.ferro.client.utils.InventoryUtils;
import dev.ferro.client.utils.Log;
import dev.ferro.client.utils.RotationUtils;
import dev.ferro.client.utils.WorldUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * AutoMace — takes the mace out the moment a player shows up and slams when the fall is long enough.
 *
 * <p>Nothing here is spoofed. The hotbar switch is an ordinary slot change the client syncs itself, the
 * attack goes through {@code MultiPlayerGameMode#attack} — the exact code path a left click runs — and the
 * aim turns your real view. There is no packet mode, because there is no second path to pick.</p>
 *
 * <p>What happens per tick, in this order:</p>
 * <ol>
 *   <li>pick the closest real player inside {@code Horizontal Target Dist},</li>
 *   <li>aim, according to {@code Aim}: the camera, or nothing,</li>
 *   <li>act only while falling, and take the mace out only inside {@code Swap Distance}, so it is in hand
 *       just before the hit,</li>
 *   <li>drop the elytra when diving on that target, when Takeoff Elytra is on,</li>
 *   <li>fall, then run the sequence from whatever is held: when Stun Slam is on and the target holds its
 *       shield up it swings the axe first to break the shield, then the mace;</li>
 *   <li>take the mace out — a Breach mace when the fall is short and Breach Check is on, a Density mace
 *       otherwise — and wait out the slot change,</li>
 *   <li>swing when the target is inside reach and Trigger is on, then (when Swap Back is on) return to the
 *       slot that was held before the mace came out.</li>
 * </ol>
 *
 * <p>Three details decide whether a hit actually registers:</p>
 * <ul>
 *   <li>A hotbar change needs {@value #SWAP_SETTLE} ms before the swing: the server resolves an attack
 *       against the item it believes you hold, so swapping and hitting in the same millisecond lands as a
 *       sword hit.</li>
 *   <li>The mace has a long attack cooldown. With {@code Ignore Cooldown} on (the default) the swing goes out
 *       every tick when ignored (the default), which is what makes the module feel alive; turn it off to
 *       only hit full charges.</li>
 *   <li>The swing uses the same distance as the search, so a target that makes the module take the mace out
 *       is also a target it swings at. {@code Expand Hitbox} widens that distance and inflates the client
 *       side box by half a block, which is the "easier hits, higher flag risk" trade-off.</li>
 * </ul>
 */
public class AutoMace extends Module implements TickListener {

    /** Milliseconds the mace is held before it swings: about two ticks, so the stun reads as a separate
     * step from the slam. */
    private static final long SWAP_SETTLE = 120L;
    /** Milliseconds the axe is held before it swings: a deliberate one tick pause on the stun step. */
    private static final long AXE_HOLD = 50L;
    /** Milliseconds between two debug lines. */
    private static final long DEBUG_INTERVAL = 1000L;
    /** Degrees per tick the camera aim turns, when Aim is set to Camera. */
    private static final float AIM_SPEED = 30.0F;
    /** Extra reach, in blocks, that Expand Hitbox allows on top of the vanilla reach. */
    private static final double EXPANDED_REACH = 0.5D;

    private static final Item[] MACES = {Items.MACE};
    private static final Item[] AXES = {Items.NETHERITE_AXE, Items.DIAMOND_AXE, Items.IRON_AXE, Items.COPPER_AXE,
            Items.STONE_AXE, Items.GOLDEN_AXE, Items.WOODEN_AXE};
    private static final Item[] SWORDS = {Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD,
            Items.COPPER_SWORD, Items.STONE_SWORD, Items.GOLDEN_SWORD, Items.WOODEN_SWORD};
    private static final Item[] SPEARS = {Items.NETHERITE_SPEAR, Items.DIAMOND_SPEAR, Items.IRON_SPEAR,
            Items.COPPER_SPEAR, Items.GOLDEN_SPEAR, Items.STONE_SPEAR, Items.WOODEN_SPEAR};

    /** The two mace enchantments the swap chooses between. */
    private static final ResourceKey<Enchantment> BREACH = Enchantments.BREACH;
    private static final ResourceKey<Enchantment> DENSITY = Enchantments.DENSITY;

    /** Where the slam sequence currently is. */
    private enum Stage {
        /** Looking for a target, no weapon has been swapped yet. */
        SEARCH,
        /** Stun slam: the axe was taken out, waiting before swinging it. */
        TAKE_AXE,
        /** Stun slam: the axe has swung, moving to the mace on its own step. */
        GO_MACE,
        /** The mace is out, waiting before swinging. */
        MACE_HIT
    }

    private final NumberSetting horizontalDistance = new NumberSetting("Horizontal Target Dist", 0.0D, 6.0D, 0.1D,
            5.0D, "Searches for targets within this horizontal distance (blocks).");
    private final NumberSetting minFallDistance = new NumberSetting("Min Fall Distance", 0.0D, 20.0D, 0.5D, 2.0D,
            "Swaps to the mace only after falling at least this many blocks. A jump is not a slam.");
    private final BooleanSetting onlyFalling = new BooleanSetting("Only While Falling", true,
            "Only takes the mace out and slams while you are actually falling, so the slam lands in the dive.");
    private final NumberSetting swapDistance = new NumberSetting("Swap Distance", 0.0D, 6.0D, 0.1D, 3.5D,
            "Only takes the mace out this close, so you hold it just before the hit and not from across the "
                    + "fight.");
    private final BooleanSetting stunSlam = new BooleanSetting("Stun Slam", false,
            "When the target holds its shield up, swaps to the axe to break it, then maces; otherwise goes "
                    + "straight from whatever you hold to the mace.");
    private final BooleanSetting breachCheck = new BooleanSetting("Breach Check", false,
            "Swaps to Breach instead of Density on low fall distance: below Min Fall Distance the mace swap is "
                    + "still made, but with the Breach mace.");
    private final BooleanSetting trigger = new BooleanSetting("Trigger", true,
            "Attacks with the mace automatically.");
    private final ModeSetting aim = new ModeSetting("Aim", "Camera", "Camera", "Off");
    private final BooleanSetting takeoffElytra = new BooleanSetting("Takeoff Elytra", false,
            "Unequips the elytra when diving on a maceable target.");
    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", true,
            "Returns to the previous slot after the mace hit lands.");
    private final BooleanSetting expandHitbox = new BooleanSetting("Expand Hitbox", false,
            "Grows the target hitbox for the hit. Easier hits, higher flag risk.");
    private final BooleanSetting ignoreCooldown = new BooleanSetting("Ignore Cooldown", true,
            "Skips the vanilla attack cooldown check. Turn off to only hit with a fully charged mace.");
    private final BooleanSetting debug = new BooleanSetting("Debug", false,
            "Logs why a swing is or is not happening, into the game log.");

    private long lastSwap;
    private long lastElytra;
    private long lastDebug;
    private long stageAt;
    private Stage stage = Stage.SEARCH;
    private int previousSlot = -1;

    /**
     * Creates the module.
     */
    public AutoMace() {
        super("AutoMace", Category.COMBAT, "Slams the player you see with a mace.");
        aim.described("Camera turns your real view towards the target. Off aims at nothing.");
        addSettings(horizontalDistance, minFallDistance, onlyFalling, swapDistance, stunSlam, breachCheck,
                trigger, aim, takeoffElytra, swapBack, expandHitbox, ignoreCooldown, debug);
    }

    @Override
    protected void onEnable() {
        listen(TickEvent.class);
        stage = Stage.SEARCH;
        Log.info("AutoMace enabled, mace in hotbar slot {}", maceSlot(false));
    }

    @Override
    protected void onDisable() {
        unlisten();
        stage = Stage.SEARCH;
        restoreSlot();
    }

    @Override
    public void onTick(TickEvent event) {
        LocalPlayer player = mc().player;
        if (player == null || mc().level == null || mc().gameMode == null || mc().screen != null) {
            return;
        }
        if (player.isDeadOrDying() || player.isSpectator()) {
            reset();
            return;
        }

        List<Entity> targets = collectTargets(player);
        Entity target = targets.isEmpty() ? null : targets.get(0);

        // Only while falling: the slam belongs to a dive, so nothing happens while you stand or rise.
        if (onlyFalling.get() && (player.onGround() || player.getDeltaMovement().y >= 0.0D)) {
            if (stage != Stage.SEARCH) {
                reset();
            }
            debug("not falling, standing down");
            return;
        }
        // Only fall far enough: a jump or a one block hop is not a slam.
        if (onlyFalling.get() && player.fallDistance < minFallDistance.getDouble()) {
            if (stage != Stage.SEARCH) {
                reset();
            }
            debug("fall {} is below the {} block minimum, standing down", player.fallDistance,
                    minFallDistance.getDouble());
            return;
        }
        if (target == null) {
            if (stage != Stage.SEARCH) {
                reset();
            }
            return;
        }

        applyAim(player, target);

        if (takeoffElytra.get()) {
            takeoffElytra(player);
        }

        long now = System.currentTimeMillis();
        switch (stage) {
            case SEARCH -> startSequence(player, target);
            case TAKE_AXE -> {
                if (now - stageAt < AXE_HOLD) {
                    return;
                }
                // Swing the axe only while the target is actually holding its shield up. If it dropped the
                // shield during the swap, skip the axe and go straight to the mace.
                boolean stillShielding = target instanceof LivingEntity living && living.isBlocking();
                if (stillShielding && trigger.get()) {
                    attack(player, target);
                    debug("axe hit to break the shield");
                } else {
                    debug("shield is down, skipping the axe swing");
                }
                // The mace comes out on its own step, not in the same tick as the axe swing.
                stage = Stage.GO_MACE;
                stageAt = now;
            }
            case GO_MACE -> beginMace(player, target);
            case MACE_HIT -> {
                // Hold the mace for a couple of ticks before the swing, so the stun and the slam read as
                // two separate actions instead of one instant swap.
                if (now - stageAt < SWAP_SETTLE) {
                    debug("holding the mace before the slam");
                    return;
                }
                if (!canReach(player, target)) {
                    debug("target out of reach");
                    reset();
                    return;
                }
                float charge = player.getAttackStrengthScale(0.5F);
                if (!ignoreCooldown.get() && charge < 1.0F) {
                    debug("attack cooldown not charged ({})", charge);
                    return;
                }
                if (trigger.get()) {
                    attack(player, target);
                    debug("mace hit on {} (charge {}, fall {})", target.getName().getString(), charge,
                            player.fallDistance);
                } else {
                    debug("trigger off: mace in hand, not swinging");
                }
                reset();
            }
        }
    }

    /**
     * Starts either the stun path or the direct mace path, remembering the slot that was held first.
     *
     * @param player the local player
     * @param target the target
     */
    private void startSequence(LocalPlayer player, Entity target) {
        rememberSlot();
        boolean blocking = target instanceof LivingEntity living && living.isBlocking();
        boolean hasAxe = InventoryUtils.findBest(AXES) != -1;
        if (stunSlam.get() && blocking && hasAxe) {
            int axe = InventoryUtils.findBest(AXES);
            if (InventoryUtils.getSelectedSlot() != axe) {
                InventoryUtils.holdSlot(axe);
            }
            stage = Stage.TAKE_AXE;
            stageAt = System.currentTimeMillis();
            debug("stun slam: axe slot {} to break the shield", axe);
            return;
        }
        beginMace(player, target);
    }

    /**
     * Gets the mace into hand and moves to the hit stage.
     *
     * <p>When the mace is already in hand the hit can start immediately; otherwise the best Breach or Density
     * mace is taken out and the {@code SWAP_SETTLE} hold in the mace stage gives the slam its own step, so it
     * reads as a separate action from the stun.</p>
     *
     * @param player the local player
     * @param target the target
     */
    private void beginMace(LocalPlayer player, Entity target) {
        if (player.getMainHandItem().is(Items.MACE)) {
            stage = Stage.MACE_HIT;
            stageAt = System.currentTimeMillis();
            return;
        }
        if (!canReach(player, target)) {
            debug("holding the mace back: target too far");
            reset();
            return;
        }
        boolean lowFall = player.fallDistance < minFallDistance.getDouble();
        if (lowFall && !breachCheck.get()) {
            debug("fall distance {} below Min Fall Distance, keeping the current weapon", player.fallDistance);
            reset();
            return;
        }
        int slot = maceSlot(lowFall);
        if (slot == -1) {
            int inventorySlot = InventoryUtils.findBestInInventory(MACES);
            if (inventorySlot == -1) {
                debug("no mace anywhere, keeping the current weapon");
                reset();
                return;
            }
            debug("moving the mace from inventory slot {} to the hotbar", inventorySlot);
            InventoryUtils.quickMove(InventoryUtils.containerSlot(inventorySlot));
            lastSwap = System.currentTimeMillis();
        } else {
            InventoryUtils.holdSlot(slot);
            lastSwap = System.currentTimeMillis();
            debug("swapped to hotbar slot {} ({})", slot, lowFall ? "breach" : "density");
        }
        stage = Stage.MACE_HIT;
        stageAt = System.currentTimeMillis();
    }

    /**
     * Returns the module to searching, putting the old slot back when a swap had been made and Swap Back
     * is on.
     */
    private void reset() {
        if (stage != Stage.SEARCH) {
            if (swapBack.get()) {
                restoreSlot();
            } else {
                previousSlot = -1;
            }
        }
        stage = Stage.SEARCH;
        stageAt = 0L;
    }

    /**
     * Aims, according to the Aim setting.
     *
     * <p>Camera turns the real view, which is what a human does. Off leaves the aim alone, for players who
     * want to line up the slam themselves.</p>
     *
     * @param player the local player
     * @param target the target
     */
    private void applyAim(LocalPlayer player, Entity target) {
        if (aim.is("Camera")) {
            RotationUtils.face(target, AIM_SPEED);
        }
    }

    /**
     * Attacks through the game's own attack path, so the packet on the wire is the one a left click
     * produces.
     *
     * @param player the local player
     * @param target the entity to hit
     */
    private void attack(LocalPlayer player, Entity target) {
        AABB original = null;
        if (expandHitbox.get()) {
            // Inflating the client side box only changes what the client itself resolves; it is restored in
            // the same tick. The server still validates against its own box, which is the flag risk.
            original = target.getBoundingBox();
            target.setBoundingBox(original.inflate(EXPANDED_REACH));
        }
        mc().gameMode.attack(player, target);
        if (original != null) {
            target.setBoundingBox(original);
        }
    }

    /**
     * The distance the mace comes out at and the distance it swings at are the same one, exactly like
     * Shield Drain works: if the module takes the mace out for a target, it also swings at it. It is
     * deliberately tighter than the search distance, so the mace is in hand just before the hit instead of
     * from across the fight.
     *
     * @param player the local player
     * @param target the target
     * @return {@code true} when the target is inside the swap and attack distance
     */
    private boolean canReach(LocalPlayer player, Entity target) {
        double allowed = swapDistance.getDouble() + (expandHitbox.get() ? EXPANDED_REACH : 0.0D);
        double horizontal = Math.hypot(target.getX() - player.getX(), target.getZ() - player.getZ());
        return horizontal <= allowed;
    }

    /**
     * Picks the best mace in the hotbar for the situation.
     *
     * <p>On a short fall the mace with the highest Breach level wins, otherwise the highest Density level.
     * A plain mace counts as level zero, so it is only taken when there is nothing better.</p>
     *
     * @param preferBreach whether the short fall variant should win
     * @return the hotbar slot, or {@code -1}
     */
    private int maceSlot(boolean preferBreach) {
        int bestSlot = -1;
        int bestLevel = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = InventoryUtils.getStack(slot);
            if (!stack.is(Items.MACE)) {
                continue;
            }
            int level = enchantLevel(stack, preferBreach ? BREACH : DENSITY);
            if (level > bestLevel) {
                bestLevel = level;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    /**
     * @param stack       the stack
     * @param enchantment the enchantment to look for
     * @return the level of that enchantment on the stack, {@code 0} when it is not there
     */
    private int enchantLevel(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        for (var entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().unwrapKey().filter(enchantment::equals).isPresent()) {
                return entry.getValue();
            }
        }
        return 0;
    }

    /**
     * @param player the local player
     * @return every attackable player inside the horizontal distance, closest first
     */
    private List<Entity> collectTargets(LocalPlayer player) {
        List<Entity> targets = new ArrayList<>();
        for (Entity entity : WorldUtils.getEntities(horizontalDistance.getDouble() + 1.5D)) {
            if (!(entity instanceof LivingEntity living) || living == player || living.isDeadOrDying()) {
                continue;
            }
            if (living instanceof Player other) {
                if (other.isSpectator() || other.isCreative() || WorldUtils.isFriend(other)) {
                    continue;
                }
            }
            double horizontal = Math.hypot(entity.getX() - player.getX(), entity.getZ() - player.getZ());
            if (horizontal > horizontalDistance.getDouble()) {
                continue;
            }
            targets.add(entity);
        }
        targets.sort(Comparator.comparingDouble(entity -> entity.distanceTo(player)));
        return targets;
    }

    /**
     * Drops the elytra into the inventory when diving, so the glide turns into a smash.
     *
     * <p>Stowing an item is an ordinary inventory action: the same click a player makes when they move
     * their elytra, just timed for you.</p>
     *
     * @param player the local player
     */
    private void takeoffElytra(LocalPlayer player) {
        if (!player.isFallFlying() || player.getDeltaMovement().y >= 0.0D) {
            return;
        }
        if (maceSlot(false) == -1 && InventoryUtils.findBestInInventory(MACES) == -1) {
            return;
        }
        if (System.currentTimeMillis() - lastElytra < 500L) {
            return;
        }
        lastElytra = System.currentTimeMillis();
        // Slot 6 of the player inventory menu is the chest armour slot: quick moving it stows the elytra.
        InventoryUtils.click(6, 0, ClickType.QUICK_MOVE);
        debug("elytra stowed for the dive");
    }

    /**
     * Remembers the slot that was held before the module started swapping.
     */
    private void rememberSlot() {
        if (previousSlot < 0) {
            previousSlot = InventoryUtils.getSelectedSlot();
        }
    }

    /**
     * Returns to the slot that was held before the swap.
     */
    private void restoreSlot() {
        if (previousSlot >= 0) {
            InventoryUtils.holdSlot(previousSlot);
            lastSwap = System.currentTimeMillis();
        }
        previousSlot = -1;
    }

    /**
     * Logs a decision, rate limited so the log stays readable.
     *
     * @param message the format string
     * @param args    the format arguments
     */
    private void debug(String message, Object... args) {
        if (!debug.get()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastDebug < DEBUG_INTERVAL) {
            return;
        }
        lastDebug = now;
        Log.info("AutoMace: " + message, args);
    }
}
