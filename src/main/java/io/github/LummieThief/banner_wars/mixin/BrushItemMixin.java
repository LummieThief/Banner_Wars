package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.*;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.BrushItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrushItem.class)
public abstract class BrushItemMixin {
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    public void paintBanner(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        World world = context.getWorld();
        if (world.isClient || !world.getRegistryKey().equals(World.OVERWORLD))
            return;

        BlockPos pos = context.getBlockPos();
        BlockEntity entity = world.getBlockEntity(pos);
        if (!(entity instanceof BannerBlockEntity bannerEntity && context.getPlayer() instanceof ServerPlayerEntity player)) {
            return;
        }

        ItemStack headStack = player.getEquippedStack(EquipmentSlot.HEAD);
        // check that the player has a banner equipped and they have permission to claim
        if (headStack != null && TerritoryManager.IsBanner(headStack) && TerritoryManager.HasPattern(headStack) && TerritoryManager.HasPermission(world, player, pos)) {
            // If the chunk is already claimed, the player is only allowed to re-upkeep it.
            BlockPos claimPos = TerritoryManager.GetBannerPosInChunk(pos);
            if (claimPos != null && !claimPos.equals(pos)) {
                return;
            }
            BlockState oldState = bannerEntity.getCachedState();
            BlockState newState;
            // checks if the clicked banner is a wall banner or regular banner and copies its state to a new banner
            boolean wallBanner = oldState.getBlock() instanceof WallBannerBlock;
            AbstractBannerBlock abb;
            BlockState abbDefaultState;
            if (wallBanner) {
                abb = getBannerBlock(headStack, true);
                if (abb == null) return;
                abbDefaultState = abb.getDefaultState();
                if (abbDefaultState == null) return;
                newState = abbDefaultState.with(WallBannerBlock.FACING, oldState.get(WallBannerBlock.FACING));
            }
            else {
                abb = getBannerBlock(headStack, false);
                if (abb == null) return;
                abbDefaultState = abb.getDefaultState();
                if (abbDefaultState == null) return;
                newState = abbDefaultState.with(BannerBlock.ROTATION, oldState.get(BannerBlock.ROTATION));
            }
            AbstractBannerBlock newBlock = ((AbstractBannerBlock)newState.getBlock());

            bannerEntity.readFrom(headStack, newBlock.getColor());
            bannerEntity.setCachedState(newState);
            TerritoryManager.ScheduleETB(pos, bannerEntity);
            TerritoryManager.CreateFireworkEffect(world, pos.getX() + 0.5, wallBanner ? pos.getY() + 0.3 : pos.getY() + 1, pos.getZ() + 0.5, bannerEntity.getPatterns());
            String headPattern = TerritoryManager.BannerToString(headStack);
            assert headPattern != null; // pattern cannot be null because we already checked IsBanner and HasPattern
            TerritoryManager.AddChunk(headPattern, pos);

            ItemStack stack = player.getStackInHand(context.getHand());
            stack.damage(1, player, (p) -> p.sendEquipmentBreakStatus(context.getHand().equals(Hand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
            cir.setReturnValue(ActionResult.CONSUME);
        }
    }

    @Unique
    @Nullable
    private AbstractBannerBlock getBannerBlock(ItemStack stack, boolean wall) {
        String id = Registries.ITEM.getId(stack.getItem()).toString();
        if (wall) {
            id = id.replace("_banner", "_wall_banner");
        }
        Block block = Registries.BLOCK.get(new Identifier(id));
        if (block instanceof AbstractBannerBlock abb) {
            return abb;
        }
        return null;
    }
}
