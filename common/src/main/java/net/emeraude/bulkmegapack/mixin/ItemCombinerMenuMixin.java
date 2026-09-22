package net.emeraude.bulkmegapack.mixin;

import net.emeraude.bulkmegapack.BulkMegapackConfig;
import net.emeraude.bulkmegapack.Refill;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Smithing table. SmithingMenu inherits quickMoveStack from ItemCombinerMenu, so the hook lives
 * here and filters on SmithingMenu (the anvil is an ItemCombinerMenu too, and stays vanilla: it
 * empties its inputs and charges levels, so chaining it would spend XP without asking).
 *
 * <p>onTake shrinks template, base and addition by one each. Templates and ingots stack, so they
 * are topped up before the take; the base is usually a tool or armour piece that doesn't stack,
 * so a slot the take emptied is refilled right after it with an identical item. The result then
 * recomputes to the same item and vanilla's shift-click loop carries on.
 */
@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuMixin {

    private static final int INPUT_SLOTS = 3; // template, base, addition

    /** Inputs as they were before the take in progress, or null when it isn't a smithing take. */
    @Unique private ItemStack[] bulkmegapack$before;

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void bulkmegapack$refillBefore(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        this.bulkmegapack$before = null;
        if (!((Object) this instanceof SmithingMenu menu)) return;
        if (index != menu.getResultSlot() || player.level().isClientSide() || !BulkMegapackConfig.smithing()) return;

        ItemStack[] before = new ItemStack[INPUT_SLOTS];
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            Refill.topUp(menu, player, slot);
            before[slot] = menu.slots.get(slot).getItem().copyWithCount(1);
        }
        this.bulkmegapack$before = before;
    }

    @Inject(method = "quickMoveStack", at = @At("RETURN"))
    private void bulkmegapack$refillAfter(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack[] before = this.bulkmegapack$before;
        this.bulkmegapack$before = null;
        if (before == null || cir.getReturnValue().isEmpty()) return;

        SmithingMenu menu = (SmithingMenu) (Object) this;
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            Refill.refillEmptied(menu, player, slot, before[slot]);
        }
    }
}
