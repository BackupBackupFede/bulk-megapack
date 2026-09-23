package net.emeraude.bulkmegapack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * The gestures that happen in the world rather than in a menu. All of them are decided on the
 * server, from one seam: the block-use path of {@code ServerPlayerGameMode}.
 *
 * <p>Each one returns a result to stop vanilla, or null to let vanilla carry on.
 */
public final class WorldGestures {

    /** Mods that already own one of these gestures; when one is loaded, we don't hook it too. */
    private static final String[] HARVEST_MODS = {"quark", "rightclickharvest", "harvestwithease", "simpleharvest"};
    private static final String[] DOOR_MODS = {"quark", "doubledoors", "couplings"};

    private static final int FRAME_ROTATIONS = 8;

    private WorldGestures() {}

    public static InteractionResult useItemOn(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !(level instanceof ServerLevel serverLevel)) return null;

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof CropBlock crop) return harvest(serverLevel, player, pos, state, crop);
        if (state.getBlock() instanceof ComposterBlock) return compostStack(serverLevel, player, pos, state, stack);
        if (state.getBlock() instanceof RepeaterBlock) return cycleBack(serverLevel, player, pos, state, stack);
        if (state.getBlock() instanceof DoorBlock door) openPair(serverLevel, player, pos, state, door);
        return null;
    }

    /**
     * Right-click a grown crop: it drops what breaking it would drop, minus one seed, and goes
     * back to age 0. The seed kept is the one item in the drops that plants this very crop, so no
     * seed is created and none is lost.
     */
    private static InteractionResult harvest(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, CropBlock crop) {
        if (!BulkMegapackConfig.harvest() || !crop.isMaxAge(state)) return null;
        if (ModsPresent.any(HARVEST_MODS)) return null;

        List<ItemStack> drops = Block.getDrops(state, level, pos, null);
        boolean seedKept = false;
        for (ItemStack drop : drops) {
            if (!seedKept && drop.getItem() instanceof BlockItem item && item.getBlock() == crop) {
                drop.shrink(1);
                seedKept = true;
            }
            if (!drop.isEmpty()) Block.popResource(level, pos, drop);
        }
        if (!seedKept) return null; // nothing to replant with: leave it to vanilla

        level.setBlock(pos, crop.getStateForAge(0), 3);
        return InteractionResult.SUCCESS;
    }

    /** Sneak + right-click a composter with a compostable stack: the whole stack goes in. */
    private static InteractionResult compostStack(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, ItemStack stack) {
        if (!BulkMegapackConfig.composter() || stack.isEmpty() || !player.isSecondaryUseActive()) return null;
        if (ModsPresent.any("quark")) return null;

        BlockState current = state;
        int before = stack.getCount();
        // Each pass is vanilla's own insert: same 30-85% chances, same shrink, same particles.
        while (!stack.isEmpty() && current.getValue(ComposterBlock.LEVEL) < 7) {
            BlockState next = ComposterBlock.insertItem(player, current, level, stack, pos);
            if (next == current && stack.getCount() == before) return null; // not compostable at all
            current = next;
            before = stack.getCount();
        }
        return InteractionResult.SUCCESS;
    }

    /** Sneak + right-click a repeater with an empty hand: the delay goes back one notch. */
    private static InteractionResult cycleBack(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, ItemStack stack) {
        if (!BulkMegapackConfig.cycleBack() || !stack.isEmpty() || !player.isSecondaryUseActive()) return null;

        int delay = state.getValue(RepeaterBlock.DELAY);
        int previous = delay <= 1 ? 4 : delay - 1;
        level.setBlock(pos, state.setValue(RepeaterBlock.DELAY, previous), 3);
        return InteractionResult.SUCCESS;
    }

    /**
     * Opening one half of a double door opens the other half with it. Vanilla still handles the
     * door that was clicked, so this only moves its twin and returns nothing.
     */
    private static void openPair(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, DoorBlock door) {
        if (!BulkMegapackConfig.doubleDoors() || !door.type().canOpenByHand()) return;
        if (ModsPresent.any(DOOR_MODS)) return;

        Direction facing = state.getValue(DoorBlock.FACING);
        Direction toTwin = state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT ? facing.getCounterClockWise() : facing.getClockWise();
        BlockPos twinPos = pos.relative(toTwin);
        BlockState twin = level.getBlockState(twinPos);

        boolean sameDoor = twin.getBlock() == door
            && twin.getValue(DoorBlock.FACING) == facing
            && twin.getValue(DoorBlock.HINGE) != state.getValue(DoorBlock.HINGE)
            && twin.getValue(DoorBlock.HALF) == state.getValue(DoorBlock.HALF);
        if (!sameDoor) return;

        boolean willOpen = !state.getValue(DoorBlock.OPEN);
        if (twin.getValue(DoorBlock.OPEN) != willOpen) {
            door.setOpen(player, level, twin, twinPos, willOpen);
        }
    }

    /**
     * Refills the main hand from the inventory when the stack in it runs out — the block you were
     * placing, the food you were eating. Called once per player tick with what the hand held last
     * tick; only the first 36 slots (hotbar + inventory) are searched, never armour or offhand.
     */
    public static void restockHand(ServerPlayer player, ItemStack lastHeld) {
        if (!BulkMegapackConfig.restock() || lastHeld.isEmpty()) return;
        if (!player.getMainHandItem().isEmpty()) return;
        if (player.containerMenu != player.inventoryMenu) return; // a GUI is open: not our business
        if (ModsPresent.any("quark", "stackrefill", "inventoryprofilesnext", "tweakeroo")) return;

        for (int slot = 0; slot < 36; slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (candidate.isEmpty() || !ItemStack.isSameItemSameComponents(candidate, lastHeld)) continue;

            player.getInventory().setItem(slot, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.MAIN_HAND, candidate);
            return;
        }
    }

    /**
     * Sneak + right-click on a filled item frame turns its item back one step. Returns true when
     * it did, which tells the caller to stop vanilla from turning it forward.
     */
    public static boolean rotateFrameBack(ItemFrame frame, Player player) {
        if (!BulkMegapackConfig.cycleBack() || player.level().isClientSide()) return false;
        if (!player.isSecondaryUseActive() || frame.getItem().isEmpty()) return false;
        if (ModsPresent.any("quark")) return false;

        frame.setRotation((frame.getRotation() + FRAME_ROTATIONS - 1) % FRAME_ROTATIONS);
        return true;
    }

    /** The stack a player holds, copied so a later change doesn't rewrite what we remembered. */
    public static ItemStack remember(Player player) {
        ItemStack held = player.getMainHandItem();
        return held.isEmpty() ? ItemStack.EMPTY : held.copyWithCount(1);
    }
}
