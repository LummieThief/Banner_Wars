package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LightningEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightningEntity.class)
public class LightningEntityMixin {
    @Shadow private int blocksSetOnFire;
    @Redirect(method = "spawnFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private boolean overrideSpawnFire(World world, BlockPos pos, BlockState state) {
        if (world.isClient || !world.getRegistryKey().equals(World.OVERWORLD))
            return world.setBlockState(pos, state);
        String claimBanner = TerritoryManager.GetBannerInChunk(pos);
        if (claimBanner != null && !TerritoryManager.InDecay(world, pos, claimBanner)) {
            blocksSetOnFire--;
            return false;
        }
        else {
            return world.setBlockState(pos, state);
        }
    }
}
