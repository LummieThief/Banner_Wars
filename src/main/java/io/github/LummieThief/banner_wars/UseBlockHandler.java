package io.github.LummieThief.banner_wars;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.VerticallyAttachableBlockItem;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

// After an item fails the useOnBlock check (or if the player is empty-handed), it will come here to check if the block
// itself can be used instead. This is a fairly simple check since we only need to check if the block is in a claimed
// chunk, but there is a notable edge case which allows players to open containers.
public class UseBlockHandler implements UseBlockCallback {
    @Override
    public ActionResult interact(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (!(player instanceof ServerPlayerEntity serverPlayer) || world.isClient || !world.getRegistryKey().equals(World.OVERWORLD)) {
            return ActionResult.PASS;
        }

        // if the player doesn't have permission
        if (!TerritoryManager.HasPermission(world, player, hitResult.getBlockPos())) {
            BlockEntity be = world.getBlockEntity(hitResult.getBlockPos());
            // check if they are trying to open a container, and if they are then let it pass
            if (be != null && be.getCachedState().getBlock().asItem() instanceof VerticallyAttachableBlockItem) {
                world.updateListeners(hitResult.getBlockPos(), be.getCachedState(), be.getCachedState(), Block.NOTIFY_LISTENERS);
            }
            else if (be instanceof LockableContainerBlockEntity && !player.isSneaking()) {
                return ActionResult.PASS;
            }
            // otherwise, they truly don't have permission
            else {
                ScreenHandler screenHandler = player.currentScreenHandler;
                PlayerInventory inventory = player.getInventory();
                int slot = hand == Hand.OFF_HAND ? 45 : PlayerInventory.MAIN_SIZE + inventory.selectedSlot;
                serverPlayer.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
                        screenHandler.syncId,
                        screenHandler.getRevision(),
                        slot,
                        player.getStackInHand(hand)));
                //TODO: This is where placing a block on the perimeter fails.

                return ActionResult.FAIL;
            }
        }
        return ActionResult.PASS;
    }
}
