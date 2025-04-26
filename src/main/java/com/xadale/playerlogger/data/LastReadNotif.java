package com.xadale.playerlogger.data;

import com.xadale.playerlogger.core.AbstractData;
import java.util.UUID;

public class LastReadNotif extends AbstractData<LastReadNotif, UUID> {

  private UUID uuid;
  private int lastNotifId;

  public LastReadNotif() {}

  public LastReadNotif(UUID uuid, int lastNotifId) {
    this.uuid = uuid;
    this.lastNotifId = lastNotifId;
  }

  public UUID getUuid() {
    return uuid;
  }

  public int getLastNotifId() {
    return lastNotifId;
  }

  public void setLastNotifId(int lastNotifId) {
    this.lastNotifId = lastNotifId;
    this.getRepository().update(this);
  }
}
