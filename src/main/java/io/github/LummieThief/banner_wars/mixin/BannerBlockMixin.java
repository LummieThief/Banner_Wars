package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Equipment;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(BannerBlock.class)
public abstract class BannerBlockMixin extends AbstractBannerBlock implements Equipment {
    protected BannerBlockMixin(DyeColor color, Settings settings) {
        super(color, settings);
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
    public void onStacksDropped(BlockState state, ServerWorld world, BlockPos pos, ItemStack tool, boolean dropExperience) {
        TerritoryManager.LOGGER.info("dropped stack");
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        //builder.
        return new ArrayList<>();
        List<ItemStack> itemStacks = super.getDroppedStacks(state, builder);
        if (itemStacks.size() > 0) {
            ItemStack bannerStack = itemStacks.get(0);
            TerritoryManager.LOGGER.info(TerritoryManager.BannerToString(bannerStack));
            //if (TerritoryManager.GetBannerInChunk())
        }
        return itemStacks;
    }*/
}