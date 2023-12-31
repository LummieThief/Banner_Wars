package io.github.LummieThief.banner_wars;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/*
nbt{
    chunks[
        chunk{
            banner,
            pos,
            epoch
        }
    ],
    players[
        player{
            username,
            banners[
                banner{
                    pattern
                }
            ]
        }
    ]
}
*/
public class ServerState extends PersistentState {
    private static final boolean RESET = false;
    public Map<String, TerritoryData> territoryMap = new HashMap<>();
    public Map<Long, ChunkData> chunkMap = new HashMap<>();
    public Map<String, DecayData> decayMap = new HashMap<>();
    public Map<String, Set<String>> betrayalMap = new HashMap<>();
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        if (RESET) {
            nbt = new NbtCompound();
        }
        else {
            NbtList chunks = new NbtList();
            for (long chunkCode : chunkMap.keySet()) {
                NbtCompound chunk = new NbtCompound();

                ChunkData chunkData = chunkMap.get(chunkCode);
                chunk.putString("banner", chunkData.bannerPattern());
                chunk.putLong("pos", chunkData.bannerPos());
                chunk.putInt("epoch", chunkData.epoch());

                chunks.add(chunk);
            }
            nbt.put("chunks", chunks);

            NbtList players = new NbtList();
            for (String username : betrayalMap.keySet()) {
                NbtCompound player = new NbtCompound();
                player.putString("username", username);

                NbtList banners = new NbtList();
                Set<String> set = betrayalMap.getOrDefault(username, new HashSet<>());
                for (String pattern : set) {
                    NbtCompound banner = new NbtCompound();
                    banner.putString("pattern", pattern);
                    banners.add(banner);
                }
                player.put("banners", banners);
                players.add(player);
            }
            nbt.put("players", players);
        }
        return nbt;
    }

    public static ServerState createFromNbt(NbtCompound tag) {
        ServerState state = new ServerState();
        if (RESET) {
            state.markDirty();
        }
        else {
            state.chunkMap = new HashMap<>();
            state.betrayalMap = new HashMap<>();
            state.territoryMap = new HashMap<>();

            NbtList chunkList = tag.getList("chunks", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < chunkList.size(); i++) {
                NbtCompound chunk = chunkList.getCompound(i);
                String banner = chunk.getString("banner");
                ChunkData chunkData = new ChunkData(banner, chunk.getLong("pos"), chunk.getInt("epoch"));
                state.AddChunk(TerritoryManager.ConvertBlockEncodingToChunkEncoding(chunkData.bannerPos()), chunkData);
            }

            NbtList playerList = tag.getList("players", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < playerList.size(); i++) {
                NbtCompound player = playerList.getCompound(i);
                String username = player.getString("username");
                Set<String> patternSet = new HashSet<>();
                NbtList bannerList = player.getList("banners", NbtElement.COMPOUND_TYPE);
                for (int k = 0; k < bannerList.size(); k++) {
                    NbtCompound banner = bannerList.getCompound(k);
                    String pattern = banner.getString("pattern");
                    patternSet.add(pattern);
                }
                state.betrayalMap.put(username, patternSet);
            }
        }
        return state;
    }

    public static ServerState getServerState(MinecraftServer server) {
        // First we get the persistentStateManager for the OVERWORLD
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        assert overworld != null;
        PersistentStateManager persistentStateManager = overworld.getPersistentStateManager();

        // Calling this reads the file from the disk if it exists, or creates a new one and saves it to the disk
        // You need to use a unique string as the key. You should already have a MODID variable defined by you somewhere in your code. Use that.

        return persistentStateManager.getOrCreate(
                ServerState::createFromNbt,
                ServerState::new,
                TerritoryManager.MOD_ID);

    }

    public void AddChunk(Long chunkCode, ChunkData data) {
        TerritoryData territory = territoryMap.computeIfAbsent(data.bannerPattern(), k -> new TerritoryData());

        ChunkData oldData = chunkMap.get(chunkCode);
        if (oldData != null) {
            territory.remove(oldData);
        }

        chunkMap.put(chunkCode, data);
        territory.add(data);
        markDirty();
    }

    public void RemoveChunk(Long chunkCode) {
        ChunkData data = chunkMap.remove(chunkCode);
        if (data != null) {
            TerritoryData territory = territoryMap.get(data.bannerPattern());
            if (territory == null)
                return;
            if (territory.remove(data))
                markDirty();
            if (territory.size() == 0)
                territoryMap.remove(data.bannerPattern());
        }
    }

    public List<ChunkData> PeekTerritoryData(@NotNull String pattern, int window) {
        TerritoryData data = territoryMap.get(pattern);
        if (data == null) {
            return new ArrayList<>();
        }

        return data.peek(window);
    }
}
