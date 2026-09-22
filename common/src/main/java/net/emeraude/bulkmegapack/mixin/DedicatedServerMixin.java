package net.emeraude.bulkmegapack.mixin;

import net.emeraude.bulkmegapack.BulkMegapackSelfTest;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runs {@link BulkMegapackSelfTest} once the dedicated server's levels exist. No-op unless requested. */
@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin {

    @Inject(method = "initServer", at = @At("RETURN"))
    private void bulkmegapack$selfTest(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && BulkMegapackSelfTest.requested()) {
            BulkMegapackSelfTest.run((MinecraftServer) (Object) this);
        }
    }
}
