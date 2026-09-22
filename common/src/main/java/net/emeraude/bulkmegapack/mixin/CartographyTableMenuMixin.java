package net.emeraude.bulkmegapack.mixin;

import net.emeraude.bulkmegapack.BulkMegapackConfig;
import net.emeraude.bulkmegapack.Refill;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cartography table: onTake takes one map and one additional item (empty map, paper, glass pane).
 * Both are topped up before each take. Cloning needs one more step: the usual case is a single
 * map, which the take consumes while its two copies land in the inventory. Refilling the emptied
 * map slot with one of those copies right after the take keeps the chain going, so one click
 * turns every empty map in the inventory into a copy.
 */
@Mixin(CartographyTableMenu.class)
public abstract class CartographyTableMenuMixin {

    private static final int INPUT_SLOTS = 2; // map, additional
    private static final int RESULT_SLOT = 2;

    /** Inputs as they were before the take in progress, or null when it isn't a result take. */
    @Unique private ItemStack[] bulkmegapack$before;

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void bulkmegapack$refillBefore(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        this.bulkmegapack$before = null;
        if (index != RESULT_SLOT || player.level().isClientSide() || !BulkMegapackConfig.cartography()) return;

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
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

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            Refill.refillEmptied(menu, player, slot, before[slot]);
        }
    }
}
