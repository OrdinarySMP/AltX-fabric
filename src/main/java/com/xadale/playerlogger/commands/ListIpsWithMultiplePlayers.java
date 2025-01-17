package com.xadale.playerlogger.commands;

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

public class ListIpsWithMultiplePlayers {
  public static int execute(CommandContext<ServerCommandSource> context, File logFile) {
    Map<String, Set<String>> ipToPlayers = new HashMap<>();

    // check if show ips
    final ServerCommandSource source = context.getSource();

    try {
      ListIpsWithMultiplePlayers.readLogFile(logFile, ipToPlayers);
    } catch (IOException e) {
      context.getSource().sendError(Text.literal("§cFailed to read log file: " + e.getMessage()));
      return 0;
    }

    // Filter and build the result
    StringBuilder response = new StringBuilder("§bIPs with two or more users:");
    boolean found = false;

    for (Map.Entry<String, Set<String>> entry : ipToPlayers.entrySet()) {
      if (entry.getValue().size() >= 2) {
        found = true;
        response.append("\n");
        if (Permissions.check(source, "altx.viewips", 4)) {
          response.append("§3- (§b").append(entry.getKey()).append("§3): §f");
        } else {
          response.append("§3- §f");
        }
        response.append(String.join(", ", entry.getValue()));
      }
    }

    if (!found) {
      response.append("§cNo IPs with two or more players found.");
    }

    // Send the response
    context.getSource().sendFeedback(() -> Text.literal(response.toString()), false);

    return 1;
  }

  private static void readLogFile(File logFile, Map<String, Set<String>> ipToPlayers)
      throws IOException {
    // Load the IP-to-players map from the log file
    BufferedReader reader = new BufferedReader(new FileReader(logFile));
    String line;
    while ((line = reader.readLine()) != null) {
      String[] parts = line.split(";");
      if (parts.length == 2) {
        String playerName = parts[0].trim();
        String ipAddress = parts[1].trim();

        ipToPlayers.computeIfAbsent(ipAddress, k -> new HashSet<>()).add(playerName);
      }
    }
  }
}
