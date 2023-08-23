package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.VerticallyAttachableBlockItem;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {
    @Inject(method = "postPlacement", at = @At("HEAD"))
    protected void overridePostPlacement(BlockPos pos, World world, PlayerEntity player, ItemStack stack, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient)
            return;
        String existingBanner = TerritoryManager.GetBannerInChunk(pos);
        if (existingBanner == null && TerritoryManager.isBanner(stack)) {
            String banner = TerritoryManager.BannerToString(stack);
            TerritoryManager.LOGGER.info("adding chunk " + banner);
            TerritoryManager.AddBannerToChunk(banner, pos);
        }
    }

    @Inject(method = "canPlace", at = @At("RETURN"), cancellable = true)
    private void logPlacement(ItemPlacementContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (context.getWorld().isClient)
            return;
        TerritoryManager.LOGGER.info("does this work?");
        if (cir.getReturnValue() && !TerritoryManager.HasPermission(context.getPlayer(), context.getBlockPos())) {
            PlayerEntity player = context.getPlayer();
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity)player;
            World world = context.getWorld();

            ScreenHandler screenHandler = player.currentScreenHandler;
            PlayerInventory inventory = player.getInventory();
            Hand hand = context.getHand();
            ItemStack handItem = player.getStackInHand(hand);
            int slot = hand == Hand.OFF_HAND ? 45 : PlayerInventory.MAIN_SIZE + inventory.selectedSlot;
            serverPlayer.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
                    screenHandler.syncId,
                    screenHandler.getRevision(),
                    slot,
                    handItem));
            BlockPos pos = context.getBlockPos();
            serverPlayer.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, world.getBlockState(pos)));
            world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), Block.NOTIFY_LISTENERS);
            cir.setReturnValue(false);
        }
    }
}
