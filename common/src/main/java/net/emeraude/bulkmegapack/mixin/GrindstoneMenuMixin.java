package net.emeraude.bulkmegapack.mixin;

import net.emeraude.bulkmegapack.BulkMegapackConfig;
import net.emeraude.bulkmegapack.Refill;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Grindstone — the one station vanilla doesn't chain: onTake empties both input slots, so there is
 * nothing left to top up. We refill AFTER the take instead (RETURN of quickMoveStack) and drive the
 * chain ourselves, because the next item may be a different type (a bow after a sword) and the
 * vanilla loop stops on a type change.
 *
 * <p>Only the disenchant use is chained: exactly one input filled with something grindable. Each
 * pass is a real vanilla take, so the XP (a fresh 50-100% roll per item) is exactly what clicking
 * one by one would give. Renamed items and, by default, the hotbar are never touched.
 */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {

    private static final int INPUT_SLOT = 0;
    private static final int ADDITIONAL_SLOT = 1;
    private static final int RESULT_SLOT = 2;
    /** Upper bound on one chain; a full inventory is 36 slots, so this is never the real limit. */
    private static final int MAX_CHAIN = 64;

    /** Input slot used by the take in progress, or -1 when it isn't a disenchant. */
    @Unique private int bulkmegapack$grindSlot = -1;
    /** True while our own chain runs, so the nested takes don't start chains of their own. */
    @Unique private boolean bulkmegapack$chaining = false;

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void bulkmegapack$recordInput(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        this.bulkmegapack$grindSlot = -1;
        if (index != RESULT_SLOT || player.level().isClientSide() || !BulkMegapackConfig.grindstone()) return;

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        ItemStack input = menu.slots.get(INPUT_SLOT).getItem();
        ItemStack additional = menu.slots.get(ADDITIONAL_SLOT).getItem();
        if (!input.isEmpty() && additional.isEmpty() && Refill.isGrindable(input)) {
            this.bulkmegapack$grindSlot = INPUT_SLOT;
        } else if (input.isEmpty() && !additional.isEmpty() && Refill.isGrindable(additional)) {
            this.bulkmegapack$grindSlot = ADDITIONAL_SLOT;
        }
    }

    @Inject(method = "quickMoveStack", at = @At("RETURN"))
    private void bulkmegapack$chain(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        int slot = this.bulkmegapack$grindSlot;
        if (slot < 0 || this.bulkmegapack$chaining || cir.getReturnValue().isEmpty()) return;

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        this.bulkmegapack$chaining = true;
        try {
            for (int i = 0; i < MAX_CHAIN && Refill.grindstoneNext(menu, player, slot); i++) {
                // Moving the item out of the inventory freed a slot, so the result always fits.
                if (menu.quickMoveStack(player, RESULT_SLOT).isEmpty()) break;
            }
        } finally {
            this.bulkmegapack$chaining = false;
        }
    }
}
