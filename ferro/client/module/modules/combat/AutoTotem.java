
package dev.ferro.client.module.modules.combat;

import dev.ferro.client.event.events.TickEvent;
import dev.ferro.client.event.events.TickListener;
import dev.ferro.client.module.Category;
import dev.ferro.client.module.Module;
import dev.ferro.client.module.setting.BooleanSetting;
import dev.ferro.client.module.setting.ModeSetting;
import dev.ferro.client.module.setting.NumberSetting;
import dev.ferro.client.module.setting.StringSetting;
import dev.ferro.client.utils.InventoryUtils;
import dev.ferro.client.utils.MathUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

/**
 * Keeps a totem of undying where you want it.
 *
 * <p>{@code Target} decides what the module manages: the offhand, a specific hotbar slot, or nothing at all.
 * For the offhand the health threshold applies, for a hotbar slot the module keeps it stocked permanently,
 * because that is the point of picking a slot for it.</p>
 *
 * <p>{@code Keep In Offhand} is the important safety valve: anything whose name matches that list is left
 * alone. Put a shield there and the module will never take it out — the totem only goes to the offhand when
 * the offhand is empty or already holds a totem.</p>
 */
public class AutoTotem extends Module implements TickListener {

    /** Swap click button that targets the offhand. */
    public static final int OFFHAND_SWAP_BUTTON = 40;

    private final ModeSetting target = new ModeSetting("Target", "Offhand", "Off", "Offhand", "Hotbar");
    private final NumberSetting hotbarSlot = new NumberSetting("Hotbar Slot", 1.0D, 9.0D, 1.0D, 1.0D,
            "The hotbar slot the totem is kept in.");
    private final NumberSetting healthThreshold = new NumberSetting("Health", 1.0D, 20.0D, 0.5D, 16.0D,
            "Health below which the offhand is stocked.");
    private final ModeSetting mode = new ModeSetting("Mode", "Legit", "Legit", "Blatant");
    private final NumberSetting delay = new NumberSetting("Delay", 0.0D, 1000.0D, 10.0D, 150.0D);
    private final StringSetting keepInOffhand = new StringSetting("Keep In Offhand", "shield", 128,
            "Comma separated item names that are never taken out of the offhand.");
    private final BooleanSetting refill = new BooleanSetting("Refill", true,
            "Move totems from your inventory into the hotbar.");
    private final BooleanSetting pauseOnKill = new BooleanSetting("Pause On Kill", false,
            "Stop for a moment right after you killed someone.");

    private long lastSwap;
    private long lastKill;

    /**
     * Creates the module.
     */
    public AutoTotem() {
        super("AutoTotem", Category.COMBAT, "Keeps a totem where you want it.");
        hotbarSlot.visibleWhen(() -> target.is("Hotbar"));
        addSettings(target, hotbarSlot, healthThreshold, mode, delay, keepInOffhand, refill, pauseOnKill);
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
        if (player == null || mc().gameMode == null || target.is("Off")) {
            return;
        }
        if (pauseOnKill.get() && System.currentTimeMillis() - lastKill < 2000L) {
            return;
        }
        long now = System.currentTimeMillis();
        long interval = mode.is("Blatant") ? 0L : (long) delay.getDouble();
        if (now - lastSwap < interval) {
            return;
        }
        if (target.is("Hotbar")) {
            stockHotbar(player, now);
            return;
        }
        if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            if (refill.get()) {
                refillHotbar();
            }
            return;
        }
        if (keepsOffhandItem(player)) {
            return;
        }
        if (player.getHealth() + player.getAbsorptionAmount() > healthThreshold.getFloat()) {
            return;
        }
        int slot = InventoryUtils.findInInventory(Items.TOTEM_OF_UNDYING);
        if (slot < 0) {
            return;
        }
        InventoryUtils.click(InventoryUtils.containerSlot(slot), OFFHAND_SWAP_BUTTON, ClickType.SWAP);
        lastSwap = now;
        if (refill.get()) {
            refillHotbar();
        }
    }

    /**
     * Keeps the chosen hotbar slot stocked with a totem.
     *
     * @param player the local player
     * @param now    the current timestamp
     */
    private void stockHotbar(LocalPlayer player, long now) {
        int slot = MathUtils.clamp(hotbarSlot.getInt() - 1, 0, InventoryUtils.HOTBAR_SIZE - 1);
        if (InventoryUtils.getStack(slot).is(Items.TOTEM_OF_UNDYING)) {
            if (refill.get()) {
                refillHotbar();
            }
            return;
        }
        int source = -1;
        for (int index = 0; index < 36; index++) {
            if (index != slot && InventoryUtils.getStack(index).is(Items.TOTEM_OF_UNDYING)) {
                source = index;
                break;
            }
        }
        if (source < 0) {
            return;
        }
        InventoryUtils.click(InventoryUtils.containerSlot(source), slot, ClickType.SWAP);
        lastSwap = now;
    }

    /**
     * @param player the local player
     * @return {@code true} when the offhand holds something that must not be swapped out
     */
    private boolean keepsOffhandItem(LocalPlayer player) {
        ItemStack offhand = player.getOffhandItem();
        if (offhand.isEmpty() || keepInOffhand.get().isBlank()) {
            return false;
        }
        String name = offhand.getHoverName().getString().toLowerCase(Locale.ROOT);
        for (String entry : keepInOffhand.get().split(",")) {
            String wanted = entry.trim().toLowerCase(Locale.ROOT);
            if (!wanted.isEmpty() && name.contains(wanted)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Moves totems from the main inventory into the hotbar so a swap stays fast.
     */
    private void refillHotbar() {
        if (InventoryUtils.findInHotbar(Items.TOTEM_OF_UNDYING) != -1) {
            return;
        }
        for (int index = InventoryUtils.HOTBAR_SIZE; index < 36; index++) {
            if (InventoryUtils.getStack(index).is(Items.TOTEM_OF_UNDYING)) {
                InventoryUtils.quickMove(InventoryUtils.containerSlot(index));
                return;
            }
        }
    }

    /**
     * Called by combat modules when a kill is registered.
     */
    public void onKill() {
        lastKill = System.currentTimeMillis();
    }

    /**
     * @return the health threshold currently configured
     */
    public float getHealthThreshold() {
        return healthThreshold.getFloat();
    }

    /**
     * @param player the local player
     * @return {@code true} when the player is low enough to need a totem
     */
    public boolean needsTotem(LocalPlayer player) {
        return player != null && player.getHealth() + player.getAbsorptionAmount() <= healthThreshold.getFloat();
    }

    /**
     * @return the target mode that is currently selected
     */
    public String getTarget() {
        return target.get();
    }
}
