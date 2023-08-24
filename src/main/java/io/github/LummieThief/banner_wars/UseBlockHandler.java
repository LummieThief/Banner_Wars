package io.github.LummieThief.banner_wars;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.VerticallyAttachableBlockItem;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class UseBlockHandler implements UseBlockCallback {
    @Override
    public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (!(player instanceof ServerPlayerEntity) || world.isClient) {
            return ActionResult.PASS;
        }
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        if (!TerritoryManager.HasPermission(player, hitResult.getBlockPos())) {
            BlockEntity be = world.getBlockEntity(hitResult.getBlockPos());
            if (!(be instanceof LockableContainerBlockEntity)) {
                ScreenHandler screenHandler = player.currentScreenHandler;
                PlayerInventory inventory = player.getInventory();
                int slot = hand == Hand.OFF_HAND ? 45 : PlayerInventory.MAIN_SIZE + inventory.selectedSlot;
                serverPlayer.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
                        screenHandler.syncId,
                        screenHandler.getRevision(),
                        slot,
                        player.getStackInHand(hand)));
                BlockPos pos = hitResult.getBlockPos();
                serverPlayer.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, world.getBlockState(pos)));
                world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), Block.NOTIFY_LISTENERS);
                return ActionResult.FAIL;
            }
        }
        return ActionResult.PASS;

        /*
        ItemStack handItem = player.getStackInHand(hand);
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity)player;
        BlockPos blockPos = hitResult.getBlockPos();
        BlockPos tangentPos = blockPos.add(hitResult.getSide().getVector());

        if (handItem.getItem() instanceof VerticallyAttachableBlockItem && !TerritoryManager.HasPermission(player, tangentPos)) {
            ScreenHandler screenHandler = player.currentScreenHandler;
            PlayerInventory inventory = player.getInventory();
            int slot = hand == Hand.OFF_HAND ? 45 : PlayerInventory.MAIN_SIZE + inventory.selectedSlot;
            serverPlayer.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
                    screenHandler.syncId,
                    screenHandler.getRevision(),
                    slot,
                    handItem));
            BlockPos pos = hitResult.getBlockPos();
            serverPlayer.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, world.getBlockState(pos)));
            world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), Block.NOTIFY_LISTENERS);
            return ActionResult.FAIL;
        }


        return ActionResult.PASS;
         */
/*        if () {
            return ActionResult.PASS;
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
            BlockPos pos = hitResult.getBlockPos();
            serverPlayer.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, world.getBlockState(pos)));
            world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), Block.NOTIFY_LISTENERS);
            return ActionResult.FAIL;
        }*/
    }
}
