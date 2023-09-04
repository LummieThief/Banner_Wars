package io.github.LummieThief.banner_wars;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class ServerPlayDisconnectHandler implements ServerPlayConnectionEvents.Disconnect {
/*    @Override
    public void onPlayDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        VexEntity vex = new VexEntity(EntityType.VEX, player.getWorld());
        vex.setPos(player.getX(), player.getY() + 1, player.getZ());
        vex.setAiDisabled(true);
        player.getWorld().spawnEntity(vex);
        vex.setCustomName(Text.literal(player.getEntityName() + "'s Spirit"));
        vex.setPersistent();
        vex.setSilent(true);
        vex.equipStack(EquipmentSlot.MAINHAND, player.getInventory().getArmorStack(3));
        NbtList inventoryNbt = new NbtList();
        player.getInventory().writeNbt(inventoryNbt);
        //vex.setCustomNameVisible(true);
        //player.getWorld().spawnEntity(new ZombieEntity())
        TerritoryManager.LOGGER.info(handler.getPlayer().getEntityName());

    }*/
    @Override
    public void onPlayDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        if (player.getPrimeAdversary() == null || !player.getPrimeAdversary().isPlayer())
            return;
        ItemStack headStack = player.getEquippedStack(EquipmentSlot.HEAD);
        if (headStack == null || headStack.isEmpty() || !TerritoryManager.IsBanner(headStack))
            return;

        PlayerEntity adversary = (PlayerEntity) player.getPrimeAdversary();
        ItemStack adversaryHeadStack = adversary.getEquippedStack(EquipmentSlot.HEAD);
        String pattern = TerritoryManager.BannerToString(headStack);
        if (adversaryHeadStack == null || adversaryHeadStack.isEmpty() ||
                (TerritoryManager.IsBanner(adversaryHeadStack) && !TerritoryManager.BannerToString(adversaryHeadStack).equals(pattern))) {
            // players territory should go into decay
            String name = player.getEntityName();
            String cmd = String.format("/tellraw @a [{\"text\":\"[Server] \"},{\"text\":\"%s\",\"color\":\"yellow\"}," +
                    "{\"text\":\" has cowardly fled from battle! Their territory is now open to attack for the next 15 minutes.\",\"color\":\"red\"}]", name);
            TerritoryManager.ExecuteCommand(cmd);
            TerritoryManager.DecayBanner(pattern, name);
        }
    }
}
