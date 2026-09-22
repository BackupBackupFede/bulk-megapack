package net.emeraude.bulkmegapack.mixin;

import net.emeraude.bulkmegapack.BulkMegapackConfig;
import net.emeraude.bulkmegapack.Refill;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stonecutter: vanilla already chains a shift-click on the result (onTake takes one input and
 * recomputes the result), it just stops when the input slot runs dry. Topping the input up before
 * each take keeps the chain going through the whole inventory. The selected recipe survives,
 * because slotsChanged only resets it when the input changes item.
 */
@Mixin(StonecutterMenu.class)
public abstract class StonecutterMenuMixin {

    private static final int INPUT_SLOT = 0;
    private static final int RESULT_SLOT = 1;

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void bulkmegapack$refill(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index != RESULT_SLOT || player.level().isClientSide() || !BulkMegapackConfig.stonecutter()) return;
        Refill.topUp((AbstractContainerMenu) (Object) this, player, INPUT_SLOT);
    }
}
