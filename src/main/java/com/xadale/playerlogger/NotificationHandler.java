package com.xadale.playerlogger;

import com.xadale.playerlogger.compat.JustSyncIntegration;
import com.xadale.playerlogger.data.AuthorizedAccounts;
import com.xadale.playerlogger.data.IpAss;
import com.xadale.playerlogger.data.LastReadNotif;
import com.xadale.playerlogger.data.Notif;
import com.xadale.playerlogger.repositories.AuthorizedAccountsRepository;
import com.xadale.playerlogger.repositories.NotifRepository;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class NotificationHandler {

  public static void handleNotif(IpAss ipAss, UUID uuid) {
    if (!PlayerLogger.getInstance().getConfig().altNotifs.enableNotifs) {
      return;
    }
    List<UUID> uuids = ipAss.getUuids();
    List<UUID> linkedUuids = JustSyncIntegration.getIntegration().getRelatedUuids(uuid);

    AuthorizedAccountsRepository authAltsRepo =
        PlayerLogger.getInstance().getAuthorizedAccountsRepository();
    Optional<AuthorizedAccounts> authorizedAccountsOptional = authAltsRepo.get(uuid);
    List<UUID> filteredUuids = new ArrayList<>();
    if (authorizedAccountsOptional.isEmpty()) {
      filteredUuids = uuids.stream().filter(u -> !linkedUuids.contains(u)).toList();
    } else {
      List<UUID> authorizedAccounts = authorizedAccountsOptional.get().getUuids();
      filteredUuids =
          uuids.stream()
              .filter(u -> !linkedUuids.contains(u))
              .filter(u -> !authorizedAccounts.contains(u))
              .toList();
    }
    if (filteredUuids.isEmpty()) {
      return;
    }
    String message = getMessage(uuid, filteredUuids);

    PlayerLogger.getInstance()
        .getServer()
        .getPlayerManager()
        .getPlayerList()
        .forEach(
            player -> {
              if (Permissions.check(player, "altx.notify", 4)) {
                player.sendMessage(Text.literal(message).formatted(Formatting.RED), false);
                PlayerLogger.getInstance()
                    .getLastReadNotifRepository()
                    .get(player.getUuid())
                    // TODO: CHANGEEEEE
                    .get()
                    .setLastNotifId(getLastNotifId() + 1);
                ;
              }
            });

    String notificationLog =
        PlayerLogger.getConfigFolder()
            .resolve(PlayerLogger.modId + ".notifications.log")
            .toString();
    try (BufferedWriter output = new BufferedWriter(new FileWriter(notificationLog, true))) {
      output.append(LocalDateTime.now().toString()).append(String.valueOf(' '));
      output.append(message);
      output.newLine();
    } catch (IOException ignored) {
    }

    Notif notif = new Notif(getLastNotifId() + 1, uuid, filteredUuids, LocalDateTime.now());
    PlayerLogger.getInstance().getNotifRepository().add(notif);
  }

  public static void onJoin(
      ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
    ServerPlayerEntity player = handler.getPlayer();

    // has permission for notifications
    if (!Permissions.check(player, "altx.notify", 4)) {
      return;
    }

    // if not already seen notifications before (joined with permission before)
    // create last read tracking object
    Optional<LastReadNotif> lastReadNotif =
        PlayerLogger.getInstance().getLastReadNotifRepository().get(player.getUuid());
    if (lastReadNotif.isEmpty()) {
      LastReadNotif newLastReadNotif = new LastReadNotif(player.getUuid(), getLastNotifId());
      PlayerLogger.getInstance().getLastReadNotifRepository().add(newLastReadNotif);
      return;
    }

    if (!PlayerLogger.getInstance().getConfig().altNotifs.showMissedNotifsOnJoin) {
      return;
    }

    // show missed notifications
    List<Notif> missedNotifs =
        getMissedNotifs(
            lastReadNotif.get().getLastNotifId(),
            PlayerLogger.getInstance().getConfig().altNotifs.notificationPeriod);
    for (Notif notif : missedNotifs) {
      player.sendMessage(
          Text.literal(getMessage(notif.getUuid(), notif.getAlts())).formatted(Formatting.RED),
          false);
    }

    lastReadNotif.get().setLastNotifId(getLastNotifId());
  }

  public static List<Notif> getMissedNotifs(long afterId, int hours) {
    LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
    NotifRepository repository = PlayerLogger.getInstance().getNotifRepository();

    return repository
        .getAll()
        .filter(notif -> notif.getId() > afterId)
        .filter(notif -> notif.getTime() != null && !notif.getTime().isBefore(cutoff))
        .toList();
  }

  public static String getMessage(UUID uuid, List<UUID> uuids) {
    return "Player "
        + Utils.getPlayerName(uuid)
        + " is a potential alt of: "
        + uuids.stream().map(Utils::getPlayerName).toList()
        + ".";
  }

  public static int getLastNotifId() {
    return PlayerLogger.getInstance()
        .getNotifRepository()
        .getAll()
        .mapToInt(Notif::getId)
        .max()
        .orElse(0);
  }

  public static void purgeOldNotifs(int hours) {
    LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
    NotifRepository repository = PlayerLogger.getInstance().getNotifRepository();

    Optional<Notif> newestNotifOptional = repository.get(getLastNotifId());

    if (newestNotifOptional.isEmpty()) {
      return;
    }

    Notif newestNotif = newestNotifOptional.get();

    List<Notif> toRemove =
        repository
            .getAll()
            .filter(notif -> notif != newestNotif)
            .filter(notif -> notif.getTime() != null && notif.getTime().isBefore(cutoff))
            .toList();

    toRemove.forEach(repository::remove);
  }
}
