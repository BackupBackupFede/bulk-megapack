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
 * Loom: onTake takes one banner and one dye, and the pattern stays selected while both slots hold
 * something. Topping up the banner and the dye before each take applies the same pattern to every
 * identical banner in the inventory. The pattern item itself is never consumed.
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
}
