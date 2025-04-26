package com.xadale.playerlogger.commands;

import com.mojang.brigadier.context.CommandContext;
import com.xadale.playerlogger.Utils;
import com.xadale.playerlogger.data.IpAss;
import com.xadale.playerlogger.repositories.IpAssRepository;
import java.util.List;
import java.util.stream.Collectors;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ListIpsWithMultiplePlayers {
  public static int execute(
      CommandContext<ServerCommandSource> context, IpAssRepository ipAssDataRepository) {

    final ServerCommandSource source = context.getSource();

    StringBuilder response = new StringBuilder();

    List<IpAss> multiAccountIps =
        ipAssDataRepository.getAll().filter(ipAss -> ipAss.getUuids().size() >= 2).toList();

    if (multiAccountIps.isEmpty()) {
      response.append("§cNo IPs with two or more players found.");
    } else {
      multiAccountIps.forEach(
          ipAss -> {
            response.append("\n");

            if (Permissions.check(source, "altx.viewips", 4)) {
              response.append("§3- (§b").append(ipAss.getIp()).append("§3): §f");
            } else {
              response.append("§3- §f");
            }

            String playerNames =
                ipAss.getUuids().stream()
                    .map(Utils::getPlayerName)
                    .collect(Collectors.joining(", "));

            response.append(playerNames);
          });
    }

    if (!multiAccountIps.isEmpty()) {
      response.insert(0, "§bIPs with two or more users:");
    }

    source.sendFeedback(() -> Text.literal(response.toString()), false);

    return 1;
  }
}
