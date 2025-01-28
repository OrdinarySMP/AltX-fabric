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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import me.lucko.fabric.api.permissions.v0.Permissions;

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

          // Only proceed if the log entry is new
          if (writeToFileIfAbsent(logEntry)) {
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
              StringBuilder potentialAlts = new StringBuilder();
              String line;
              while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length == 2
                    && parts[1].equals(ipAddress)
                    && !parts[0].equals(playerName)) {
                  if (potentialAlts.length() > 0) {
                    potentialAlts.append(", "); // Add a comma between names
                  }
                  potentialAlts.append(parts[0]);
                }
              }

              if (potentialAlts.length() > 0) {
                // Found one or more potential alts
                String message =
                    "Player " + playerName + " is a potential alt of: " + potentialAlts + ".";

                // Send the message to staff
                server
                    .getPlayerManager()
                    .getPlayerList()
                    .forEach(
                        player -> {
                          if (Permissions.check(player, "altx.notify", 4)) {
                            player.sendMessage(
                                Text.literal(message).formatted(Formatting.RED), false);
                          }
                        });
              }
            } catch (IOException e) {
              System.err.println("Failed to read log file: " + e.getMessage());
            }
          }
        });

    Commands commands = new Commands(this);
    commands.register();
  }

  private boolean writeToFileIfAbsent(String entry) {
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
        return true; // New entry added
      }
    } catch (IOException e) {
      System.err.println("Failed to write player log: " + e.getMessage());
    }
    return false; // No new entry added or an exception occurred
  }

  public File getLogFile() {
    return this.logFile;
  }
}
