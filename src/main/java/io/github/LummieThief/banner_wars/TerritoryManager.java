package io.github.LummieThief.banner_wars;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TerritoryManager implements ModInitializer {
    public static final String MOD_ID = "banner_wars";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static MinecraftServer server;
    public static ServerState state;
    public static ServerWorld overworld;
    public static ServerTickHandler serverTickHandler;

    private static final Map<BlockPos, BlockEntity> exileCache = new HashMap<>();
    private static long lastDecayedEpoch;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(srvr -> {
            server = srvr;
            state = ServerState.getServerState(server);
            overworld = server.getOverworld();

            serverTickHandler = new ServerTickHandler();
            ServerTickEvents.START_SERVER_TICK.register(serverTickHandler);
        });

/*        ServerEntityEvents.EQUIPMENT_CHANGE.register((livingEntity, equipmentSlot, previous, next) ->
        {
            if (equipmentSlot != EquipmentSlot.HEAD)
                return;
        });*/
        UseBlockCallback.EVENT.register(new UseBlockHandler());
        UseItemCallback.EVENT.register(new UseItemHandler());
        AttackBlockCallback.EVENT.register(new AttackBlockHandler());

        PlayerBlockBreakEvents.BEFORE.register(new BreakBlockHandler());

    }

    /**
     *  Determines if a given item is one of the 16 banner types, regardless of color.
     * @param stack the item to check.
     * @return whether the item is a banner.
     */
    public static boolean IsBanner(@NotNull ItemStack stack) {
        return IsBanner(stack.getItem());
    }
    public static boolean IsBanner(@NotNull Item item) {
        int id = Item.getRawId(item);
        // 1087 is id of white banner
        // 1102 is id of black banner
        return id >= 1087 && id <= 1102;
    }

    public static String BannerToString(ItemStack bannerStack) {
        if (bannerStack == null)
            return "";
        String s = Item.getRawId(bannerStack.getItem()) + "";
        NbtCompound nbt = bannerStack.getNbt();
        if (nbt != null && !nbt.isEmpty()) {
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
    public static String GetBannerInChunk(BlockPos pos) {
        if (state == null)
            return null;
        ChunkData data = state.chunkMap.get(EncodeChunkPosition(pos));
        if (data != null) {
            return data.bannerPattern();
        }
        return null;
    }
    public static BlockPos GetBannerPosInChunk(BlockPos pos) {
        if (state == null)
            return null;
        ChunkData data = state.chunkMap.get(EncodeChunkPosition(pos));
        if (data != null) {
            return DecodePosition(data.bannerPos());
        }
        return null;
    }
    public static boolean HasBannerInChunk(BlockPos pos) {
        return GetBannerInChunk(pos) != null;
    }

    public static void AddChunk(String banner, BlockPos bannerPos) {
        if (state == null)
            return;
        long chunkCode = EncodeChunkPosition(bannerPos);
        long blockCode = EncodeBlockPosition(bannerPos);
        state.chunkMap.put(chunkCode, new ChunkData(banner, blockCode, serverTickHandler.getEpoch()));
        state.markDirty();
    }

    public static boolean HasPermission(World world, PlayerEntity player, BlockPos pos) {
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
        return InDecay(world, pos, banner);
    }

    public static boolean HasPermission(World world, BlockPos source, BlockPos dest) {
        String destBanner = TerritoryManager.GetBannerInChunk(dest);
        if (destBanner == null) {
            return true;
        }
        String sourceBanner = TerritoryManager.GetBannerInChunk(source);
        return destBanner.equals(sourceBanner) || InDecay(world, dest, destBanner);
    }
    public static boolean InDecay(World world, BlockPos pos, String banner) {
        if (world.isClient || !world.getRegistryKey().equals(World.OVERWORLD) || state == null) {
            return true;
        }
        DecayData data = state.decayMap.get(banner);
        // if there's no entry in the decay map, this chunk isn't in decay
        if (data == null) {
            return false;
        }
        // otherwise, this chunk is in decay if the epoch is greater or equal to than the last decay epoch
        // and the position of the block queried is not the position of the banner that claims the chunk.
        return data.epoch() >= lastDecayedEpoch && (pos == null || !pos.equals(GetBannerPosInChunk(pos)));
    }

    public static boolean HasPattern(ItemStack bannerItem) {
        NbtCompound comp = bannerItem.getOrCreateNbt();
        NbtCompound blockEntityTag = comp.getCompound("BlockEntityTag");
        if (blockEntityTag == null)
            return false;
        NbtList list = blockEntityTag.getList("Patterns", NbtElement.COMPOUND_TYPE);
        for (NbtElement c : list) {
            return true;
        }
        return false;
    }

    public static BlockHitResult GetPlayerHitResult(PlayerEntity player, boolean includeFluid) {
        float maxDistance = 5f;
        HitResult hit = player.raycast(maxDistance, 0, includeFluid);
        if (hit.getType() == HitResult.Type.BLOCK) {
            return (BlockHitResult) hit;
        }
        else {
            return null;
        }
    }

    public static void RemoveChunk(BlockPos pos) {
        if (state == null)
            return;
        state.chunkMap.remove(EncodeChunkPosition(pos));
    }

    public static void FlickerFadingBanners(long lastLivingEpoch) {
        if (state == null)
            return;
        Queue<BlockPos> toRemove = new LinkedList<>();
        for (ChunkData chunk : state.chunkMap.values()) {
            if (chunk.epoch() < lastLivingEpoch) {
                BlockPos bannerPos = DecodePosition(chunk.bannerPos());
                toRemove.add(bannerPos);
                BlockState bannerState = overworld.getBlockState(bannerPos);
                if (bannerState.getBlock() instanceof AbstractBannerBlock) {
                    BlockEntity bannerEntity = new BannerBlockEntity(bannerPos, bannerState);
                    ScheduleETB(bannerPos, bannerEntity);
                }
            }
        }
        for(BlockPos pos : toRemove) {
            RemoveChunk(pos);
        }
    }

    public static void ETBBanners() {
        boolean c = false;
        for (BlockPos pos : exileCache.keySet()) {
            BlockEntity e = exileCache.get(pos);
            overworld.setBlockState(pos, e.getCachedState());
            overworld.addBlockEntity(e);
            overworld.updateListeners(pos, e.getCachedState(), e.getCachedState(), Block.NOTIFY_LISTENERS);
            LOGGER.info("etbd");
            c = true;
        }
        if (c)
            exileCache.clear();
    }

    public static void ScheduleETB(BlockPos pos, BlockEntity blockEntity) {
        exileCache.put(pos, blockEntity);
        overworld.removeBlock(pos, false);
    }

    public static void DecayBanner(String banner, String name) {
        if (state == null)
            return;
        if (!state.decayMap.containsKey(banner)) {
            state.decayMap.put(banner, new DecayData(serverTickHandler.getEpoch(), name));
            String cmd = String.format("/tellraw @a [{\"text\":\"[Server] \"},{\"text\":\"%s\",\"color\":\"light_purple\"}," +
                    "{\"text\":\"'s territory has entered decay for 15 minutes! \",\"color\":\"aqua\"}]", name);
            ExecuteCommand(cmd);
        }
    }

    public static void UnDecayBanners(long lastDecayedEpoch, long lastDecayProtectionEpoch) {
        if (state == null)
            return;
        TerritoryManager.lastDecayedEpoch = lastDecayedEpoch;
        for (String banner : state.decayMap.keySet()) {
            DecayData data = state.decayMap.get(banner);
            if (data.epoch() < lastDecayProtectionEpoch) {
                state.decayMap.remove(banner);
                String cmd = String.format("/tellraw @a [{\"text\":\"[Server] \"},{\"text\":\"%s\",\"color\":\"light_purple\"}," +
                        "{\"text\":\"'s territory is no longer protected from decay. \",\"color\":\"aqua\"}]", data.name());
                ExecuteCommand(cmd);
            }
            else if (data.epoch() == lastDecayedEpoch - 1) {
                String cmd = String.format("/tellraw @a [{\"text\":\"[Server] \"},{\"text\":\"%s\",\"color\":\"light_purple\"}," +
                        "{\"text\":\"'s territory is now protected from decay for 15 minutes! \",\"color\":\"aqua\"}]", data.name());
                ExecuteCommand(cmd);
            }
        }
    }

    private static void ExecuteCommand(String sayCommand) {
        server.getCommandManager().executeWithPrefix(server.getCommandSource(), sayCommand);
    }

    public static void createFireworkEffect(World world, double x, double y, double z, List<Pair<RegistryEntry<BannerPattern>, DyeColor>> patternList) {
        DyeColor mainColor = DyeColor.WHITE;
        if (patternList.size() > 0)
            mainColor = patternList.get(0).getSecond();
        Set<DyeColor> uniqueColors = new HashSet<>();
        for (int i = 0; i < patternList.size(); i++) {
            uniqueColors.add(patternList.get(i).getSecond());
        }
        int m = 2;
        int[] fireworkColors = new int[uniqueColors.size() + m];
        for (int i = 0; i < m; i++) {
            fireworkColors[i] = mainColor.getFireworkColor();
        }
        for (DyeColor color : uniqueColors) {
            fireworkColors[m] = color.getFireworkColor();
            m++;
        }

        ItemStack fireworkStack = Items.FIREWORK_ROCKET.getDefaultStack();
        NbtCompound nbt = new NbtCompound();
        NbtCompound fireworkNbt = new NbtCompound();
        NbtList explosions = new NbtList();
        NbtCompound star = new NbtCompound();
        star.putIntArray("Colors", fireworkColors);
        star.putByte("Type", (byte) 0);
        explosions.add(star);
        fireworkNbt.put("Explosions", explosions);
        fireworkNbt.putByte("Flight", (byte) 0);
        nbt.put("Fireworks", fireworkNbt);
        fireworkStack.setNbt(nbt);
        FireworkRocketEntity firework = new FireworkRocketEntity(world, x, y, z, fireworkStack);
        world.spawnEntity(firework);
        ((IFireworkRocketEntityMixin)firework).triggerExplosion();
    }
}
