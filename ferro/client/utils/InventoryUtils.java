package dev.ferro.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Inventory and hotbar helpers.
 *
 * <p>Every version sensitive inventory call is funnelled through this class so that a Minecraft
 * update only needs a fix in one place: {@code selected} versus {@code getSelectedSlot()} is exactly
 * the kind of rename that keeps breaking clients.</p>
 */
public final class InventoryUtils {

    /** Amount of hotbar slots. */
    public static final int HOTBAR_SIZE = 9;

    private InventoryUtils() {
    }

    /**
     * @return the local player, or {@code null}
     */
    public static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    /**
     * @return the currently selected hotbar slot, or {@code 0} when unavailable
     */
    public static int getSelectedSlot() {
        LocalPlayer player = player();
        return player == null ? 0 : player.getInventory().getSelectedSlot();
    }

    /**
     * Selects a hotbar slot on the client. Vanilla syncs it to the server on the next tick.
     *
     * @param slot the hotbar slot, {@code 0} to {@code 8}
     * @return the slot that was selected
     */
    public static int holdSlot(int slot) {
        LocalPlayer player = player();
        if (player == null) {
            return -1;
        }
        int clamped = MathUtils.clamp(slot, 0, HOTBAR_SIZE - 1);
        player.getInventory().setSelectedSlot(clamped);
        return clamped;
    }

    /**
     * Switches the server side held slot without changing the client's selected slot. This is the
     * "silent switch" used by crystal and anchor modules.
     *
     * @param slot the hotbar slot, {@code 0} to {@code 8}
     */
    public static void holdSlotSilently(int slot) {
        if (slot < 0 || slot >= HOTBAR_SIZE) {
            return;
        }
        PacketUtils.sendCarriedItem(slot);
    }

    /**
     * @param slot the inventory slot, {@code 0} to {@code 35} for the main inventory
     * @return the stack in that slot, never {@code null}
     */
    public static ItemStack getStack(int slot) {
        LocalPlayer player = player();
        if (player == null || slot < 0 || slot >= 36) {
            return ItemStack.EMPTY;
        }
        return player.getInventory().getItem(slot);
    }

    /**
     * @return the stack the player is currently holding
     */
    public static ItemStack getMainHand() {
        LocalPlayer player = player();
        return player == null ? ItemStack.EMPTY : player.getMainHandItem();
    }

    /**
     * @return the stack in the offhand
     */
    public static ItemStack getOffHand() {
        LocalPlayer player = player();
        return player == null ? ItemStack.EMPTY : player.getOffhandItem();
    }

