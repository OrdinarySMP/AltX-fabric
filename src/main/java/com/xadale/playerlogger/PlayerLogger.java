package com.xadale.playerlogger;

import com.mojang.logging.LogUtils;
import com.xadale.playerlogger.compat.FloodgateIntegration;
import com.xadale.playerlogger.config.Config;
import com.xadale.playerlogger.repositories.AuthorizedAccountsRepository;
import com.xadale.playerlogger.repositories.IpAssRepository;
import com.xadale.playerlogger.repositories.LastReadNotifRepository;
import com.xadale.playerlogger.repositories.NotifRepository;
import com.xadale.playerlogger.repositories.implementations.JsonAuthorizedAccountsRepositoryImpl;
import com.xadale.playerlogger.repositories.implementations.JsonIpAssRepositoryImpl;
import com.xadale.playerlogger.repositories.implementations.JsonLastReadNotifRepositoryImpl;
import com.xadale.playerlogger.repositories.implementations.JsonNotifRepositoryImpl;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

public class PlayerLogger implements ModInitializer {

  public static String modId = "altx";
  private static PlayerLogger instance;
  private MinecraftServer server;
  private FloodgateIntegration floodgateIntegration;
  private IpAssRepository ipAssDataRepository;
  private NotifRepository notifRepository;
  private LastReadNotifRepository lastReadNotifRepository;
  private AuthorizedAccountsRepository authorizedAccountsRepository;
  private Config config = Config.loadConfig();

  @Override
  public void onInitialize() {
    PlayerLogger.instance = this;

    ServerLifecycleEvents.SERVER_STARTING.register(s -> this.server = s);
    this.floodgateIntegration = new FloodgateIntegration();

    File ipAssFile =
        PlayerLogger.getConfigFolder().resolve(PlayerLogger.modId + ".ips.json").toFile();
    File notifFile =
        PlayerLogger.getConfigFolder().resolve(PlayerLogger.modId + ".notifs.json").toFile();
    File lastReadNotifFile =
        PlayerLogger.getConfigFolder().resolve(PlayerLogger.modId + ".lastReadNotif.json").toFile();
    File authorizedAccountsFile =
        PlayerLogger.getConfigFolder()
            .resolve(PlayerLogger.modId + ".authorizedAccounts.json")
            .toFile();

    try {
      Files.createDirectories(PlayerLogger.getConfigFolder());
    } catch (IOException ignored) {
    }

    this.ipAssDataRepository = JsonIpAssRepositoryImpl.from(ipAssFile);
    this.notifRepository = JsonNotifRepositoryImpl.from(notifFile);
    this.lastReadNotifRepository = JsonLastReadNotifRepositoryImpl.from(lastReadNotifFile);
    this.authorizedAccountsRepository =
        JsonAuthorizedAccountsRepositoryImpl.from(authorizedAccountsFile);

    ServerPlayConnectionEvents.JOIN.register(NotificationHandler::onJoin);

    NotificationHandler.purgeOldNotifs(this.config.altNotifs.purgePeriod);

    Commands commands = new Commands();
    commands.register();
  }

  public void reloadConfig() {
    LogUtils.getLogger().info("Reloading Config");
    this.config = Config.loadConfig();
    LogUtils.getLogger().info("Config succesfully reloaded");
  }

  public static Path getConfigFolder() {
    return FabricLoader.getInstance().getConfigDir().resolve(PlayerLogger.modId);
  }

  public static PlayerLogger getInstance() {
    return PlayerLogger.instance;
  }

  public MinecraftServer getServer() {
    return this.server;
  }

  public FloodgateIntegration getFloodgateIntegration() {
    return this.floodgateIntegration;
  }

  public IpAssRepository getIpAssRepository() {
    return this.ipAssDataRepository;
  }

  public NotifRepository getNotifRepository() {
    return this.notifRepository;
  }

  public LastReadNotifRepository getLastReadNotifRepository() {
    return this.lastReadNotifRepository;
  }

  public AuthorizedAccountsRepository getAuthorizedAccountsRepository() {
    return this.authorizedAccountsRepository;
  }

  public Config getConfig() {
    return this.config;
  }
}
