package io.github.LummieThief.banner_wars;

import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

public interface IBlockWithEntityMixin {
    default void addPacket(ServerPlayerEntity player, BlockUpdateS2CPacket packet) {}
}
