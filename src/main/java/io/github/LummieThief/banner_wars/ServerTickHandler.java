package io.github.LummieThief.banner_wars;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

public class ServerTickHandler implements ServerTickEvents.StartTick {
    private static final int TICKS_PER_EPOCH = 100; // 100 = 12 ticks/minute
    private static final int CLAIM_LIFETIME = 60; // 17280 = 24 hours * 60 minutes * 12 ticks/minute
    private static final int DECAY_TIME = 12; // 180 = 15 minutes * 12 ticks/minute
    private long time;

    public ServerTickHandler() {
    }
    @Override
    public void onStartTick(MinecraftServer server) {
        World world = server.getWorld(World.OVERWORLD);
        assert world != null;
        time = world.getTime();
        if (time % TICKS_PER_EPOCH == 0) {
            TerritoryManager.LOGGER.info("ServerTick: " + (time / TICKS_PER_EPOCH));
            TerritoryManager.FlickerFadingBanners(getEpoch() - CLAIM_LIFETIME);
            TerritoryManager.UnDecayBanners(getEpoch() - DECAY_TIME, getEpoch() - DECAY_TIME * 2);
        }
        else
            TerritoryManager.ETBBanners();
    }
    public int getEpoch() {
        return (int)(time / TICKS_PER_EPOCH);
    }
}
