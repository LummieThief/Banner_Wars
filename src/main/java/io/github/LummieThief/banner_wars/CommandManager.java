package io.github.LummieThief.banner_wars;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class CommandManager {
    public static int kmsCommand(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getPlayer() != null) {
            ServerPlayerEntity player = context.getSource().getPlayer();
            ItemStack headStack = player.getEquippedStack(EquipmentSlot.HEAD);
            if (TerritoryManager.IsBanner(headStack) && TerritoryManager.HasPattern(headStack)) {
                String pattern = TerritoryManager.BannerToString(headStack);
                assert pattern != null; // we just checked isBanner and HasPattern.
                if (TerritoryManager.DecayBanner(pattern, player.getEntityName())) {
                    String cmd = String.format("/tellraw @a [{\"text\":\"[Server] \"},{\"text\":\"%s\",\"color\":\"yellow\"}," +
                            "{\"text\":\" has chosen their own fate. Their territory is now open to attack for the next 15 minutes.\",\"color\":\"red\"}]", player.getEntityName());
                    ExecuteCommand(cmd);
                }

            }
            player.kill();
        }
        return 1;
    }

    public static int statusCommand(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getPlayer() != null) {
            ServerPlayerEntity player = context.getSource().getPlayer();
            player.sendMessage(Text.literal("[Server] ------------------------------"));
            ItemStack headStack = player.getEquippedStack(EquipmentSlot.HEAD);
            if (TerritoryManager.IsBanner(headStack) && TerritoryManager.HasPattern(headStack)) {
                String pattern = TerritoryManager.BannerToString(headStack);
                assert pattern != null; // we just checked isBanner and HasPattern.

                if (TerritoryManager.state == null)
                    return 1;
                List<ChunkData> chunks = TerritoryManager.state.PeekTerritoryData(pattern, 5);
                int lastLivingEpoch = TerritoryManager.serverTickHandler.getEpoch() - ServerTickHandler.CLAIM_LIFETIME;
                float secondsPerEpoch = ServerTickHandler.TICKS_PER_EPOCH / 20f;
                if (chunks == null || chunks.size() == 0) {
                    Text noText = Text.literal("No banners found in territory.").setStyle(Style.EMPTY.withColor(TextColor.parse("gray")).withItalic(true));
                    player.sendMessage(noText);
                }
                else {
                    for (ChunkData data : chunks) {
                        int epochsLeft = data.epoch() - lastLivingEpoch + 1;
                        float hoursLeft = epochsLeft * secondsPerEpoch / 3600;
                        float percentage = (100f * epochsLeft) / (ServerTickHandler.CLAIM_LIFETIME + 1);
                        BlockPos pos = TerritoryManager.DecodePosition(data.bannerPos());
                        String posString = pos.toShortString();
                        String color;
                        if (percentage <= 25)
                            color = "red";
                        else if (percentage <= 50)
                            color = "gold";
                        else
                            color = "green";
                        Text bannerText = Text.literal(String.format("Banner at (" + posString + ") : ")).setStyle(Style.EMPTY.withColor(TextColor.parse("gray")).withItalic(true));
                        Text timeRemainingText = Text.literal(String.format("%.1f%% (%.2f hours)", percentage, hoursLeft)).setStyle(Style.EMPTY.withColor(TextColor.parse(color)));
                        player.sendMessage(bannerText.copy().append(timeRemainingText));
                    }
                }
            }
            else {
                Text noText = Text.literal("No banner equipped.").setStyle(Style.EMPTY.withColor(TextColor.parse("gray")).withItalic(true));
                player.sendMessage(noText);
            }
        }
        return 1;
    }

    public static void ExecuteCommand(String cmd) {
        MinecraftServer server = TerritoryManager.server;
        server.getCommandManager().executeWithPrefix(server.getCommandSource(), cmd);
    }
}
