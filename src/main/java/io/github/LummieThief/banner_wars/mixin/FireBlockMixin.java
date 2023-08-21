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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public class FireBlockMixin {
    @Shadow
    protected boolean isFlammable(BlockState state) {return false;}
    @Shadow
    private int getSpreadChance(BlockState state) {return 0;}
    @Shadow
    private void trySpreadingFire(World world, BlockPos pos, int spreadFactor, Random random, int currentAge) {}

    // in order to have access to the fire's position, we have to do some mixin magic.
    // First: Redirect all of the trySpreadingFire() calls in scheduledTick here and do the spreadChance check immediately.
    // Second: If the fire would spread, check if it has permissions.
    // Third: If the fire spread permission check passes, make a call to trySpreadingFire() again
    // Fourth: Avoid making another spreadChance call and instead give it a 100% chance
    @Redirect(method = "scheduledTick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/FireBlock;trySpreadingFire(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/math/random/Random;I)V"))
    public void redirectFireBurn(FireBlock instance, World world, BlockPos pos, int spreadFactor, Random random, int currentAge,
                                      BlockState _state, ServerWorld _world, BlockPos _pos, Random _random) {
        int i = this.getSpreadChance(world.getBlockState(pos));
        if (random.nextInt(spreadFactor) < i) {
            BlockPos firePos = _pos;
            if (!TerritoryManager.HasPermission(firePos, pos)) {
                tryExtinguishFire(world, firePos);
            }
            else {
                trySpreadingFire(world, pos, spreadFactor, random, currentAge);
            }
        }
    }
    @Redirect(method = "trySpreadingFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/FireBlock;getSpreadChance(Lnet/minecraft/block/BlockState;)I"))
    public int skipRandomCheck(FireBlock instance, BlockState state) {
        return Integer.MAX_VALUE;
    }

    @Redirect(method = "scheduledTick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z",
            ordinal = 1))
    public boolean redirectFireSpread(ServerWorld world, BlockPos pos, BlockState blockState, int i,
                                         BlockState _state, ServerWorld _world, BlockPos _pos, Random _random) {
        BlockPos firePos = _pos;
        if (TerritoryManager.HasPermission(firePos, pos)) {
            BlockPos n = pos.north();
            BlockPos e = pos.east();
            BlockPos s = pos.south();
            BlockPos w = pos.west();
            BlockPos u = pos.up();
            BlockPos d = pos.down();
            if ((isFlammable(world.getBlockState(n)) && TerritoryManager.HasPermission(firePos, n)) ||
                    (isFlammable(world.getBlockState(e)) && TerritoryManager.HasPermission(firePos, e)) ||
                    (isFlammable(world.getBlockState(s)) && TerritoryManager.HasPermission(firePos, s)) ||
                    (isFlammable(world.getBlockState(w)) && TerritoryManager.HasPermission(firePos, w)) ||
                    (isFlammable(world.getBlockState(u)) && TerritoryManager.HasPermission(firePos, u)) ||
                    (isFlammable(world.getBlockState(d)) && TerritoryManager.HasPermission(firePos, d))) {

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
}

