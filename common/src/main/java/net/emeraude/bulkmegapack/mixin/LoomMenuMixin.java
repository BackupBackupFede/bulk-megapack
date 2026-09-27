package net.emeraude.bulkmegapack.mixin;

import net.emeraude.bulkmegapack.BulkMegapackConfig;
import net.emeraude.bulkmegapack.Refill;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Loom: onTake takes one banner and one dye, and clears the chosen pattern as soon as either slot
 * runs empty — so here the slots are topped up BEFORE the take rather than refilled after it. The
 * pattern item is never consumed, so only banners and dye limit the run, and whatever is left over
 * once the chain stops is handed back to the player.
 */
@Mixin(LoomMenu.class)
public abstract class LoomMenuMixin {

    private static final int BANNER_SLOT = 0;
    private static final int DYE_SLOT = 1;
    private static final int RESULT_SLOT = 3;

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void bulkmegapack$refill(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index != RESULT_SLOT || player.level().isClientSide() || !BulkMegapackConfig.loom()) return;
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        Refill.topUp(menu, player, BANNER_SLOT);
        Refill.topUp(menu, player, DYE_SLOT);
    }

    @Inject(method = "quickMoveStack", at = @At("RETURN"))
    private void bulkmegapack$giveBack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index != RESULT_SLOT || player.level().isClientSide() || !BulkMegapackConfig.loom()) return;
        if (cir.getReturnValue().isEmpty()) return;

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (!menu.slots.get(RESULT_SLOT).hasItem()) Refill.giveBack(menu, player, BANNER_SLOT, DYE_SLOT);
    }
}
