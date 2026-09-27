package dev.ferro.client.module.modules.movement;

import dev.ferro.client.event.events.TickEvent;
import dev.ferro.client.event.events.TickListener;
import dev.ferro.client.module.Category;
import dev.ferro.client.module.Module;
import dev.ferro.client.module.setting.BooleanSetting;
import dev.ferro.client.module.setting.MinMaxSetting;
import dev.ferro.client.utils.InventoryUtils;
import dev.ferro.client.utils.Log;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Instant Lunge — one keypress takes the spear out, lunges and puts the old item back.
 *
 * <p>This is a movement module: the spear's lunge is a dash, so nothing here aims, attacks or builds a
 * packet. The module switches to the spear like a player would, uses it (that is the same call a right click
 * makes) and switches back after a random delay inside {@code Swap Back Delay}, then it switches itself off
 * again. The item cooldown is deliberately not consulted, which is what makes the lunge instant.</p>
 */
public class InstantLunge extends Module implements TickListener {

    /** Spears, best first. */
    private static final Item[] SPEARS = {Items.NETHERITE_SPEAR, Items.DIAMOND_SPEAR, Items.IRON_SPEAR,
            Items.COPPER_SPEAR, Items.GOLDEN_SPEAR, Items.STONE_SPEAR, Items.WOODEN_SPEAR};
    /** The spear enchantment the module looks for. */
    private static final ResourceKey<Enchantment> LUNGE = Enchantments.LUNGE;
    /** Milliseconds between taking the spear out and using it, so the client and server agree on the slot. */
    private static final long SETTLE = 50L;
    /** Milliseconds between two debug lines. */
    private static final long DEBUG_INTERVAL = 500L;

    /** Where the sequence currently is. */
    private enum Stage {
        /** The spear is taken out and aimed at nothing: waiting to use it. */
        ARMED,
        /** The lunge went out, waiting for the swap back. */
        LUNGED
    }

    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", true,
            "Returns to the previous slot after the lunge.");
    private final MinMaxSetting swapBackDelay = new MinMaxSetting("Swap Back Delay", 0.0D, 500.0D, 5.0D, 20.0D,
            70.0D, "Waits a random time in this range before swapping back (ms).");
    private final BooleanSetting debug = new BooleanSetting("Debug", false,
            "Logs every lunge step into the game log, so you can see why a press does or does not lunge.");

    private Stage stage = Stage.ARMED;
    private long armedAt;
    private long swappedAt;
    private long lungedAt;
    private long waitForSwapBack;
    private long lastDebug;
    private int previousSlot = -1;

    /**
     * Creates the module.
     */
    public InstantLunge() {
        super("Instant Lunge", Category.MOVEMENT, "Instantly lunges with the spear without needing to swap or click.");
        swapBackDelay.visibleWhen(swapBack::get);
        addSettings(swapBack, swapBackDelay, debug);
    }

    @Override
    protected void onEnable() {
        listen(TickEvent.class);
        stage = Stage.ARMED;
        armedAt = System.currentTimeMillis();
        swappedAt = 0L;
        previousSlot = -1;
    }

    @Override
    protected void onDisable() {
        unlisten();
        restore();
    }

    @Override
    public void onTick(TickEvent event) {
        LocalPlayer player = mc().player;
        if (player == null || mc().level == null || mc().gameMode == null) {
            setEnabled(false);
            return;
        }
        if (player.isDeadOrDying() || player.isSpectator()) {
            setEnabled(false);
            return;
        }
        if (mc().screen != null) {
            return;
        }
        long now = System.currentTimeMillis();

        if (stage == Stage.LUNGED) {
            if (swapBack.get() && now - lungedAt < waitForSwapBack) {
                return;
            }
            restore();
            setEnabled(false);
            return;
        }

        int slot = lungeSlot();
        if (slot == -1) {
            // No hotbar spear carries the Lunge enchantment: stand down without lunging.
            debug("no Lunge spear in the hotbar, standing down");
            setEnabled(false);
            return;
        }
        if (previousSlot < 0) {
            previousSlot = InventoryUtils.getSelectedSlot();
            debug("remembered previous slot {}", previousSlot);
        }
        if (InventoryUtils.getSelectedSlot() != slot) {
            InventoryUtils.holdSlot(slot);
            swappedAt = now;
            debug("swapped to lunge spear slot {} (lunge level {})", slot, enchantLevel(InventoryUtils.getStack(slot)));
            return;
        }
        if (swappedAt != 0L && now - swappedAt < SETTLE) {
            return;
        }
        // The right click a player would make, and without a cooldown check: the lunge goes out instantly.
        player.swing(InteractionHand.MAIN_HAND);
        mc().gameMode.useItem(player, InteractionHand.MAIN_HAND);
        lungedAt = now;
        waitForSwapBack = (long) swapBackDelay.random();
        stage = Stage.LUNGED;
        debug("lunge released, waiting {} ms then swapping back", waitForSwapBack);
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
        Log.info("Instant Lunge: " + message, args);
    }

    /**
     * Finds the hotbar spear carrying the highest Lunge enchantment level.
     *
     * <p>Only the hotbar is considered, so a Lunge spear sitting in the main inventory is ignored. A spear
     * with Lunge three wins over Lunge two, and so on; a spear without any Lunge enchantment is never
     * picked.</p>
     *
     * @return the hotbar slot, or {@code -1} when no hotbar spear has a Lunge enchantment
     */
    private int lungeSlot() {
        int bestSlot = -1;
        int bestLevel = -1;
        for (int slot = 0; slot < InventoryUtils.HOTBAR_SIZE; slot++) {
            ItemStack stack = InventoryUtils.getStack(slot);
            if (!isSpear(stack.getItem())) {
                continue;
            }
            int level = enchantLevel(stack);
            if (level > bestLevel) {
                bestLevel = level;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    /**
     * @param item the item to test
     * @return {@code true} when the item is one of the spear types
     */
    private boolean isSpear(Item item) {
        for (Item spear : SPEARS) {
            if (item == spear) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param stack the stack
     * @return the Lunge enchantment level on the stack, {@code 0} when it is not there
     */
    private int enchantLevel(ItemStack stack) {
        for (var entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().unwrapKey().filter(LUNGE::equals).isPresent()) {
                return entry.getValue();
            }
        }
        return 0;
    }

    /**
     * Returns to the slot that was held before the lunge.
     */
    private void restore() {
        if (swapBack.get() && previousSlot >= 0) {
            InventoryUtils.holdSlot(previousSlot);
        }
        previousSlot = -1;
        swappedAt = 0L;
        stage = Stage.ARMED;
    }
}
