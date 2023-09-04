package io.github.LummieThief.banner_wars.mixin;

import com.mojang.datafixers.util.Pair;
import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BannerItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

// If using the item has failed and using the item on the block has failed, then blocks will come here to try one more
// time to be placed, and it's fairly simple to prevent them. The important part of this step in the process is that it's
// when we can confirm than a block has been placed, and it's where we add new claims.
@Mixin(BlockItem.class)
public class BlockItemMixin {
    @Inject(method = "postPlacement", at = @At("HEAD"))
    protected void overridePostPlacement(BlockPos pos, World world, PlayerEntity player, ItemStack stack, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient || !world.getRegistryKey().equals(World.OVERWORLD))
            return;
        String existingBanner = TerritoryManager.GetBannerInChunk(pos);
        if (existingBanner == null && TerritoryManager.IsBanner(stack) && TerritoryManager.HasPattern(stack)) {
            String banner = TerritoryManager.BannerToString(stack);
            TerritoryManager.LOGGER.info("adding chunk " + banner);
            TerritoryManager.AddChunk(banner, pos);
            if (world.getBlockEntity(pos) instanceof BannerBlockEntity entity &&
                stack.getItem() instanceof BannerItem bannerItem) {
                List<Pair<RegistryEntry<BannerPattern>, DyeColor>> patterns =
                        BannerBlockEntity.getPatternsFromNbt(bannerItem.getColor(), BannerBlockEntity.getPatternListNbt(stack));
                if (entity.getCachedState().getBlock() instanceof WallBannerBlock) {
                    TerritoryManager.CreateFireworkEffect(world, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, patterns);
                }
                else {
                    TerritoryManager.CreateFireworkEffect(world, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, patterns);
                }

            }
        }
    }

    @Inject(method = "canPlace", at = @At("RETURN"), cancellable = true)
    private void overrideCanPlace(ItemPlacementContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (context.getWorld().isClient  || !context.getWorld().getRegistryKey().equals(World.OVERWORLD) || context.getPlayer() == null)
            return;
        PlayerEntity player = context.getPlayer();
        Hand hand = context.getHand();
        ItemStack handItem = player.getStackInHand(hand);
        PlayerInventory inventory = player.getInventory();
        if (cir.getReturnValue() && !TerritoryManager.HasPermission(context.getWorld(), context.getPlayer(), context.getBlockPos())) {

            ServerPlayerEntity serverPlayer = (ServerPlayerEntity)player;
            World world = context.getWorld();

            ScreenHandler screenHandler = player.currentScreenHandler;
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
