package io.github.LummieThief.banner_wars;

import com.mojang.datafixers.util.Pair;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.DyeColor;

import java.util.List;

public interface IBannerBlockEntityMixin {
    default void clearNBT() {}
}
