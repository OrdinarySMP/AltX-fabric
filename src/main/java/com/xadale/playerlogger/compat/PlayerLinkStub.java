package com.xadale.playerlogger.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import tronka.justsync.linking.PlayerData;
import tronka.justsync.linking.PlayerLink;

public class PlayerLinkStub {

  private final UUID uuid;
  private final List<UUID> alts;

  public PlayerLinkStub(PlayerLink playerLink) {
    this.uuid = playerLink.getPlayerId();
    this.alts = playerLink.getAlts().stream().map(PlayerData::getId).collect(Collectors.toList());
  }

  public PlayerLinkStub(UUID uuid, List<UUID> altsUuids) {
    this.uuid = uuid;
    this.alts = altsUuids;
  }

  public PlayerLinkStub(UUID uuid) {
    this.uuid = uuid;
    this.alts = new ArrayList<>();
  }

  public UUID getUuid() {
    return uuid;
  }

  public List<UUID> getAlts() {
    return alts;
  }
}
