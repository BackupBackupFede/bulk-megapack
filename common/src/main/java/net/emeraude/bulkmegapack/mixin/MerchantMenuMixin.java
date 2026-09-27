package net.emeraude.bulkmegapack.mixin;

import net.emeraude.bulkmegapack.BulkMegapackConfig;
import net.minecraft.world.entity.player.Player;
import net.emeraude.bulkmegapack.Refill;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The only thing the mod adds: refill the payment slots from the player's inventory before each
 * trade.
 *
 * <p>Vanilla already chains trades on a shift-click (AbstractContainerMenu.doClick re-runs
 * quickMoveStack in a loop), but only consumes what's in the 2 payment slots and never refills
 * them. We inject at the HEAD of quickMoveStack (result slot), call the vanilla
 * {@code moveFromInventoryToPaymentSlot}, and let vanilla do the trade. Its own loop keeps calling
 * quickMoveStack, so the trade continues until the inventory is drained.
 *
 * <p>The merchant API is identical on 1.21.1 and 26.2 (verified against decompiled sources), so
 * this single mixin compiles and applies on both versions and both loaders (Mojmap names).
 */
@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {

    private static final int PAYMENT1_SLOT = 0;
    private static final int PAYMENT2_SLOT = 1;
    private static final int RESULT_SLOT = 2;

    @Shadow @Final private MerchantContainer tradeContainer;

    @Shadow protected abstract void moveFromInventoryToPaymentSlot(int paymentSlotIndex, ItemCost payment);

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void bulkmegapack$refillBeforeTrade(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index != RESULT_SLOT || player.level().isClientSide()) return;
        if (!BulkMegapackConfig.merchant()) return;

        // getActiveOffer() is non-null only when the payment slots already hold the cost -> always
        // true here, since the result slot is only clickable in that case. Otherwise: no-op (vanilla).
        MerchantOffer offer = this.tradeContainer.getActiveOffer();
        if (offer == null) return;

        this.moveFromInventoryToPaymentSlot(0, offer.getItemCostA());
        offer.getItemCostB().ifPresent(cost -> this.moveFromInventoryToPaymentSlot(1, cost));
        this.tradeContainer.updateSellItem(); // recompute the result slot
    }

    @Inject(method = "quickMoveStack", at = @At("RETURN"))
    private void bulkmegapack$giveBack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index != RESULT_SLOT || player.level().isClientSide() || !BulkMegapackConfig.merchant()) return;
        if (cir.getReturnValue().isEmpty()) return;

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (!menu.slots.get(RESULT_SLOT).hasItem()) Refill.giveBack(menu, player, PAYMENT1_SLOT, PAYMENT2_SLOT);
    }
}
