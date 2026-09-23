package net.emeraude.bulkmegapack.fabric;

import net.emeraude.bulkmegapack.BulkMegapack;
import net.emeraude.bulkmegapack.ModsPresent;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.ModInitializer;

/** Fabric entry point (both sides). All logic is loader-agnostic in {@link BulkMegapack}. */
public final class BulkMegapackFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ModsPresent.use(FabricLoader.getInstance()::isModLoaded);
        BulkMegapack.init();
    }
}
