package net.emeraude.bulkmegapack.mixin;

import net.emeraude.bulkmegapack.WorldGestures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restocking the main hand. There is no event for "the stack you were holding just ran out", so the
 * tick watches the hand.
 *
 * <p>It keeps the stack <em>object</em> the hand held, not just a copy of it, because that is what
 * tells "used up" apart from "not in hand any more". A stack consumed in place ends at count 0, so
 * the remembered object reports itself empty; a stack the player scrolled away from, dropped or
 * moved is still a full stack living somewhere else, and then nothing is restocked. Without that
 * distinction, scrolling onto an empty hotbar slot dragged the previous stack along with it.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    /** The very stack the hand held last tick, watched for being used up. */
    @Unique private ItemStack bulkmegapack$lastHeld = ItemStack.EMPTY;
    /** What to look for in the inventory: one of the same item, with the same components. */
    @Unique private ItemStack bulkmegapack$lastPattern = ItemStack.EMPTY;

    @Inject(method = "tick", at = @At("HEAD"))
    private void bulkmegapack$restock(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        ItemStack held = player.getMainHandItem();

        boolean usedUp = held.isEmpty() && !this.bulkmegapack$lastPattern.isEmpty() && this.bulkmegapack$lastHeld.isEmpty();
        if (usedUp) {
            WorldGestures.restockHand(player, this.bulkmegapack$lastPattern);
            held = player.getMainHandItem();
        }

        if (held.isEmpty()) {
            this.bulkmegapack$lastHeld = ItemStack.EMPTY;
            this.bulkmegapack$lastPattern = ItemStack.EMPTY;
        } else {
            this.bulkmegapack$lastHeld = held;
            this.bulkmegapack$lastPattern = held.copyWithCount(1);
        }
    }
}
