package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmallFireballEntity.class)
public abstract class SmallFireballEntityMixin extends Entity {
    public SmallFireballEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }
    @Inject(method = "onBlockHit", at = @At("HEAD"), cancellable = true)
    protected void overrideOnBlockHit(BlockHitResult blockHitResult, CallbackInfo ci) {
        if (getEntityWorld().isClient || !getEntityWorld().getRegistryKey().equals(World.OVERWORLD))
            return;
        BlockPos pos = blockHitResult.getBlockPos().offset(blockHitResult.getSide());
        String claimBanner = TerritoryManager.GetBannerInChunk(pos);
        if (claimBanner != null && !TerritoryManager.InDecay(getEntityWorld(), pos, claimBanner)) {
            ci.cancel();
        }
    }
}
