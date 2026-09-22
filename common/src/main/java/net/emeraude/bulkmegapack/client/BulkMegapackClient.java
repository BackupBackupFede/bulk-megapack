package net.emeraude.bulkmegapack.client;

import net.emeraude.bulkmegapack.BulkMegapack;

/**
 * Loader-agnostic CLIENT entry point — TIER B/C logic goes here.
 *
 * <p>Reached only from the loaders' client entry points, never from {@link BulkMegapack#init()}:
 * a dedicated server must never load this class.
 *
 * <p>Keep as little as possible here. Rendering APIs diverge both between loaders and between
 * Minecraft versions, so client code is what makes a port expensive — anything that can live in
 * {@link BulkMegapack} should.
 *
 * <p>A pure server-side mod (Tier A) deletes this class and the two loader client entry points,
 * and drops "client" from fabric.mod.json + the Dist.CLIENT @Mod class.
 */
public final class BulkMegapackClient {

    private static boolean initialized = false;

    private BulkMegapackClient() {}

    public static synchronized void initClient() {
        if (initialized) return;
        initialized = true;

        BulkMegapack.LOGGER.info("{} client loaded", BulkMegapack.MOD_ID);
    }
}
