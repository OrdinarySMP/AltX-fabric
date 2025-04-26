package com.xadale.playerlogger.config;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlComment;
import com.moandjiezana.toml.TomlWriter;
import com.xadale.playerlogger.PlayerLogger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {

  public static Config loadConfig() {
    Path configDir = PlayerLogger.getConfigFolder();
    File configFile = configDir.resolve(PlayerLogger.modId + ".toml").toFile();
    Config instance;
    if (configFile.exists()) {
      instance = new Toml().read(configFile).to(Config.class);
    } else {
      instance = new Config();
    }
    upgradeConfig(instance);
    try {
      Files.createDirectories(configDir);
      new TomlWriter().write(instance, configFile);
    } catch (IOException ignored) {
    }
    return instance;
  }

  // config migration if needed
  private static void upgradeConfig(Config instance) {
    ;
  }

  public AltNotifs altNotifs = new AltNotifs();
  @TomlComment({"------------------------------",
    "",
  "------------------------------"})
  public DiscordJustSyncIntegration discordJustSyncIntegration = new DiscordJustSyncIntegration();

  public static class DiscordJustSyncIntegration {
    @TomlComment({
      "check if account on same ip as other account is linked as alt",
      "only relevant if alt notifs are enabled and Discord: JustSync is installed and the alt"
          + " feature is in use"
    })
    public boolean enable = false;
  }

  public static class AltNotifs {
    @TomlComment({
      "get a notification in chat when an account",
      "joins with the same ip as another",
      "players to notify are determined by 'altx.notify' permission or op level 4"
    })
    public boolean enableNotifs = true;

    @TomlComment("show notifs issued when player was offline")
    public boolean showMissedNotifsOnJoin = true;

    @TomlComment("oldest notification to show in hours")
    public int notificationPeriod = 24;

    @TomlComment(
        "period after which notifications will only be accessible through the respective file not"
            + " ingame")
    public int purgePeriod = 72;
  }
}
