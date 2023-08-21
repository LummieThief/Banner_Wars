package io.github.LummieThief.banner_wars;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class TerritoryManager implements ModInitializer {
    public static final String MOD_ID = "banner_wars";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ServerState state;
    public static World world;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            state = ServerState.getServerState(server);
            world = server.getOverworld();
            //LOGGER.info(world.getBlockState(new BlockPos(0, 500, 0)).isAir() + " tt");
        });

        ServerEntityEvents.EQUIPMENT_CHANGE.register((livingEntity, equipmentSlot, previous, next) ->
        {
            if (equipmentSlot != EquipmentSlot.HEAD)
                return;
            if (isBanner(next)) {
                LOGGER.info(next.getNbt() + "");
            }
        });

        UseBlockCallback.EVENT.register(new UseBlockHandler());
        UseItemCallback.EVENT.register(new UseItemHandler());

        PlayerBlockBreakEvents.BEFORE.register(new BreakBlockHandler());
        //PlayerBlockBreakEvents.CANCELED.register(new BreakBlockCancelledHandler());
        //PlayerBlockBreakEvents.AFTER.register(new AfterBreakBlockHandler());
    }

    /**
     *  Determines if a given item is one of the 16 banner types, regardless of color.
     * @param item the item to check.
     * @return whether the item is a banner.
     */
    public static boolean isBanner(@NotNull ItemStack item) {
        int id = Item.getRawId(item.getItem());
        // 1087 is id of white banner
        // 1102 is id of black banner
        return id >= 1087 && id <= 1102;
    }

    public static Long EncodeChunk(int chunkX, int chunkZ) {
        long lcx = Integer.toUnsignedLong(chunkX) << 32;
        long lcz = Integer.toUnsignedLong(chunkZ);
        return lcx | lcz;
    }

    public static Pair<Integer, Integer> DecodeChunk(Long chunkL) {
        int x = (int)(chunkL >> 32);
        int z = (int)((chunkL << 32) >> 32);
        return new Pair<>(x, z);
    }

    public static String BannerToString(ItemStack bannerStack) {
        String s = Item.getRawId(bannerStack.getItem()) + "";
        NbtCompound nbt = bannerStack.getNbt();
        if (nbt != null) {
            s += nbt.toString();
            s = s.substring(0, s.lastIndexOf(']'));
        }
        return s;
    }

    public static boolean HasChunk(int chunkX, int chunkZ) {
        return GetBannerFromChunk(chunkX, chunkZ) != null;
    }

    public static String GetBannerFromChunk(int chunkX, int chunkZ) {
        long chunkL = EncodeChunk(chunkX, chunkZ);
        String s = state.chunkToBannerMap.get(chunkL);
        return s;
    }

    public static void AddChunk(String banner, int chunkX, int chunkZ) {
        long chunkL = EncodeChunk(chunkX, chunkZ);
        Set<Long> set = state.bannerToChunkMap.getOrDefault(banner, new HashSet<>());
        set.add(chunkL);
        state.bannerToChunkMap.put(banner, set);
        state.chunkToBannerMap.put(chunkL, banner);
        //LOGGER.info("added " + chunkL + " -> " + banner + " to chunkToBannerMap");
        state.markDirty();
    }

    public static boolean HasPermission(World world, PlayerEntity player, BlockPos pos) {
        ChunkPos chunkPos = world.getChunk(pos).getPos();
        // chunk is unclaimed, so player has permission
        if (!TerritoryManager.HasChunk(chunkPos.x, chunkPos.z)) {
            //TerritoryManager.LOGGER.info("chunk is unclaimed");
            return true;
        }

        // chunk is claimed, so player only has permission if their banner matches the banner that owns the chunk
        String existingBanner = TerritoryManager.GetBannerFromChunk(chunkPos.x, chunkPos.z);

        ItemStack headStack = player.getEquippedStack(EquipmentSlot.HEAD);
        if (headStack != null) {
            String headBanner = TerritoryManager.BannerToString(headStack);
            if (existingBanner.equals(headBanner)) {
                //TerritoryManager.LOGGER.info("banner matched");
                return true;
            }
        }

        // player's banner did not match
        //TerritoryManager.LOGGER.info("banner didn't match or no banner equipped");
        return false;
    }

    public static BlockHitResult GetPlayerHitResult(World world, PlayerEntity player, boolean includeFluid) {
        float maxDistance = 4.2f;
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            if (serverPlayer.interactionManager.getGameMode() != GameMode.SURVIVAL)
                maxDistance = 4.7f;
        }
        HitResult hit = player.raycast(maxDistance, 0, includeFluid);
        if (hit.getType() == HitResult.Type.BLOCK) {
            return (BlockHitResult) hit;
        }
        else {
            return null;
        }
    }
}
