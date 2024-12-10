package com.example.playerlogger;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerLogger implements ModInitializer {
    private static final File logFile = new File("player_ips.txt");

    @Override
    public void onInitialize() {
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
            dispatcher.register(CommandManager.literal("alts")
                    .requires(source -> source.hasPermissionLevel(4))
                    .then(CommandManager.argument("query", StringArgumentType.string())
                            .executes(this::handleAltsCommand))
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
                StringBuilder response = new StringBuilder("§bPlayer §3" + query + "§b IPs: §f" + String.join(", ", ips) + "\n");
                for (String ip : ips) {
                    Set<String> players = ipToPlayers.get(ip);
                    response.append("§bUsers joined on §3").append(ip).append("§b: §f").append(String.join(", ", players)).append("\n");
                }
                context.getSource().sendFeedback(() -> Text.literal(response.toString()), false);
            } else {
                context.getSource().sendFeedback(() -> Text.literal("§cNo IPs found for player: " + query), false);
            }
        }

        return 1;
    }
}

