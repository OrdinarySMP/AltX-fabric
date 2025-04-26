package com.xadale.playerlogger;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.xadale.playerlogger.core.DataRepository;
import com.xadale.playerlogger.data.IpAss;
import com.xadale.playerlogger.repositories.IpAssRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Utils {

  private static final Gson gson = new Gson();

  public static String getPlayerName(UUID uuid) {
    if (PlayerLogger.getInstance().getFloodgateIntegration().isBedrock(uuid)) {
      return PlayerLogger.getInstance().getFloodgateIntegration().getUsername(uuid);
    }
    ProfileResult result =
        PlayerLogger.getInstance().getServer().getSessionService().fetchProfile(uuid, false);
    if (result == null) {
      return "unknown";
    }
    return result.profile().getName();
  }

  public static GameProfile fetchProfile(String name) {
    if (PlayerLogger.getInstance().getFloodgateIntegration().isFloodGateName(name)) {
      return PlayerLogger.getInstance().getFloodgateIntegration().getGameProfileFor(name);
    }
    try {
      return fetchProfileData("https://api.mojang.com/users/profiles/minecraft/" + name);
    } catch (IOException ignored) {
    }
    try {
      return fetchProfileData("https://api.minetools.eu/uuid/" + name);
    } catch (IOException e) {
      return null;
    }
  }

  private static GameProfile fetchProfileData(String urlLink) throws IOException {
    URL url = URI.create(urlLink).toURL();
    URLConnection connection = url.openConnection();
    connection.addRequestProperty("User-Agent", "DiscordJS");
    connection.addRequestProperty("Accept", "application/json");
    connection.connect();
    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    String data = reader.lines().collect(Collectors.joining());
    if (data.endsWith("\"ERR\"}")) {
      return null;
    }
    // fix uuid format
    String fixed =
        data.replaceFirst(
            "\"(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)\"",
            "$1-$2-$3-$4-$5");
    return gson.fromJson(fixed, GameProfile.class);
  }

  public static Set<String> getIpsOfUuid(IpAssRepository ipAssDataRepository, UUID uuid) {
    return ipAssDataRepository
        .getAll()
        .filter(ipAss -> ipAss.getUuids().contains(uuid))
        .map(IpAss::getIp)
        .collect(Collectors.toSet());
  }
}
