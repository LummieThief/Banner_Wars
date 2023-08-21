package io.github.LummieThief.banner_wars;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class UseItemHandler implements UseItemCallback {
    @Override
    public TypedActionResult<ItemStack> interact(PlayerEntity player, World world, Hand hand) {
        ItemStack handItem = player.getStackInHand(hand);
        if (!(player instanceof ServerPlayerEntity) || world.isClient) {
            return TypedActionResult.pass(handItem);
        }

        // We don't want to block functional items, we only want to block items that place blocks (buckets are NONE for some reason)
        if (!(handItem.getUseAction() == UseAction.BLOCK || handItem.getUseAction() == UseAction.NONE)) {
            return TypedActionResult.pass(handItem);
        }

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;


        BlockHitResult hit = TerritoryManager.GetPlayerHitResult(world, serverPlayer, false);
        if (hit == null) {
            return TypedActionResult.pass(handItem);
        }

        BlockPos realPos = hit.getBlockPos().add(hit.getSide().getVector());
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
