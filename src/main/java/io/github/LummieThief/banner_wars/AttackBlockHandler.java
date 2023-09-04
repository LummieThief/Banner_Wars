package io.github.LummieThief.banner_wars;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.VerticallyAttachableBlockItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class AttackBlockHandler implements AttackBlockCallback {
    @Override
    public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) {
        if (world.isClient || !world.getRegistryKey().equals(World.OVERWORLD)) {
            return ActionResult.PASS;
        }
        BlockState state = world.getBlockState(pos);
        if (state.getBlock().asItem() instanceof VerticallyAttachableBlockItem) {
            world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
        }
        return ActionResult.PASS;
    }
}
