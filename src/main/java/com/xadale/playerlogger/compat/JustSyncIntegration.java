package com.xadale.playerlogger.compat;

import com.xadale.playerlogger.PlayerLogger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import net.fabricmc.loader.api.FabricLoader;
import tronka.justsync.JustSyncApplication;
import tronka.justsync.linking.PlayerData;
import tronka.justsync.linking.PlayerLink;

/** Provides integration with Discord: JustSync mod for player UUID linking. */
public class JustSyncIntegration {
  private JustSyncApplication integration;
  private static JustSyncIntegration instance;
  private static boolean loaded = false;

  /** Initializes integration if Discord: JustSync is loaded and enabled in config. */
  private JustSyncIntegration() {
    if (FabricLoader.getInstance().isModLoaded("discordjustsync")
        && PlayerLogger.getInstance().getConfig().discordJustSyncIntegration.enable) {
      if (JustSyncApplication.getInstance() != null) {
        this.integration = JustSyncApplication.getInstance();
      }
    }
    JustSyncIntegration.instance = this;
  }

  /**
   * @return Singleton instance of this integration handler
   */
  public static JustSyncIntegration getIntegration() {
    if (!JustSyncIntegration.loaded) {
      JustSyncIntegration.instance = new JustSyncIntegration();
      JustSyncIntegration.loaded = true;
    }
    return JustSyncIntegration.instance;
  }

  /**
   * @param uuid Player UUID to check
   * @return List of all related UUIDs (main + alts) from JustSync's linking system
   */
  public List<UUID> getRelatedUuids(UUID uuid) {
    if (this.integration != null && this.integration.getConfig().linking.enableLinking) {
      Optional<PlayerLink> playerLink = this.integration.getLinkManager().getDataOf(uuid);
      if (playerLink.isPresent()) {
        // TODO: once implemented change to:
        //  return playerLink.get().getAllUuids();
        List<UUID> uuids =
            playerLink.get().getAlts().stream()
                .map(PlayerData::getId)
                .collect(Collectors.toList());
        uuids.add(playerLink.get().getPlayerId());
        return uuids;
      }
    }

    return List.of(uuid);
  }
}
