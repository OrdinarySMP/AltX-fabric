package com.xadale.playerlogger.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class HandleAltsCommand {
  public static int execute(CommandContext<ServerCommandSource> context, File logFile) {
    String query = StringArgumentType.getString(context, "query").trim();
    Map<String, Set<String>> ipToPlayers = new HashMap<>();
    Map<String, Set<String>> playerToIps = new HashMap<>();

    final ServerCommandSource source = context.getSource();

    try {
      HandleAltsCommand.readLogFile(ipToPlayers, playerToIps, logFile);
    } catch (IOException e) {
      context.getSource().sendError(Text.literal("§cFailed to read log file: " + e.getMessage()));
      return 0;
    }

    if ((query.contains(".") && !Permissions.check(source, "altx.viewips", 4))) {
      context
          .getSource()
          .sendFeedback(
              () -> Text.literal("§cYou do not have permission to search for players using IPs"),
              false);
      return 1;
    }

    if (query.contains(".")) {
      // Query is an IP address
      Set<String> players = ipToPlayers.get(query);
      if (players != null) {
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
    Set<String> ips = playerToIps.get(query);
    if (ips != null) {
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
        Set<String> players = ipToPlayers.get(ip);

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

  private static void readLogFile(
      Map<String, Set<String>> ipToPlayers, Map<String, Set<String>> playerToIps, File logFile)
      throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(logFile));

    String line;
    while ((line = reader.readLine()) != null) {
      String[] parts = line.split(";");
      if (parts.length == 2) {
        String playerName = parts[0].trim();
        String ipAddress = parts[1].trim();

        ipToPlayers.computeIfAbsent(ipAddress, k -> new HashSet<>()).add(playerName);

        playerToIps.computeIfAbsent(playerName, k -> new HashSet<>()).add(ipAddress);
      }
    }

    reader.close();
  }
}
