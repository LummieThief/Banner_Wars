package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(World.class)
public class WorldMixin {

    @Inject(method = "breakBlock", at = @At("RETURN"))
    public void overrideBannerBreak(BlockPos pos, boolean drop, Entity breakingEntity, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        World world = (World)(Object)this;
        if (world.getBlockState(pos).getBlock() instanceof AbstractBannerBlock) {
            TerritoryManager.LOGGER.info("world broken");
        }
    }

    /*@Inject(method = "onBlockChanged", at = @At("RETURN"))
    public void onBannerBroken(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo ci) {
        ServerWorld world = (ServerWorld)(Object)this;
        if (oldBlock.getBlock() instanceof AbstractBannerBlock && !(newBlock.getBlock() instanceof AbstractBannerBlock)) {
            TerritoryManager.LOGGER.info("banner broken");
            *//*List<ItemStack> stacks = oldBlock.getDroppedStacks(new LootContextParameterSet.Builder(world));
            if (stacks.size() >= 0) {
                ItemStack stack = stacks.get(0);
                if (TerritoryManager.GetBannerInChunk(pos).equals(TerritoryManager.BannerToString(stack))) {
                    TerritoryManager.LOGGER.info("banner match");
                }
            }*//*
        }
    }*/
}
