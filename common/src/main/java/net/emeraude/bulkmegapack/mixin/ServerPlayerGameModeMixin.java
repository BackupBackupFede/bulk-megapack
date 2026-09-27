package net.emeraude.bulkmegapack.mixin;

import net.emeraude.bulkmegapack.Composting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The seam for the composter gesture. Injected at the head of the server's block-use path, before
 * vanilla decides what the click means: a sneaking player holding something never reaches the
 * block's own code, and that is exactly the click this gesture needs.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void bulkmegapack$composting(
            ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult handled = Composting.useItemOn(player, level, stack, hand, hit);
        if (handled != null) cir.setReturnValue(handled);
    }
}
