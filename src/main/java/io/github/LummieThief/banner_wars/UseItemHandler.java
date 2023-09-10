package io.github.LummieThief.banner_wars;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Waterloggable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

// For some reason, buckets don't get caught by any of the other 3 fail safes, so we need a 4th here to specifically
// prevent bucket items and boats.
public class UseItemHandler implements UseItemCallback {
    @Override
    public TypedActionResult<ItemStack> interact(PlayerEntity player, World world, Hand hand) {
        ItemStack handItem = player.getStackInHand(hand);
        if (!(player instanceof ServerPlayerEntity serverPlayer) || world.isClient || !world.getRegistryKey().equals(World.OVERWORLD)) {
            return TypedActionResult.pass(handItem);
        }
        Item item = handItem.getItem();

        // We only want to block buckets and boats here
        if (!(item instanceof FluidModificationItem || item instanceof BoatItem)) {
            return TypedActionResult.pass(handItem);
        }

        boolean includeFluid = (item instanceof BucketItem bucket && ((IBucketItemMixin) bucket).getFluid().equals(Fluids.EMPTY)) || item instanceof BoatItem;

        BlockHitResult hit = TerritoryManager.GetPlayerHitResult(serverPlayer, includeFluid);
        if (hit == null) {
            return TypedActionResult.pass(handItem);
        }

        BlockPos realPos = hit.getBlockPos();

        // unless we clicked a waterloggable block with a bucket, we need to use the face of the block, not the raw position.
        if (item instanceof BoatItem || !(world.getBlockState(hit.getBlockPos()).getBlock() instanceof Waterloggable))
            realPos = realPos.add(hit.getSide().getVector());

        if (TerritoryManager.HasPermission(world, serverPlayer, realPos)) {
            return TypedActionResult.pass(handItem);
        }
        else {
            ScreenHandler screenHandler = player.currentScreenHandler;
            PlayerInventory inventory = player.getInventory();
            int slot = hand == Hand.OFF_HAND ? 45 : PlayerInventory.MAIN_SIZE + inventory.selectedSlot;
            serverPlayer.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
                    screenHandler.syncId,
                    screenHandler.getRevision(),
                    slot,
                    handItem));
            return TypedActionResult.fail(handItem);
        }
    }
}
