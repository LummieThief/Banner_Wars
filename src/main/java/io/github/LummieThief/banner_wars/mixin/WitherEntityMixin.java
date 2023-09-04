package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherEntity.class)
public class WitherEntityMixin {
    @Inject(method = "mobTick", at = @At(value="TAIL"))
    protected void overrideMobTick(CallbackInfo ci) {
        WitherEntity ths = ((WitherEntity) (Object) this);
        if (ths.age >= 24000) {
            ths.discard();
        }
    }
    @Redirect(
            method = "mobTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;")
    )
    private BlockState injected(World world, BlockPos pos) {
        if (world.isClient || !world.getRegistryKey().equals(World.OVERWORLD))
            return world.getBlockState(pos);
        String claimBanner = TerritoryManager.GetBannerInChunk(pos);
        if (claimBanner != null && !TerritoryManager.InDecay(world, pos, claimBanner)) {
            return world.getBlockState(new BlockPos(0, -64, 0));
        }
        else {
            return world.getBlockState(pos);
        }
    }
}
