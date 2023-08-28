package io.github.LummieThief.banner_wars.mixin;

import com.mojang.datafixers.util.Pair;
import io.github.LummieThief.banner_wars.IBannerBlockEntityMixin;
import net.fabricmc.fabric.api.block.v1.FabricBlockState;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.DyeColor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(BannerBlockEntity.class)
public abstract class BannerBlockEntityMixin implements IBannerBlockEntityMixin {
    @Shadow @Nullable
    private NbtList patternListNbt;
    @Shadow @Nullable
    private List<Pair<RegistryEntry<BannerPattern>, DyeColor>> patterns;


    @Override
    public void clearNBT()
    {
        patterns = null;
        patternListNbt = null;

    }
}
