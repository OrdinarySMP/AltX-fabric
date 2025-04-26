package com.xadale.playerlogger;

import com.xadale.playerlogger.data.IpAss;
import com.xadale.playerlogger.repositories.IpAssRepository;
import java.util.Optional;
import java.util.UUID;

public class IpLogger {

  public static void handleJoin(String ip, UUID uuid) {
    IpAssRepository ipAssdataRepository = PlayerLogger.getInstance().getIpAssRepository();
    Optional<IpAss> ipAss = ipAssdataRepository.get(ip);
    if (ipAss.isEmpty()) {
      ipAssdataRepository.add(new IpAss(ip, uuid));
      return;
    }
    ipAss.get().addUUid(uuid);
    NotificationHandler.handleNotif(ipAss.get(), uuid);
  }
}
