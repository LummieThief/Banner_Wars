package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.IFallingBlockEntityMixin;
import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin implements IFallingBlockEntityMixin {
    @Unique private BlockPos spawnPos;

    @Inject(method = "spawnFromBlock", at = @At("RETURN"))
    private static void spawnFromBlock(World world, BlockPos pos, BlockState state, CallbackInfoReturnable<FallingBlockEntity> cir) {
        FallingBlockEntity e = cir.getReturnValue();
        ((IFallingBlockEntityMixin)e).setSpawnPos(pos);
    }
    @ModifyVariable(method = "tick", at = @At(value = "STORE"), ordinal = 2)
    private boolean OnLanding(boolean oldBool) {
        if (oldBool) {
            BlockPos landingPos = ((FallingBlockEntity)(Object)this).getBlockPos();
            if (TerritoryManager.HasPermission(spawnPos, landingPos)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setSpawnPos(BlockPos pos) {
        spawnPos = pos;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    public void writeSpawnPos(NbtCompound nbt, CallbackInfo ci) {
        nbt.put("spawnPos", NbtHelper.fromBlockPos(spawnPos));
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    public void readSpawnPos(NbtCompound nbt, CallbackInfo ci) {
        spawnPos = NbtHelper.toBlockPos(nbt.getCompound("spawnPos"));
    }
}