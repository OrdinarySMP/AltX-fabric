package com.xadale.playerlogger.commands;

import com.mojang.brigadier.context.CommandContext;
import com.xadale.playerlogger.PlayerLogger;
import com.xadale.playerlogger.Utils;
import com.xadale.playerlogger.compat.JustSyncIntegration;
import com.xadale.playerlogger.data.AuthorizedAccounts;
import com.xadale.playerlogger.data.IpAss;
import com.xadale.playerlogger.repositories.IpAssRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ListIpsWithMultiplePlayers {
  public static int execute(
      CommandContext<ServerCommandSource> context, IpAssRepository ipAssDataRepository) {

    final ServerCommandSource source = context.getSource();

    StringBuilder response = new StringBuilder();

    CompletableFuture.runAsync(
        () -> {
          List<IpAss> multiAccountIps =
              ipAssDataRepository
                  .getAll()
                  .filter(ipAss -> ipAss.getUuids().size() >= 2)
                  .filter(Predicate.not(ListIpsWithMultiplePlayers::isAuthorized))
                  .toList();

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
        });
    return 1;
  }

  private static boolean isAuthorized(IpAss ipass) {
    Set<UUID> uuidSet = new HashSet<>();
    for (UUID uuid : ipass.getUuids()) {
      uuidSet.add(JustSyncIntegration.getIntegration().getPlayerLink(uuid).getUuid());
    }

    // only registered alts from justsync
    if (uuidSet.size() <= 1) {
      return true;
    }

    Optional<AuthorizedAccounts> authorizedAccounts =
        PlayerLogger.getInstance().getAuthorizedAccountsRepository().get(uuidSet.iterator().next());

    // no authorized and multiple accounts on ip
    if (authorizedAccounts.isEmpty()) {
      return false;
    }

    // authorized when all uuids are in authorization object
    return new HashSet<>(authorizedAccounts.get().getUuids()).containsAll(uuidSet);
  }
}
