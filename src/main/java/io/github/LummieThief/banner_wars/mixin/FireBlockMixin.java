package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public class FireBlockMixin {
    @Unique
    private BlockPos firePos_w706;
    @Shadow
    protected boolean isFlammable(BlockState state) {
        return false;
    }

    @Inject(method = "scheduledTick", at = @At("HEAD"))
    public void saveFirePosition(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        firePos_w706 = pos;
    }

    @Inject(method = "trySpreadingFire", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;",
            ordinal = 1),
            cancellable = true)
    public void overrideTrySpreadingFire(World world, BlockPos pos, int spreadFactor, Random random, int currentAge, CallbackInfo ci) {
        if (!canSpreadToChunk(firePos_w706, pos)) {
            tryExtinguishFire(world, firePos_w706);
            ci.cancel();
        }
    }
    @Redirect(method = "scheduledTick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z",
            ordinal = 1))
    public boolean overrideScheduledTick(ServerWorld world, BlockPos pos, BlockState blockState, int i) {
        if (canSpreadToChunk(firePos_w706, pos)) {
            BlockPos n = pos.north();
            BlockPos e = pos.east();
            BlockPos s = pos.south();
            BlockPos w = pos.west();
            BlockPos u = pos.up();
            BlockPos d = pos.down();
            if ((isFlammable(world.getBlockState(n)) && canSpreadToChunk(firePos_w706, n)) ||
                    (isFlammable(world.getBlockState(e)) && canSpreadToChunk(firePos_w706, e)) ||
                    (isFlammable(world.getBlockState(s)) && canSpreadToChunk(firePos_w706, s)) ||
                    (isFlammable(world.getBlockState(w)) && canSpreadToChunk(firePos_w706, w)) ||
                    (isFlammable(world.getBlockState(u)) && canSpreadToChunk(firePos_w706, u)) ||
                    (isFlammable(world.getBlockState(d)) && canSpreadToChunk(firePos_w706, d))) {

                return world.setBlockState(pos, blockState, i);
            }
            // the flammable block that is being burned is in a claimed chunk, so don't spread
        }
        return false;
    }

    private void tryExtinguishFire(World world, BlockPos firePos) {
        Block downBlock = world.getBlockState(firePos.down()).getBlock();
        if(!(downBlock.equals(Blocks.NETHERRACK) || downBlock.equals(Blocks.MAGMA_BLOCK))) {
            world.removeBlock(firePos, false);
        }
    }

    private boolean canSpreadToChunk(BlockPos sourcePos, BlockPos spreadPos) {
        String blockBanner = TerritoryManager.GetBannerFromChunk(spreadPos.getX() >> 4, spreadPos.getZ() >> 4);
        if (blockBanner == null) {
            return true;
        }
        String fireBanner = TerritoryManager.GetBannerFromChunk(sourcePos.getX() >> 4, sourcePos.getZ() >> 4);
        return blockBanner.equals(fireBanner);
    }
}

