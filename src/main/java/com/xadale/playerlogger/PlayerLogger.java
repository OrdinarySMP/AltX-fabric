package com.xadale.playerlogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class PlayerLogger implements ModInitializer {

  private File logFile;

  @Override
  public void onInitialize() {
    File logDir = new File("AltX-Files");
    if (!logDir.exists()) {
      logDir.mkdir(); // Creates the folder if it doesn't exist
    }
    this.logFile = new File(logDir, "player_ips.txt");
    // Register connection event to log player IPs
    ServerPlayConnectionEvents.JOIN.register(
        (handler, sender, server) -> {
          String playerName = handler.getPlayer().getName().getString();

          String ipAddress = "Unknown IP";
          if (handler.getConnectionAddress() instanceof InetSocketAddress inetSocketAddress) {
            ipAddress = inetSocketAddress.getAddress().getHostAddress();
          }

          String logEntry = playerName + ";" + ipAddress;
          writeToFileIfAbsent(logEntry);
        });

    Commands commands = new Commands(this);
    commands.register();
  }

  private void writeToFileIfAbsent(String entry) {
    try {
      if (!logFile.exists()) {
        logFile.createNewFile();
      }

      boolean alreadyLogged = false;
      try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.trim().equals(entry)) {
            alreadyLogged = true;
            break;
          }
        }
      }

      if (!alreadyLogged) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
          writer.write(entry + "\n");
        }
      }
    } catch (IOException e) {
      System.err.println("Failed to write player log: " + e.getMessage());
    }
  }

  public File getLogFile() {
    return this.logFile;
  }
}
