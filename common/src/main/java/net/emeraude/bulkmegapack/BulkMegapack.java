package net.emeraude.bulkmegapack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader-agnostic entry point.
 *
 * <p>Every gesture this pack adds is decided server-side, so all of it lives here or below.
 * Nothing in this package may touch client-only Minecraft classes, or a dedicated server crashes
 * on class load.
 */
public final class BulkMegapack {

    public static final String MOD_ID = "bulkmegapack";
    public static final Logger LOGGER = LoggerFactory.getLogger("Bulk Megapack");

    private static boolean initialized = false;

    private BulkMegapack() {}

    /** Called once from each loader's entry point, as early as possible. */
    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        BulkMegapackConfig.load();
        LOGGER.info("Bulk Megapack loaded — one switch per station in config/{}.json", MOD_ID);
    }
}
