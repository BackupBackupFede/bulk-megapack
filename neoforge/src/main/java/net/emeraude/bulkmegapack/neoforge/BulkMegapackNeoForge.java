package net.emeraude.bulkmegapack.neoforge;

import net.emeraude.bulkmegapack.BulkMegapack;
import net.emeraude.bulkmegapack.ModsPresent;
import net.neoforged.fml.ModList;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** NeoForge entry point (both sides). All logic is loader-agnostic in {@link BulkMegapack}. */
@Mod(BulkMegapack.MOD_ID)
public final class BulkMegapackNeoForge {

    public BulkMegapackNeoForge(IEventBus modBus, ModContainer container) {
        ModsPresent.use(ModList.get()::isLoaded);
        BulkMegapack.init();
    }
}
