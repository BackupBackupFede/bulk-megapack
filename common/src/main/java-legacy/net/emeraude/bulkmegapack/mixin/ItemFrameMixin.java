package net.emeraude.bulkmegapack.mixin;

import net.emeraude.bulkmegapack.WorldGestures;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sneak + right-click a filled item frame: the item turns back one step instead of forward.
 *
 * <p>1.21.x shape of {@code interact}. The 26.x one takes the hit location, and the two cannot
 * live in one class: a handler's parameters must mirror the target's, and on Fabric the refmap
 * matches by name only, so a descriptor in the selector does not disambiguate them either. Which
 * of the two source folders is compiled is decided by the Minecraft line being built.
 */
@Mixin(ItemFrame.class)
public abstract class ItemFrameMixin {

    @Inject(
        method = "interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bulkmegapack$rotateBack(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (WorldGestures.rotateFrameBack((ItemFrame) (Object) this, player)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
