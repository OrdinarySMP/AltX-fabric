package com.xadale.playerlogger;

import com.xadale.playerlogger.data.IpAss;
import com.xadale.playerlogger.repositories.IpAssRepository;
import java.util.Optional;
import java.util.UUID;

public class IpLogger {

  public static void handleJoin(String ip, UUID uuid) {
    IpAssRepository ipAssDataRepository = PlayerLogger.getInstance().getIpAssRepository();
    Optional<IpAss> ipAss = ipAssDataRepository.get(ip);
    if (ipAss.isEmpty()) {
      ipAssDataRepository.add(new IpAss(ip, uuid));
      return;
    }
    ipAss.get().addUUid(uuid);
    NotificationHandler.handleNotif(ipAss.get(), uuid);
  }
}