    /**
     * Finds a hotbar slot containing one of the given items.
     *
     * @param items the items to look for
     * @return the hotbar slot, or {@code -1}
     */
    public static int findInHotbar(Item... items) {
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            ItemStack stack = getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            for (Item item : items) {
                if (stack.is(item)) {
                    return slot;
                }
            }
        }
        return -1;
    }

    /**
     * Finds the best hotbar slot among a list of items that is ordered by preference.
     *
     * @param byPriority the items, best first
     * @return the hotbar slot with the best available item, or {@code -1}
     */
    public static int findBest(Item... byPriority) {
        for (Item item : byPriority) {
            int slot = findInHotbar(item);
            if (slot != -1) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * Finds the best slot in the whole inventory among a list of items that is ordered by preference.
     *
     * @param byPriority the items, best first
     * @return the inventory slot with the best available item, or {@code -1}
     */
    public static int findBestInInventory(Item... byPriority) {
        for (Item item : byPriority) {
            int slot = findInInventory(item);
            if (slot != -1) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * Finds a slot in the whole inventory containing one of the given items.
     *
     * @param items the items to look for
     * @return the inventory slot, or {@code -1}
     */
    public static int findInInventory(Item... items) {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            for (Item item : items) {
                if (stack.is(item)) {
                    return slot;
                }
            }
        }
        return -1;
    }

    /**
     * Finds a hotbar slot holding a block from a comma separated list of block names.
     *
     * @param names block names, for example {@code "obsidian, crying_obsidian"}
     * @return the hotbar slot, or {@code -1}
     */
    public static int findBlockInHotbar(List<Block> blocks) {
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            ItemStack stack = getStack(slot);
            if (stack.getItem() instanceof BlockItem blockItem && blocks.contains(blockItem.getBlock())) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * @param blocks the blocks to look for
     * @return the amount of matching blocks in the whole inventory
     */
    public static int countBlocks(List<Block> blocks) {
        int count = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = getStack(slot);
            if (stack.getItem() instanceof BlockItem blockItem && blocks.contains(blockItem.getBlock())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * @param slot the slot to inspect
     * @return {@code true} when the slot contains an empty bucket
     */
    public static boolean isEmptyBucket(int slot) {
        return getStack(slot).is(Items.BUCKET);
    }

    /**
     * @param stack the stack to test
     * @return {@code true} when the stack is a sword
     */
    public static boolean isSword(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ItemTags.SWORDS);
    }

    /**
     * @param stack the stack to test
     * @return {@code true} when the stack is an axe
     */
    public static boolean isAxe(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ItemTags.AXES);
    }

    /**
     * @param stack the stack to test
     * @return {@code true} for anything that can attack at melee range
     */
    public static boolean isWeapon(ItemStack stack) {
        return isSword(stack) || isAxe(stack);
    }

    /**
     * @param stack the stack to test
     * @return {@code true} when the stack is edible
     */
    public static boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && stack.get(net.minecraft.core.component.DataComponents.FOOD) != null;
    }

    /**
     * @return the inventory slots that contain food, sorted from the hotbar downwards
     */
    public static List<Integer> getFoodSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            if (isFood(getStack(slot))) {
                slots.add(slot);
            }
        }
        return slots;
    }

    /**
     * @return the total armour value of the worn armour
     */
    public static int getArmorValue() {
        LocalPlayer player = player();
        if (player == null) {
            return 0;
        }
        return player.getArmorValue();
    }

    /**
     * @param slot the equipment slot
     * @return the worn stack in that slot
     */
    public static ItemStack getArmor(EquipmentSlot slot) {
        LocalPlayer player = player();
        return player == null ? ItemStack.EMPTY : player.getItemBySlot(slot);
    }

    /**
     * Performs an inventory click through the interaction manager.
     *
     * @param slot   the container slot, use {@code 36} to {@code 44} for the hotbar
     * @param button the mouse button, {@code 0} for left
     * @param type   the click type
     */
    public static void click(int slot, int button, ClickType type) {
        LocalPlayer player = player();
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null || minecraft.gameMode == null) {
            return;
        }
        minecraft.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, slot, button, type, player);
    }

    /**
     * Converts an inventory slot index into the matching slot of the player inventory menu. The player
     * menu numbers the hotbar as 36..44 and the main inventory as 9..35.
     *
     * @param inventorySlot the inventory slot, {@code 0} to {@code 35}
     * @return the menu slot
     */
    public static int containerSlot(int inventorySlot) {
        return inventorySlot < HOTBAR_SIZE ? 36 + inventorySlot : inventorySlot;
    }

    /**
     * Moves an item from the inventory into the hotbar with a shift click.
     *
     * @param slot the inventory slot to quick move
     */
    public static void quickMove(int slot) {
        click(slot, 0, ClickType.QUICK_MOVE);
    }

    /**
     * @param item the item to count
     * @return the amount of that item in the whole inventory
     */
    public static int count(Item item) {
        int total = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = getStack(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * @return {@code true} when the player holds a throwable item in either hand
     */
    public static boolean isHoldingThrowable() {
        return getMainHand().is(Items.ENDER_PEARL) || getMainHand().is(Items.SPLASH_POTION);
    }
}
