package io.github.LummieThief.banner_wars.mixin;

import com.mojang.datafixers.util.Pair;
import io.github.LummieThief.banner_wars.IFireworkRocketEntityMixin;
import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.*;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.BannerItem;
import net.minecraft.item.Equipment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient || hand.equals(Hand.OFF_HAND))
            return ActionResult.PASS;
        ItemStack headStack = player.getInventory().getArmorStack(3);
        if (headStack != null && TerritoryManager.isBanner(headStack) && !TerritoryManager.HasBannerInChunk(pos)) {
            TerritoryManager.LOGGER.info("used banner");
            BlockEntity e = world.getBlockEntity(pos);
            if (!(e instanceof BannerBlockEntity bannerEntity)) {
                return ActionResult.FAIL;
            }
            BlockState newState = ((BannerItem)headStack.getItem()).getBlock().getDefaultState().with(
                    BannerBlock.ROTATION, bannerEntity.getCachedState().get(BannerBlock.ROTATION));
            AbstractBannerBlock newBlock = ((AbstractBannerBlock)newState.getBlock());

            bannerEntity.readFrom(headStack, newBlock.getColor());
            bannerEntity.setCachedState(newState);
            TerritoryManager.FlickerBanner(pos, bannerEntity);

            TerritoryManager.createFireworkEffect(world, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, bannerEntity.getPatterns());
            TerritoryManager.AddChunk(TerritoryManager.BannerToString(headStack), pos);
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }
}
