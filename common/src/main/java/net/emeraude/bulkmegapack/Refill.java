package net.emeraude.bulkmegapack;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Moves items from the player's inventory into a station's input slots. Shared by every station
 * mixin; works on the menu's own slot list, so it needs no per-station slot ranges.
 */
public final class Refill {

    private Refill() {}

    /**
     * Tops up a non-empty input slot with identical stacks (same item, same components) from the
     * player's inventory, up to the slot's stack limit. An empty slot is left alone: we only keep
     * going with what the player put there, never pick something new.
     */
    public static void topUp(AbstractContainerMenu menu, Player player, int slotIndex) {
        Slot target = menu.slots.get(slotIndex);
        ItemStack stack = target.getItem();
        if (stack.isEmpty()) return;

        int max = target.getMaxStackSize(stack);
        if (stack.getCount() >= max) return;

        Inventory inventory = player.getInventory();
        for (Slot source : menu.slots) {
            if (source == target || source.container != inventory) continue;
            ItemStack candidate = source.getItem();
            if (candidate.isEmpty() || !ItemStack.isSameItemSameComponents(candidate, stack)) continue;

            int moved = Math.min(max - stack.getCount(), candidate.getCount());
            stack.grow(moved);
            candidate.shrink(moved);
            source.setChanged();
            if (stack.getCount() >= max) break;
        }
        target.setChanged(); // lets the menu recompute its result
    }

    /**
     * Refills a slot the take just emptied with a stack identical to what it held before. This is
     * the only way to chain unstackable inputs (a diamond sword in the smithing table): a full
     * slot of one can't be topped up beforehand.
     */
    public static void refillEmptied(AbstractContainerMenu menu, Player player, int slotIndex, ItemStack before) {
        Slot target = menu.slots.get(slotIndex);
        if (before.isEmpty() || target.hasItem()) return;

        Inventory inventory = player.getInventory();
        for (Slot source : menu.slots) {
            if (source.container != inventory) continue;
            ItemStack candidate = source.getItem();
            if (candidate.isEmpty() || !ItemStack.isSameItemSameComponents(candidate, before)) continue;

            target.set(candidate.split(Math.min(candidate.getCount(), target.getMaxStackSize(candidate))));
            source.setChanged();
            return;
        }
    }

    /**
     * Hands the station's leftovers back to the player, once a chain has stopped. Vanilla would
     * return them when the screen closes; giving them back right away means the station is left as
     * it was found instead of holding a stack the player has to fish out.
     *
     * <p>Anything that doesn't fit stays in the slot, exactly as vanilla would leave it.
     */
    public static void giveBack(AbstractContainerMenu menu, Player player, int... slotIndexes) {
        Inventory inventory = player.getInventory();
        for (int slotIndex : slotIndexes) {
            Slot source = menu.slots.get(slotIndex);
            ItemStack leftover = source.getItem();
            if (leftover.isEmpty()) continue;

            // Merge into matching stacks first, then into the first empty slot.
            for (Slot target : menu.slots) {
                if (target.container != inventory || leftover.isEmpty()) continue;
                ItemStack destination = target.getItem();
                if (destination.isEmpty() || !ItemStack.isSameItemSameComponents(destination, leftover)) continue;

                int room = Math.min(target.getMaxStackSize(destination), destination.getMaxStackSize()) - destination.getCount();
                int moved = Math.min(room, leftover.getCount());
                if (moved <= 0) continue;
                destination.grow(moved);
                leftover.shrink(moved);
                target.setChanged();
            }
            for (Slot target : menu.slots) {
                if (target.container != inventory || leftover.isEmpty() || target.hasItem()) continue;
                target.set(leftover.split(leftover.getCount()));
                target.setChanged();
            }
            source.setChanged();
        }
    }

    /**
     * Puts one grindable item from the player's inventory into an empty grindstone input slot.
     * Only items the grindstone would actually strip (at least one non-curse enchantment), never a
     * renamed item, and never from the hotbar unless the config allows it.
     *
     * @return true if an item was moved
     */
    public static boolean grindstoneNext(AbstractContainerMenu menu, Player player, int slotIndex) {
        Slot target = menu.slots.get(slotIndex);
        if (target.hasItem()) return false;

        Inventory inventory = player.getInventory();
        boolean hotbar = BulkMegapackConfig.grindstoneIncludeHotbar();
        for (Slot source : menu.slots) {
            if (source.container != inventory) continue;
            if (!hotbar && Inventory.isHotbarSlot(source.getContainerSlot())) continue;

            ItemStack candidate = source.getItem();
            if (!isGrindable(candidate) || !target.mayPlace(candidate)) continue;

            target.set(candidate.split(1));
            source.setChanged();
            return true;
        }
        return false;
    }

    /** True when grinding gives something back: a non-curse enchantment, and no custom name. */
    public static boolean isGrindable(ItemStack stack) {
        if (stack.isEmpty() || stack.has(DataComponents.CUSTOM_NAME)) return false;
        for (Holder<Enchantment> enchantment : EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet()) {
            if (!enchantment.is(EnchantmentTags.CURSE)) return true;
        }
        return false;
    }
}
