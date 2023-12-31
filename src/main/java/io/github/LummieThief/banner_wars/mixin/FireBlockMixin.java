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
import org.spongepowered.asm.mixin.injection.Redirect;

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
                                      BlockState _state, ServerWorld _world, BlockPos firePos, Random _random) {
        int i = this.getSpreadChance(world.getBlockState(pos));
        if (random.nextInt(spreadFactor) < i) {
            if (!TerritoryManager.HasPermission(world, firePos, pos)) {
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
                                         BlockState _state, ServerWorld _world, BlockPos firePos, Random _random) {
        if (world.isClient || !world.getRegistryKey().equals(World.OVERWORLD))
            return world.setBlockState(pos, blockState, i);
        if (TerritoryManager.HasPermission(world, firePos, pos)) {
            BlockPos n = pos.north();
            BlockPos e = pos.east();
            BlockPos s = pos.south();
            BlockPos w = pos.west();
            BlockPos u = pos.up();
            BlockPos d = pos.down();
            if ((isFlammable(world.getBlockState(n)) && TerritoryManager.HasPermission(world, firePos, n)) ||
                    (isFlammable(world.getBlockState(e)) && TerritoryManager.HasPermission(world, firePos, e)) ||
                    (isFlammable(world.getBlockState(s)) && TerritoryManager.HasPermission(world, firePos, s)) ||
                    (isFlammable(world.getBlockState(w)) && TerritoryManager.HasPermission(world, firePos, w)) ||
                    (isFlammable(world.getBlockState(u)) && TerritoryManager.HasPermission(world, firePos, u)) ||
                    (isFlammable(world.getBlockState(d)) && TerritoryManager.HasPermission(world, firePos, d))) {

                return world.setBlockState(pos, blockState, i);
            }
            // the flammable block that is being burned is in a claimed chunk, so don't spread
        }
        return false;
    }
    @Unique
    private void tryExtinguishFire(World world, BlockPos firePos) {
        Block downBlock = world.getBlockState(firePos.down()).getBlock();
        if(!(downBlock.equals(Blocks.NETHERRACK) || downBlock.equals(Blocks.MAGMA_BLOCK))) {
            world.removeBlock(firePos, false);
        }
    }
}

