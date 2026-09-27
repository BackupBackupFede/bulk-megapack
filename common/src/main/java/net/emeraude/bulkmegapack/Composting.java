package net.emeraude.bulkmegapack;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The one gesture of this pack that happens on a block rather than in a menu — and it is the same
 * idea: one action puts a whole stack through. Sneak + right-click a composter and the stack in
 * hand goes in, item by item, until the composter is full or the stack is gone.
 *
 * <p>Each pass is vanilla's own insert, so the 30-85% chances per item, the shrink and the
 * particles are untouched. Returns null when the click is none of our business.
 */
public final class Composting {

    /** Mods that already bulk-compost; when one is loaded, this steps aside. */
    private static final String[] COMPOSTER_MODS = {"bulkcompost", "quark"};

    private Composting() {}

    public static InteractionResult useItemOn(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !(level instanceof ServerLevel serverLevel)) return null;
        if (!BulkMegapackConfig.composter() || stack.isEmpty() || !player.isSecondaryUseActive()) return null;

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ComposterBlock)) return null;
        if (ModsPresent.any(COMPOSTER_MODS)) return null;

        BlockState current = state;
        int before = stack.getCount();
        while (!stack.isEmpty() && current.getValue(ComposterBlock.LEVEL) < 7) {
            BlockState next = ComposterBlock.insertItem(player, current, serverLevel, stack, pos);
            if (next == current && stack.getCount() == before) return null; // not compostable at all
            current = next;
            before = stack.getCount();
        }
        return InteractionResult.SUCCESS;
    }
}
