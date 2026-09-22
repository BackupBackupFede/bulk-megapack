package net.emeraude.bulkmegapack.neoforge;

import net.emeraude.bulkmegapack.BulkMegapack;
import net.emeraude.bulkmegapack.client.BulkMegapackClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge CLIENT entry point (Tier B/C). A second @Mod class on the same mod id, restricted to
 * Dist.CLIENT, so it is never constructed on a dedicated server.
 * Delete this class for a Tier A mod.
 */
@Mod(value = BulkMegapack.MOD_ID, dist = Dist.CLIENT)
public final class BulkMegapackNeoForgeClient {

    public BulkMegapackNeoForgeClient(IEventBus modBus, ModContainer container) {
        BulkMegapackClient.initClient();
    }
}
