package com.xadale.playerlogger.data;

import com.xadale.playerlogger.core.AbstractData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Notif extends AbstractData<Notif, Integer> {

  private int id;
  private UUID uuid;
  private List<UUID> alts;
  private LocalDateTime time;

  public Notif(int id, UUID uuid, List<UUID> alts, LocalDateTime time) {
    this.id = id;
    this.uuid = uuid;
    this.alts = alts;
    this.time = time;
  }

  public int getId() {
    return id;
  }

  public UUID getUuid() {
    return uuid;
  }

  public List<UUID> getAlts() {
    return alts;
  }

  public LocalDateTime getTime() {
    return time;
  }
}
