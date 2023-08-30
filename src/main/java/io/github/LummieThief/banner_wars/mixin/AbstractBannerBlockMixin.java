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
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
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
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient || hand.equals(Hand.OFF_HAND))
            return ActionResult.PASS;
        ItemStack headStack = player.getInventory().getArmorStack(3);
        // check that the player has a banner equipped and they have permission to claim
        if (headStack != null && TerritoryManager.isBanner(headStack) && TerritoryManager.HasPermission(player, pos)) {
            // If the chunk is already claimed, the player is only allowed to re-upkeep it.
            if (TerritoryManager.HasBannerInChunk(pos) && !TerritoryManager.GetBannerPosInChunk(pos).equals(pos)) {
                return ActionResult.FAIL;
            }
            // Gets the BannerBlockEntity of the banner
            if (!(world.getBlockEntity(pos) instanceof BannerBlockEntity bannerEntity)) return ActionResult.FAIL;
            BlockState oldState = bannerEntity.getCachedState();
            BlockState newState;
            // checks if the clicked banner is a wall banner or regular banner and copies its state to a new banner
            boolean wallBanner = oldState.getBlock() instanceof WallBannerBlock;
            if (wallBanner) {
                newState = getBannerBlock(headStack, true).getDefaultState().with(
                        WallBannerBlock.FACING, oldState.get(WallBannerBlock.FACING));
            }
            else {
                newState = getBannerBlock(headStack, false).getDefaultState().with(
                        BannerBlock.ROTATION, oldState.get(BannerBlock.ROTATION));
            }
            AbstractBannerBlock newBlock = ((AbstractBannerBlock)newState.getBlock());


            bannerEntity.readFrom(headStack, newBlock.getColor());
            bannerEntity.setCachedState(newState);
            TerritoryManager.FlickerBanner(pos, bannerEntity);
            TerritoryManager.createFireworkEffect(world, pos.getX() + 0.5, wallBanner ? pos.getY() + 0.3 : pos.getY() + 1, pos.getZ() + 0.5, bannerEntity.getPatterns());
            TerritoryManager.AddChunk(TerritoryManager.BannerToString(headStack), pos);
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
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
            if (TerritoryManager.BannerToString(firstItem).equals(TerritoryManager.GetBannerInChunk(pos))) {
                // remove the claim in this chunk
                TerritoryManager.RemoveChunk(pos);
            }
        }
        return items;
    }

    private AbstractBannerBlock getBannerBlock(ItemStack stack, boolean wall) {
        String id = Registries.ITEM.getId(stack.getItem()).toString();
        if (wall) {
            id = id.replace("_banner", "_wall_banner");
        }
        Block block = Registries.BLOCK.get(new Identifier(id));
        if (block instanceof AbstractBannerBlock abb) {
            return abb;
        }
        return null;
    }
}
