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
 * <p>onTake shrinks template, base and addition by one each, and every slot the take empties gets
 * one identical replacement right after it — the only way to chain an unstackable base such as a
 * diamond sword. Inputs are not topped up to full stacks, so nothing is stranded in the table when
 * the chain stops; what remains at that point is handed back to the player.
 */
@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuMixin {

    private static final int TEMPLATE_SLOT = 0;
    private static final int BASE_SLOT = 1;
    private static final int ADDITION_SLOT = 2;
    private static final int INPUT_SLOTS = 3;

    /** Inputs as they were before the take in progress, or null when it isn't a smithing take. */
    @Unique private ItemStack[] bulkmegapack$before;

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void bulkmegapack$refillBefore(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        this.bulkmegapack$before = null;
        if (!((Object) this instanceof SmithingMenu menu)) return;
        if (index != menu.getResultSlot() || player.level().isClientSide() || !BulkMegapackConfig.smithing()) return;

        ItemStack[] before = new ItemStack[INPUT_SLOTS];
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
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
        if (!menu.slots.get(menu.getResultSlot()).hasItem()) {
            Refill.giveBack(menu, player, TEMPLATE_SLOT, BASE_SLOT, ADDITION_SLOT);
        }
    }
}
