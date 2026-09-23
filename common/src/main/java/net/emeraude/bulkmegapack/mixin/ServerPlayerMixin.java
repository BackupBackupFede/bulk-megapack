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
 * Restocking the main hand. There is no event for "the stack you were holding just ran out", so
 * the tick remembers what the hand held and notices when it goes empty.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Unique private ItemStack bulkmegapack$lastHeld = ItemStack.EMPTY;

    @Inject(method = "tick", at = @At("HEAD"))
    private void bulkmegapack$restock(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        WorldGestures.restockHand(player, this.bulkmegapack$lastHeld);
        this.bulkmegapack$lastHeld = WorldGestures.remember(player);
    }
}
