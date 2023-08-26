package io.github.LummieThief.banner_wars;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.listener.GameEventListener;
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

    public static String BannerToString(ItemStack bannerStack) {
        String s = Item.getRawId(bannerStack.getItem()) + "";
        NbtCompound nbt = bannerStack.getNbt();
        if (nbt != null) {
            s += nbt.toString();
            s = s.substring(0, s.lastIndexOf(']'));
        }
        return s;
    }

    // Encodes a blockPos as a single long, with the first 26 bits being the X value, the next 9 bits being the Y value,
    // the next 26 bits being the Z value, and the last 3 bits being the sign of each of X, Y, and Z
    public static Long EncodeBlockPosition(BlockPos blockPos) {
        int bannerX = blockPos.getX();
        int bannerY = blockPos.getY();
        int bannerZ = blockPos.getZ();
        long code = 0;
        code |= Integer.toUnsignedLong(Math.abs(bannerX)) << 38;
        code |= Integer.toUnsignedLong(Math.abs(bannerY)) << 29;
        code |= Integer.toUnsignedLong(Math.abs(bannerZ)) << 3;
        if (bannerX < 0)
            code |= 0b100;
        if (bannerY < 0)
            code |= 0b010;
        if (bannerZ < 0)
            code |= 0b001;
        return code;
    }

    // Encodes the chunk that contains the specified block into as a single long by converting it into a block and
    // encoding the block position. If the block was below y = 0, then the Y value of the BlockPos will be -1.
    // Otherwise, it will be 1.
    public static Long EncodeChunkPosition(BlockPos blockInChunk) {
        int y = blockInChunk.getY() < 0 ? -1 : 1;
        BlockPos chunkPos = new BlockPos(blockInChunk.getX() >> 4, y, blockInChunk.getZ() >> 4);
        return EncodeBlockPosition(chunkPos);
    }

    // Encodes a chunk position into a single long by converting it into a block and encoding the block position.
    // If belowZero is set, then the Y value of the BlockPos will be -1. Otherwise, it will be 1.
    public static Long EncodeChunkPosition(ChunkPos chunkPos, boolean belowZero) {
        int y = belowZero ? -1 : 1;
        BlockPos pos = new BlockPos(chunkPos.x, y, chunkPos.z);
        return EncodeBlockPosition(pos);
    }

    // Decodes an encoded block position. If a chunk position is desired, the user is expected to convert the BlockPos
    // into a ChunkPos for the sake of not requiring output parameters.
    public static BlockPos DecodePosition(Long code) {
        long bannerX = code >>> 38;
        long bannerY = (code << 26) >>> 55;
        long bannerZ = (code << 35) >>> 38;
        if (((code >> 2) & 1) == 1) bannerX *= -1;
        if (((code >> 1) & 1) == 1) bannerY *= -1;
        if ((code & 1) == 1) bannerZ *= -1;
        return new BlockPos((int)bannerX, (int)bannerY, (int)bannerZ);
    }

    // Takes an encoded block position and re-encodes it as the chunk that contains that block
    public static Long ConvertBlockEncodingToChunkEncoding(Long bannerPos) {
        return EncodeChunkPosition(DecodePosition(bannerPos));
    }

    public static String GetBannerInChunk(ChunkPos pos, boolean belowZero) {
        return state.chunkToBannerMap.get(EncodeChunkPosition(pos, belowZero));
    }
    public static String GetBannerInChunk(BlockPos pos) {
        return state.chunkToBannerMap.get(EncodeChunkPosition(pos));
    }
    public static boolean HasBannerInChunk(BlockPos pos) {
        return GetBannerInChunk(pos) != null;
    }

    public static void AddBannerToChunk(String banner, BlockPos bannerPos) {
        long chunkCode = EncodeChunkPosition(bannerPos);
        long blockCode = EncodeBlockPosition(bannerPos);
        Set<Long> set = state.bannerToPositionsMap.getOrDefault(banner, new HashSet<>());
        set.add(blockCode);
        state.bannerToPositionsMap.put(banner, set);
        state.chunkToBannerMap.put(chunkCode, banner);
        state.markDirty();
    }

    public static boolean HasPermission(PlayerEntity player, BlockPos pos) {
        String banner = TerritoryManager.GetBannerInChunk(pos);
        if (banner == null) {
            // chunk is unclaimed, so player has permission
            return true;
        }

        // chunk is claimed, so player only has permission if their banner matches the banner in the chunk
        ItemStack headStack = player.getEquippedStack(EquipmentSlot.HEAD);
        if (headStack != null) {
            String headBanner = TerritoryManager.BannerToString(headStack);
            if (banner.equals(headBanner)) {
                return true;
            }
        }

        // player's banner did not match
        return false;
    }

    public static boolean HasPermission(BlockPos source, BlockPos dest) {
        String destBanner = TerritoryManager.GetBannerInChunk(dest);
        if (destBanner == null) {
            return true;
        }
        String sourceBanner = TerritoryManager.GetBannerInChunk(source);
        return destBanner.equals(sourceBanner);
    }

    public static BlockHitResult GetPlayerHitResult(PlayerEntity player, boolean includeFluid) {
        /*float maxDistance = 4.2f;
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            if (serverPlayer.interactionManager.getGameMode() != GameMode.SURVIVAL)
                maxDistance = 4.7f;
        }*/
        float maxDistance = 5f;
        HitResult hit = player.raycast(maxDistance, 0, includeFluid);
        if (hit.getType() == HitResult.Type.BLOCK) {
            return (BlockHitResult) hit;
        }
        else {
            return null;
        }
    }
}
