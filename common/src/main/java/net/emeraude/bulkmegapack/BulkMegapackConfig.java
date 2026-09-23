package net.emeraude.bulkmegapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tiny loader-agnostic config, read once at startup from {@code config/bulkmegapack.json} (relative to
 * the game directory, which is the working directory on both loaders). Dependency-free on purpose:
 * no NeoForge {@code ModConfigSpec}, no config library — so the mod stays a single dependency on
 * every loader.
 *
 * <p>One switch per station, plus the master {@code enabled}. Keys missing from an older file keep
 * their default, and are written back so the file always lists every option.
 */
public final class BulkMegapackConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean enabled = true;
    private static boolean merchant = true;
    private static boolean stonecutter = true;
    private static boolean smithing = true;
    private static boolean loom = true;
    private static boolean cartography = true;
    private static boolean grindstone = true;
    private static boolean grindstoneIncludeHotbar = false;
    private static boolean harvest = true;
    private static boolean composter = true;
    private static boolean cycleBack = true;
    private static boolean doubleDoors = true;
    private static boolean restock = true;
    private static boolean ignoreOtherMods = false;

    private BulkMegapackConfig() {}

    public static void load() {
        Path file = Path.of("config", BulkMegapack.MOD_ID + ".json");
        try {
            JsonObject root = Files.exists(file) ? GSON.fromJson(Files.readString(file), JsonObject.class) : null;
            if (root == null) root = new JsonObject();

            enabled = read(root, "enabled", true);
            merchant = read(root, "merchant", true);
            stonecutter = read(root, "stonecutter", true);
            smithing = read(root, "smithing", true);
            loom = read(root, "loom", true);
            cartography = read(root, "cartography", true);
            grindstone = read(root, "grindstone", true);
            grindstoneIncludeHotbar = read(root, "grindstoneIncludeHotbar", false);
            harvest = read(root, "harvest", true);
            composter = read(root, "composter", true);
            cycleBack = read(root, "cycleBack", true);
            doubleDoors = read(root, "doubleDoors", true);
            restock = read(root, "restock", true);
            ignoreOtherMods = read(root, "ignoreOtherMods", false);

            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root));
        } catch (IOException | RuntimeException e) {
            BulkMegapack.LOGGER.warn("Couldn't read {}, using defaults.", file, e);
        }
    }

    /** Reads a boolean, adding it with its default when absent. */
    private static boolean read(JsonObject root, String key, boolean fallback) {
        if (!root.has(key)) root.addProperty(key, fallback);
        return root.get(key).getAsBoolean();
    }

    /** When false, the mod does nothing and every station behaves exactly like vanilla. */
    public static boolean enabled() {
        return enabled;
    }

    public static boolean merchant() {
        return enabled && merchant;
    }

    public static boolean stonecutter() {
        return enabled && stonecutter;
    }

    public static boolean smithing() {
        return enabled && smithing;
    }

    public static boolean loom() {
        return enabled && loom;
    }

    public static boolean cartography() {
        return enabled && cartography;
    }

    public static boolean grindstone() {
        return enabled && grindstone;
    }

    public static boolean harvest() {
        return enabled && harvest;
    }

    public static boolean composter() {
        return enabled && composter;
    }

    public static boolean cycleBack() {
        return enabled && cycleBack;
    }

    public static boolean doubleDoors() {
        return enabled && doubleDoors;
    }

    public static boolean restock() {
        return enabled && restock;
    }

    /** When true, a gesture stays on even if a mod that already provides it is installed. */
    public static boolean ignoreOtherMods() {
        return ignoreOtherMods;
    }

    /** When false (default), the grindstone never pulls from the hotbar, where the player keeps their own tools. */
    public static boolean grindstoneIncludeHotbar() {
        return grindstoneIncludeHotbar;
    }
}
