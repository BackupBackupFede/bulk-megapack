package net.emeraude.bulkmegapack;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Method;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Development check, off unless the environment variable {@code BULKMEGAPACK_SELFTEST=true} is set
 * (Gradle's runServer forks a JVM that inherits the environment, on both loaders).
 *
 * <p>Once the dedicated server is up, it builds a player with no connection, opens each station
 * menu on it, performs the same shift-click vanilla performs (quickMoveStack, re-run while the
 * result slot refills with the same item — the loop of AbstractContainerMenu.doClick), and checks
 * the inventory afterwards. Then it stops the server. "It compiles" proves nothing about a mixin;
 * this proves the behaviour.
 */
public final class BulkMegapackSelfTest {

    private BulkMegapackSelfTest() {}

    public static boolean requested() {
        return "true".equalsIgnoreCase(System.getenv("BULKMEGAPACK_SELFTEST"));
    }

    public static void run(MinecraftServer server) {
        List<String> failures = new ArrayList<>();
        try {
            ServerLevel level = server.overworld();
            check(failures, "merchant", merchant(server, level));
            check(failures, "cartography", cartography(server, level));
            check(failures, "stonecutter", stonecutter(server, level));
            check(failures, "smithing", smithing(server, level));
            check(failures, "loom", loom(server, level));
            check(failures, "grindstone", grindstone(server, level));
            check(failures, "harvest", harvest(server, level));
            check(failures, "composter", composter(server, level));
            check(failures, "repeater", repeater(server, level));
            check(failures, "doubleDoors", doubleDoors(server, level));
            check(failures, "itemFrame", itemFrame(server, level));
            check(failures, "restock", restock(server, level));
        } catch (Throwable t) {
            BulkMegapack.LOGGER.error("[SELFTEST] crashed", t);
            failures.add("crash: " + t);
        }
        BulkMegapack.LOGGER.info("[SELFTEST] RESULT {}", failures.isEmpty() ? "PASS" : "FAIL " + failures);
        server.halt(false);
    }

    private static void check(List<String> failures, String name, String error) {
        if (error == null) {
            BulkMegapack.LOGGER.info("[SELFTEST] {} OK", name);
        } else {
            BulkMegapack.LOGGER.error("[SELFTEST] {} FAILED: {}", name, error);
            failures.add(name + ": " + error);
        }
    }

    // --- stations -------------------------------------------------------------------------------

    /** 1 emerald -> 1 bread, 1 emerald in the payment slot and 19 in the inventory: 20 trades. */
    private static String merchant(MinecraftServer server, ServerLevel level) {
        ServerPlayer player = player(server, level);
        Inventory inv = player.getInventory();
        inv.setItem(9, stack("minecraft:emerald", 19));

        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(stack("minecraft:emerald", 1).getItem(), 1), stack("minecraft:bread", 1), 100, 1, 0.05F));
        // Reports itself client-side only so the menu skips its trade sound, which casts the
        // merchant to an Entity. The refill checks the player's side, not the merchant's.
        ClientSideMerchant merchant = new ClientSideMerchant(player) {
            @Override
            public boolean isClientSide() {
                return true;
            }
        };
        merchant.overrideOffers(offers);

        MerchantMenu menu = new MerchantMenu(1, inv, merchant);
        menu.setSelectionHint(0);
        menu.slots.get(0).set(stack("minecraft:emerald", 1));
        if (!menu.slots.get(2).hasItem()) return "no result with 1 emerald in the payment slot";

        shiftClick(menu, player, 2);
        int bread = count(inv, "minecraft:bread");
        int emeralds = count(inv, "minecraft:emerald") + menu.slots.get(0).getItem().getCount();
        return bread == 20 && emeralds == 0 ? null : bread + " bread / " + emeralds + " emeralds left (expected 20 / 0)";
    }

    /** 1 map + 1 empty map in the slots, 4 more empty maps in the inventory: 6 identical maps. */
    private static String cartography(MinecraftServer server, ServerLevel level) {
        ServerPlayer player = player(server, level);
        Inventory inv = player.getInventory();
        ItemStack map = MapItem.create(level, 0, 0, (byte) 0, false, false);
        inv.setItem(9, stack("minecraft:map", 4));

        CartographyTableMenu menu = new CartographyTableMenu(1, inv, ContainerLevelAccess.create(level, player.blockPosition()));
        menu.slots.get(0).set(map.copy());
        menu.slots.get(1).set(stack("minecraft:map", 1));
        if (!menu.slots.get(2).hasItem()) return "no result with a map + an empty map";

        shiftClick(menu, player, 2);
        int empty = count(inv, "minecraft:map") + menu.slots.get(1).getItem().getCount();
        int clones = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (ItemStack.isSameItemSameComponents(inv.getItem(i), map)) clones += inv.getItem(i).getCount();
        }
        clones += menu.slots.get(0).getItem().getCount();
        return clones == 6 && empty == 0 ? null : clones + " maps / " + empty + " empty maps left (expected 6 / 0)";
    }

    /** 64 stone in the input + 128 in the inventory, first recipe selected: all 192 must be cut. */
    private static String stonecutter(MinecraftServer server, ServerLevel level) {
        ServerPlayer player = player(server, level);
        Inventory inv = player.getInventory();
        inv.setItem(9, stack("minecraft:stone", 64));
        inv.setItem(10, stack("minecraft:stone", 64));
        StonecutterMenu menu = new StonecutterMenu(1, inv, ContainerLevelAccess.create(level, player.blockPosition()));
        menu.slots.get(0).set(stack("minecraft:stone", 64));
        menu.clickMenuButton(player, 0);
        if (!menu.slots.get(1).hasItem()) return "no result after selecting recipe 0";

        shiftClick(menu, player, 1);
        int left = count(inv, "minecraft:stone") + menu.slots.get(0).getItem().getCount();
        return left == 0 ? null : left + " stone left uncut (expected 0)";
    }

    /** 3 netherite upgrades in a row from one shift-click: 1 of each in the slots, 2 more in the inventory. */
    private static String smithing(MinecraftServer server, ServerLevel level) {
        ServerPlayer player = player(server, level);
        Inventory inv = player.getInventory();
        inv.setItem(9, stack("minecraft:netherite_upgrade_smithing_template", 2));
        inv.setItem(10, stack("minecraft:diamond_sword", 1));
        inv.setItem(11, stack("minecraft:diamond_sword", 1));
        inv.setItem(12, stack("minecraft:netherite_ingot", 2));
        SmithingMenu menu = new SmithingMenu(1, inv, ContainerLevelAccess.create(level, player.blockPosition()));
        menu.slots.get(0).set(stack("minecraft:netherite_upgrade_smithing_template", 1));
        menu.slots.get(1).set(stack("minecraft:diamond_sword", 1));
        menu.slots.get(2).set(stack("minecraft:netherite_ingot", 1));
        if (!menu.slots.get(3).hasItem()) return "no result with template + sword + ingot";

        shiftClick(menu, player, menu.getResultSlot());
        int made = count(inv, "minecraft:netherite_sword");
        return made == 3 ? null : made + " netherite swords (expected 3)";
    }

    /** 1 banner + 1 dye in the slots, 3 more of each in the inventory, pattern 0: 4 patterned banners. */
    private static String loom(MinecraftServer server, ServerLevel level) {
        ServerPlayer player = player(server, level);
        Inventory inv = player.getInventory();
        inv.setItem(9, stack("minecraft:white_banner", 3));
        inv.setItem(10, stack("minecraft:red_dye", 3));
        LoomMenu menu = new LoomMenu(1, inv, ContainerLevelAccess.create(level, player.blockPosition()));
        menu.slots.get(0).set(stack("minecraft:white_banner", 1));
        menu.slots.get(1).set(stack("minecraft:red_dye", 1));
        menu.clickMenuButton(player, 0);
        if (!menu.slots.get(3).hasItem()) return "no result after selecting pattern 0";

        shiftClick(menu, player, 3);
        int plainLeft = 0;
        int patterned = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!id(s.getItem()).equals("minecraft:white_banner")) continue;
            if (ItemStack.isSameItemSameComponents(s, stack("minecraft:white_banner", 1))) plainLeft += s.getCount();
            else patterned += s.getCount();
        }
        plainLeft += menu.slots.get(0).getItem().getCount();
        return patterned == 4 && plainLeft == 0 ? null : patterned + " patterned / " + plainLeft + " plain left (expected 4 / 0)";
    }

    /**
     * Mixed loot in the main inventory must all be stripped from one shift-click, but never the
     * enchanted pickaxe in the hotbar, never the renamed sword, never the curse-only helmet.
     */
    private static String grindstone(MinecraftServer server, ServerLevel level) {
        ServerPlayer player = player(server, level);
        Inventory inv = player.getInventory();
        Holder<Enchantment> sharpness = enchantment(level, Enchantments.SHARPNESS);
        Holder<Enchantment> power = enchantment(level, Enchantments.POWER);
        Holder<Enchantment> curse = enchantment(level, Enchantments.BINDING_CURSE);

        inv.setItem(0, enchanted("minecraft:diamond_pickaxe", sharpness));      // hotbar: must stay
        inv.setItem(9, enchanted("minecraft:bow", power));
        inv.setItem(10, enchanted("minecraft:iron_sword", sharpness));
        inv.setItem(11, enchanted("minecraft:golden_sword", sharpness));
        ItemStack renamed = enchanted("minecraft:stone_sword", sharpness);
        renamed.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Keep me"));
        inv.setItem(12, renamed);                                                // renamed: must stay
        inv.setItem(13, enchanted("minecraft:iron_helmet", curse));             // curse only: must stay

        GrindstoneMenu menu = new GrindstoneMenu(1, inv, ContainerLevelAccess.create(level, player.blockPosition()));
        menu.slots.get(0).set(enchanted("minecraft:diamond_sword", sharpness));
        if (!menu.slots.get(2).hasItem()) return "no result with an enchanted sword";

        shiftClick(menu, player, 2);
        List<String> wrong = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            String item = id(s.getItem());
            boolean mustKeep = i == 0 || s.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME) || item.equals("minecraft:iron_helmet");
            boolean enchantedNow = EnchantmentHelper.hasAnyEnchantments(s);
            if (mustKeep != enchantedNow) wrong.add(item + (enchantedNow ? " still enchanted" : " wrongly stripped"));
        }
        if (menu.slots.get(0).hasItem() || menu.slots.get(1).hasItem()) wrong.add("items left in the grindstone");
        if (count(inv, "minecraft:diamond_sword") != 1) wrong.add("diamond sword not returned");
        return wrong.isEmpty() ? null : String.join(", ", wrong);
    }

    // --- world gestures -------------------------------------------------------------------------

    /** A grown wheat crop right-clicked: back to age 0, wheat on the ground, one seed kept back. */
    private static String harvest(MinecraftServer server, ServerLevel level) {
        ServerPlayer player = player(server, level);
        BlockPos pos = freeSpot(level, 0);
        level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
        CropBlock wheat = (CropBlock) Blocks.WHEAT;
        level.setBlockAndUpdate(pos, wheat.getStateForAge(wheat.getMaxAge()));

        useOn(player, level, ItemStack.EMPTY, pos);

        BlockState after = level.getBlockState(pos);
        if (!(after.getBlock() instanceof CropBlock crop)) return "the crop is gone (block is " + after.getBlock() + ")";
        if (crop.getAge(after) != 0) return "the crop was not reset (age " + crop.getAge(after) + ")";
        return dropped(level, pos, "minecraft:wheat") >= 1 ? null : "no wheat on the ground";
    }

    /** A stack of seeds sneak-right-clicked into a composter: the whole stack goes in. */
    private static String composter(MinecraftServer server, ServerLevel level) {
        ServerPlayer player = player(server, level);
        player.setShiftKeyDown(true);
        BlockPos pos = freeSpot(level, 4);
        level.setBlockAndUpdate(pos, Blocks.COMPOSTER.defaultBlockState());

        ItemStack seeds = stack("minecraft:wheat_seeds", 64);
        useOn(player, level, seeds, pos);

        int fill = level.getBlockState(pos).getValue(ComposterBlock.LEVEL);
        if (seeds.getCount() == 64) return "nothing was composted";
        return seeds.isEmpty() || fill >= 7 ? null : seeds.getCount() + " seeds left with the composter at " + fill;
    }

    /** A repeater sneak-right-clicked with an empty hand: the delay steps backwards, 1 -> 4. */
    private static String repeater(MinecraftServer server, ServerLevel level) {
        ServerPlayer player = player(server, level);
        player.setShiftKeyDown(true);
        BlockPos pos = freeSpot(level, 8);
        level.setBlockAndUpdate(pos, Blocks.REPEATER.defaultBlockState());

        useOn(player, level, ItemStack.EMPTY, pos);
        int delay = level.getBlockState(pos).getValue(RepeaterBlock.DELAY);
        return delay == 4 ? null : "delay is " + delay + " (expected 4)";
    }

    /** One half of a double door opened: the other half follows. */
    private static String doubleDoors(MinecraftServer server, ServerLevel level) {
        ServerPlayer player = player(server, level);
        BlockPos left = freeSpot(level, 12);
        BlockPos right = left.east();
        BlockState base = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        level.setBlock(left, base.setValue(DoorBlock.HINGE, DoorHingeSide.LEFT), 2);
        level.setBlock(left.above(), base.setValue(DoorBlock.HINGE, DoorHingeSide.LEFT).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);
        level.setBlock(right, base.setValue(DoorBlock.HINGE, DoorHingeSide.RIGHT), 2);
        level.setBlock(right.above(), base.setValue(DoorBlock.HINGE, DoorHingeSide.RIGHT).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);

        useOn(player, level, ItemStack.EMPTY, left);

        boolean leftOpen = level.getBlockState(left).getValue(DoorBlock.OPEN);
        boolean rightOpen = level.getBlockState(right).getValue(DoorBlock.OPEN);
        return leftOpen && rightOpen ? null : "clicked half " + leftOpen + ", other half " + rightOpen + " (expected both open)";
    }

    /**
     * A filled item frame sneak-right-clicked: the item turns back one step. The interact method
     * gained a parameter in 26.x, so this dev-only check calls it by reflection rather than
     * carrying two versions of the test.
     */
    private static String itemFrame(MinecraftServer server, ServerLevel level) throws Exception {
        ServerPlayer player = player(server, level);
        player.setShiftKeyDown(true);
        BlockPos pos = freeSpot(level, 16);
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());

        ItemFrame frame = new ItemFrame(level, pos.above(), Direction.UP);
        frame.setItem(stack("minecraft:diamond", 1));
        frame.setRotation(3);
        level.addFreshEntity(frame);

        Method interact = null;
        for (Method m : ItemFrame.class.getMethods()) {
            if (m.getName().equals("interact") && m.getParameterCount() >= 2 && m.getParameterTypes()[0] == Player.class) interact = m;
        }
        if (interact == null) return "no interact method found on ItemFrame";
        Object[] args = new Object[interact.getParameterCount()];
        args[0] = player;
        args[1] = InteractionHand.MAIN_HAND;
        if (args.length > 2) args[2] = Vec3.atCenterOf(pos);
        interact.invoke(frame, args);

        return frame.getRotation() == 2 ? null : "rotation is " + frame.getRotation() + " (expected 2)";
    }

    /** The hand runs out of stone: the next stack in the inventory takes its place on the next tick. */
    private static String restock(MinecraftServer server, ServerLevel level) {
        ServerPlayer player = player(server, level);
        Inventory inv = player.getInventory();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack("minecraft:stone", 1));
        inv.setItem(9, stack("minecraft:stone", 64));

        ItemStack remembered = WorldGestures.remember(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); // the last block was placed
        WorldGestures.restockHand(player, remembered);

        ItemStack hand = player.getMainHandItem();
        if (hand.isEmpty()) return "the hand stayed empty";
        return hand.getCount() == 64 && count(inv, "minecraft:stone") == 64
            ? null
            : hand.getCount() + " in hand, " + count(inv, "minecraft:stone") + " in total (expected 64 / 64)";
    }

    // --- helpers --------------------------------------------------------------------------------

    /** Runs the server's own block-use path, which is where the world gestures hook in. */
    private static void useOn(ServerPlayer player, ServerLevel level, ItemStack stack, BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit);
    }

    /** Empty air well above the terrain, so nothing already there takes part in the test. */
    private static BlockPos freeSpot(ServerLevel level, int offsetX) {
        BlockPos pos = new BlockPos(offsetX, 180, 0);
        for (int x = -1; x <= 2; x++) {
            for (int y = -1; y <= 2; y++) {
                level.setBlock(pos.offset(x, y, 0), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        return pos;
    }

    private static int dropped(ServerLevel level, BlockPos pos, String id) {
        int n = 0;
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3.0))) {
            if (id(entity.getItem().getItem()).equals(id)) n += entity.getItem().getCount();
        }
        return n;
    }


    /** What AbstractContainerMenu.doClick does for a QUICK_MOVE on a slot. */
    private static void shiftClick(AbstractContainerMenu menu, ServerPlayer player, int slot) {
        ItemStack moved = menu.quickMoveStack(player, slot);
        int guard = 0;
        while (!moved.isEmpty() && ItemStack.isSameItem(menu.slots.get(slot).getItem(), moved) && guard++ < 1000) {
            moved = menu.quickMoveStack(player, slot);
        }
    }

    /**
     * A player with a connection that has no channel: vanilla sends packets on the way (recipe
     * book unlocks when a recipe is used for the first time), and an unopened Connection just
     * queues them.
     */
    private static ServerPlayer player(MinecraftServer server, ServerLevel level) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "selftest");
        ServerPlayer player = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player, CommonListenerCookie.createInitial(profile, false));
        player.getInventory().clearContent();
        return player;
    }

    private static Holder<Enchantment> enchantment(ServerLevel level, ResourceKey<Enchantment> key) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static ItemStack enchanted(String id, Holder<Enchantment> enchantment) {
        ItemStack s = stack(id, 1);
        s.enchant(enchantment, 1);
        return s;
    }

    /** Items resolved by id, not by Items.* fields: several were renamed or regrouped in 26.x. */
    private static ItemStack stack(String id, int count) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (id(item).equals(id)) return new ItemStack(item, count);
        }
        throw new IllegalArgumentException("unknown item " + id);
    }

    private static String id(Item item) {
        return String.valueOf(BuiltInRegistries.ITEM.getKey(item));
    }

    private static int count(Inventory inv, String id) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && id(s.getItem()).equals(id)) n += s.getCount();
        }
        return n;
    }
}
