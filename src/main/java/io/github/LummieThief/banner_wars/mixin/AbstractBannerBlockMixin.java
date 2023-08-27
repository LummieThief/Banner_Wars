package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Equipment;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(AbstractBannerBlock.class)
public abstract class AbstractBannerBlockMixin extends BlockWithEntity implements Equipment {
    protected AbstractBannerBlockMixin(Settings settings) {
        super(settings);
    }

    public EquipmentSlot getSlotType() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        TerritoryManager.LOGGER.info("broken");
        super.onBreak(world, pos, state, player);
    }
/*    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        BlockPos targetPos = new BlockPos(20, -23, 148);

        if (((AbstractBannerBlock)(Object)this) instanceof BannerBlock) {
            world.addParticle(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, state),
                    (double)pos.getX() + 0.5,
                    (double)pos.getY() + 2.0,
                    (double)pos.getZ() + 0.5,
                    0, 0, 0);
        }
        else {
            world.addParticle(ParticleTypes.EFFECT,
                    (double)pos.getX() + 0.5,
                    (double)pos.getY() + 1.0,
                    (double)pos.getZ() + 0.5,
                    0, 0, 0);
        }
    }*/
}
