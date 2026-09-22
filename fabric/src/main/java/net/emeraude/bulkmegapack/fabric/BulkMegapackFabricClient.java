package net.emeraude.bulkmegapack.fabric;

import net.emeraude.bulkmegapack.client.BulkMegapackClient;
import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric CLIENT entry point (Tier B/C). Never loaded on a dedicated server.
 * Delete this class and the "client" entrypoint in fabric.mod.json for a Tier A mod.
 */
public final class BulkMegapackFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BulkMegapackClient.initClient();
    }
}
