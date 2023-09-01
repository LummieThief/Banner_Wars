package io.github.LummieThief.banner_wars.mixin;

import com.mojang.datafixers.util.Pair;
import io.github.LummieThief.banner_wars.IFireworkRocketEntityMixin;
import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.*;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.*;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
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
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        List<ItemStack> items = super.getDroppedStacks(state, builder);
        if (items.size() == 0)
            return items;
        ItemStack firstItem = items.get(0);
        // if the dropped item is a banner
        if (TerritoryManager.isBanner(firstItem)) {
            // drop the default stack version of it instead
            items.set(0, firstItem.getItem().getDefaultStack());
            Vec3d origin = builder.get(LootContextParameters.ORIGIN);
            BlockPos pos = new BlockPos((int)origin.getX(), (int)origin.getY(), (int)origin.getZ());
            // if the dropped item is the same banner as the one that may or may not be claiming this chunk
            if (TerritoryManager.BannerToString(firstItem).equals(TerritoryManager.GetBannerInChunk(pos)) && builder.getWorld().getRegistryKey().equals(World.OVERWORLD)) {
                // remove the claim in this chunk
                TerritoryManager.RemoveChunk(pos);
            }
        }
        return items;
    }
}
