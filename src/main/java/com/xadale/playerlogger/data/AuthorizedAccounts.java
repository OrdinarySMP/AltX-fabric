package com.xadale.playerlogger.data;

import com.xadale.playerlogger.core.AbstractData;
import java.util.List;
import java.util.UUID;

public class AuthorizedAccounts extends AbstractData<AuthorizedAccounts, UUID> {

  private List<UUID> uuids;

  public AuthorizedAccounts(List<UUID> uuids) {
    this.uuids = uuids;
  }

  public List<UUID> getUuids() {
    return uuids;
  }
}
