package io.github.LummieThief.banner_wars;

import java.util.*;

public class TerritoryData {
    private final TreeMap<Integer, Set<ChunkData>> dataMap;

    public TerritoryData() {
        dataMap = new TreeMap<>();
    }

    public void add(ChunkData data) {
        Set<ChunkData> set = dataMap.computeIfAbsent(data.epoch(), k -> new HashSet<>());
        set.add(data);
    }

    public boolean remove(ChunkData data) {
        Set<ChunkData> set = dataMap.get(data.epoch());
        if (set == null)
            return false;
        if (set.remove(data)) {
            if (set.isEmpty()) {
                dataMap.remove(data.epoch());
            }
            return true;
        }
        return false;
    }

    public int size() {
        return dataMap.size();
    }

    public List<ChunkData> peek(int window) {
        List<ChunkData> l = new ArrayList<>();
        for (Map.Entry<Integer, Set<ChunkData>> entry : dataMap.entrySet()) {
            for (ChunkData data : entry.getValue()) {
                l.add(data);
                if (l.size() >= window)
                    return l;
            }
        }
        return l;
    }
}
