package net.emeraude.bulkmegapack;

import java.util.function.Predicate;

/**
 * "Is that other mod installed?", asked without naming a loader. Each loader entry point hands in
 * its own lookup; until then nothing is considered present.
 *
 * <p>Used to step aside: when a mod that already owns a gesture is loaded, this pack leaves that
 * gesture alone instead of both hooking the same interaction.
 */
public final class ModsPresent {

    private static volatile Predicate<String> lookup = id -> false;

    private ModsPresent() {}

    public static void use(Predicate<String> loaderLookup) {
        lookup = loaderLookup;
    }

    /** True when at least one of these mod ids is loaded, unless the player asked us not to care. */
    public static boolean any(String... modIds) {
        if (BulkMegapackConfig.ignoreOtherMods()) return false;
        for (String id : modIds) {
            if (lookup.test(id)) return true;
        }
        return false;
    }
}
