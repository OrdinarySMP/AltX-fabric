package com.xadale.playerlogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class PlayerLogger implements ModInitializer {

    private File logFile;

    @Override
    public void onInitialize() {
        File logDir = new File("AltX-Files");
        if (!logDir.exists()) {
            logDir.mkdir();  // Creates the folder if it doesn't exist
        }
        this.logFile = new File(logDir, "player_ips.txt");
        // Register connection event to log player IPs
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            String playerName = handler.getPlayer().getName().getString();

            String ipAddress = "Unknown IP";
            if (handler.getConnectionAddress() instanceof InetSocketAddress inetSocketAddress) {
                ipAddress = inetSocketAddress.getAddress().getHostAddress();
            }

            String logEntry = playerName + ";" + ipAddress;
            writeToFileIfAbsent(logEntry);
        });

        // Register the command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Root command AltX
            dispatcher.register(CommandManager.literal("altx")
                .requires(Permissions.require("altx.command", 4))
                .executes(context -> {
                    context.getSource().sendFeedback(() -> Text.literal("§bAltX by MrCookiePrincess\n§3Commands:\n§b/altx §fShows a list of available AltX commands"), false);
                    final ServerCommandSource source = context.getSource();
                    final ServerPlayerEntity player = source.getPlayer();
                    if (Permissions.check(source, "altx.list", 4)) {
                        context.getSource().sendFeedback(() -> Text.literal("§b/altx list §fShows a list of players using the same IP address"), false);
                    }
                    if (Permissions.check(source, "altx.trace", 4)) {
                        context.getSource().sendFeedback(() -> Text.literal("§b/altx trace <player> §fShows all players on given players IP address"), false);
                        if (Permissions.check(source, "altx.viewips", 4)) {
                            context.getSource().sendFeedback(() -> Text.literal("§b/altx trace <ip> §fShows all players on given IP address"), false);
                        }
                    }
                    return 1; // Return success
                })
                // Command Trace
                .then(CommandManager.literal("trace")
                .requires(Permissions.require("altx.trace", 4))
                    .then(CommandManager.argument("query", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            String partialQuery = builder.getRemaining(); // Get the current typed string
                            for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
                                String playerName = player.getName().getString();
                                if (playerName.toLowerCase().startsWith(partialQuery.toLowerCase())) {
                                    builder.suggest(playerName);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(this::handleAltsCommand))
                )
                // Command list
                .then(CommandManager.literal("list")
                .requires(Permissions.require("altx.list", 4))
                .executes(this::listIpsWithMultiplePlayers)
                )
            );
        });
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

    private int handleAltsCommand(CommandContext<ServerCommandSource> context) {

        String query = StringArgumentType.getString(context, "query").trim();
        Map<String, Set<String>> ipToPlayers = new HashMap<>();
        Map<String, Set<String>> playerToIps = new HashMap<>();

        // check if show ips
        final ServerCommandSource source = context.getSource();
        final ServerPlayerEntity player = source.getPlayer();
        if (Permissions.check(source, "altx.viewips", 4)) {
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(";");
                    if (parts.length == 2) {
                        String playerName = parts[0].trim();
                        String ipAddress = parts[1].trim();

                        ipToPlayers.computeIfAbsent(ipAddress, k -> new HashSet<>()).add(playerName);
                        playerToIps.computeIfAbsent(playerName, k -> new HashSet<>()).add(ipAddress);
                    }
                }
            } catch (IOException e) {
                context.getSource().sendError(Text.literal("§cFailed to read log file: " + e.getMessage()));
                return 0;
            }

            if (query.contains(".")) {
                // Query is an IP address
                Set<String> players = ipToPlayers.get(query);
                if (players != null) {
                    context.getSource().sendFeedback(() -> Text.literal("§bUsers joined on §3" + query + "§b: §f" + String.join(", ", players)), false);
                } else {
                    context.getSource().sendFeedback(() -> Text.literal("§cNo users found for IP: " + query), false);
                }
            } else {
                // Query is a username
                Set<String> ips = playerToIps.get(query);
                if (ips != null) {
                    StringBuilder response = new StringBuilder("§bPlayer §3" + query + "§b IPs: §f" + String.join(", ", ips));
                    for (String ip : ips) {
                        Set<String> players = ipToPlayers.get(ip);
                        response.append("\n").append("§3- (§b").append(ip).append("§3): §f").append(String.join(", ", players));
                    }
                    context.getSource().sendFeedback(() -> Text.literal(response.toString()), false);
                } else {
                    context.getSource().sendFeedback(() -> Text.literal("§cNo IPs found for player: " + query), false);
                }
            }
        } else {
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(";");
                    if (parts.length == 2) {
                        String playerName = parts[0].trim();
                        String ipAddress = parts[1].trim();

                        ipToPlayers.computeIfAbsent(ipAddress, k -> new HashSet<>()).add(playerName);
                        playerToIps.computeIfAbsent(playerName, k -> new HashSet<>()).add(ipAddress);
                    }
                }
            } catch (IOException e) {
                context.getSource().sendError(Text.literal("§cFailed to read log file: " + e.getMessage()));
                return 0;
            }

            if (query.contains(".")) {
                // Query is an IP address
                context.getSource().sendFeedback(() -> Text.literal("§cYou do not have permission to search for players using IPs"), false);
            } else {
                // Query is a username
                Set<String> ips = playerToIps.get(query);
                if (ips != null) {
                    StringBuilder response = new StringBuilder("§bPlayers with the same IP as §3" + query + "§b:");
                    for (String ip : ips) {
                        Set<String> players = ipToPlayers.get(ip);
                        response.append("\n").append("§3- §f").append(String.join(", ", players));
                    }
                    context.getSource().sendFeedback(() -> Text.literal(response.toString()), false);
                } else {
                    context.getSource().sendFeedback(() -> Text.literal("§cPlayer " + query + " §cnot found"), false);
                }
            }
        }
        return 1;
    };

    private int listIpsWithMultiplePlayers(CommandContext<ServerCommandSource> context) {
        Map<String, Set<String>> ipToPlayers = new HashMap<>();

        // check if show ips
        final ServerCommandSource source = context.getSource();
        final ServerPlayerEntity player = source.getPlayer();
        if (Permissions.check(source, "altx.viewips", 4)) {
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(";");
                    if (parts.length == 2) {
                        String playerName = parts[0].trim();
                        String ipAddress = parts[1].trim();

                        ipToPlayers.computeIfAbsent(ipAddress, k -> new HashSet<>()).add(playerName);
                    }
                }
            } catch (IOException e) {
                context.getSource().sendError(Text.literal("§cFailed to read log file: " + e.getMessage()));
                return 0;
            }

            // Filter and build the result
            StringBuilder response = new StringBuilder("§bIPs with two or more users:");
            boolean found = false;

            for (Map.Entry<String, Set<String>> entry : ipToPlayers.entrySet()) {
                if (entry.getValue().size() >= 2) {
                    found = true;
                    response.append("\n").append("§3- (§b").append(entry.getKey()).append("§3): §f").append(String.join(", ", entry.getValue()));
                }
            }

            if (!found) {
                response.append("§cNo IPs with two or more players found.");
            }

            // Send the response
            context.getSource().sendFeedback(() -> Text.literal(response.toString()), false);

            return 1; // Indicate success
        } else {

            // Load the IP-to-players map from the log file
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(";");
                    if (parts.length == 2) {
                        String playerName = parts[0].trim();
                        String ipAddress = parts[1].trim();

                        ipToPlayers.computeIfAbsent(ipAddress, k -> new HashSet<>()).add(playerName);
                    }
                }
            } catch (IOException e) {
                context.getSource().sendError(Text.literal("§cFailed to read log file: " + e.getMessage()));
                return 0;
            }

            // Filter and build the result
            StringBuilder response = new StringBuilder("§bIPs with two or more users:");
            boolean found = false;

            for (Map.Entry<String, Set<String>> entry : ipToPlayers.entrySet()) {
                if (entry.getValue().size() >= 2) {
                    found = true;
                    response.append("\n").append("§3- §f").append(String.join(", ", entry.getValue()));
                }
            }

            if (!found) {
                response.append("§cNo IPs with two or more players found.");
            }

            // Send the response
            context.getSource().sendFeedback(() -> Text.literal(response.toString()), false);

            return 1; // Indicate success
        }
    }
}
