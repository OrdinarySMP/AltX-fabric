package com.xadale.playerlogger.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.xadale.playerlogger.PlayerLogger;
import com.xadale.playerlogger.Utils;
import com.xadale.playerlogger.data.IpAss;
import com.xadale.playerlogger.repositories.IpAssRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class HandleAltsCommand {

  private static final Pattern ipPattern = Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}$");

  public static int execute(
      CommandContext<ServerCommandSource> context, IpAssRepository ipAssDataRepository) {
    String query = StringArgumentType.getString(context, "query").trim();

    final ServerCommandSource source = context.getSource();

    if ((query.contains(".") && !Permissions.check(source, "altx.viewips", 4))) {
      context
          .getSource()
          .sendFeedback(
              () -> Text.literal("§cYou do not have permission to search for players using IPs"),
              false);
      return 1;
    }

    Matcher m = ipPattern.matcher(query);
    if (m.matches()) {
      // Query is an IP address
      Optional<IpAss> ipAss = ipAssDataRepository.get(query);
      if (ipAss.isPresent()) {
        Set<String> players =
            ipAss.get().getUuids().stream().map(Utils::getPlayerName).collect(Collectors.toSet());
        context
            .getSource()
            .sendFeedback(
                () ->
                    Text.literal(
                        "§bUsers joined on §3" + query + "§b: §f" + String.join(", ", players)),
                false);
      } else {
        context
            .getSource()
            .sendFeedback(() -> Text.literal("§cNo users found for IP: " + query), false);
      }
      return 1;
    }

    // Query is a username
    ServerPlayerEntity player =
        PlayerLogger.getInstance().getServer().getPlayerManager().getPlayer(query);
    UUID uuid = null;
    if (player == null) {
      GameProfile profile = Utils.fetchProfile(query);
      if (profile != null) {
        uuid = profile.getId();
      }
    } else {
      uuid = player.getUuid();
    }
    if (uuid == null) {
      context.getSource().sendFeedback(() -> Text.literal("§cPlayer not found: " + query), false);
      return 1;
    }
    Set<String> ips = Utils.getIpsOfUuid(ipAssDataRepository, uuid);
    if (!ips.isEmpty()) {
      StringBuilder response = new StringBuilder();
      if (Permissions.check(source, "altx.viewips", 4)) {
        response
            .append("§bPlayer §3")
            .append(query)
            .append("§b IPs: §f")
            .append(String.join(", ", ips));
      } else {
        response.append("§bPlayers with the same IP as §3").append(query).append("§b:");
      }
      for (String ip : ips) {
        Set<String> players =
            ipAssDataRepository.get(ip).get().getUuids().stream()
                .map(Utils::getPlayerName)
                .collect(Collectors.toSet());

        response.append("\n");
        if (Permissions.check(source, "altx.viewips", 4)) {
          response.append("§3- (§b").append(ip).append("§3): §f");
        } else {
          response.append("§3- §f");
        }
        response.append(String.join(", ", players));
      }
      context.getSource().sendFeedback(() -> Text.literal(response.toString()), false);
    } else {
      context
          .getSource()
          .sendFeedback(() -> Text.literal("§cNo IPs found for player: " + query), false);
    }
    return 1;
  }
}
