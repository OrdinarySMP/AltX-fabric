package com.xadale.playerlogger.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.xadale.playerlogger.PlayerLogger;
import com.xadale.playerlogger.Utils;
import com.xadale.playerlogger.data.AuthorizedAccounts;
import java.util.List;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class AuthorizeAltsCommand {

  public static int execute(CommandContext<ServerCommandSource> context) {
    String playerString1 = StringArgumentType.getString(context, "player1").trim();
    String playerString2 = StringArgumentType.getString(context, "player2").trim();

    GameProfile profile1 = Utils.fetchProfile(playerString1);
    GameProfile profile2 = Utils.fetchProfile(playerString2);

    if (profile1 == null) {
      context.getSource().sendFeedback(() -> Text.literal("§cPlayer 1 could not be found"), false);
    }
    if (profile2 == null) {
      context.getSource().sendFeedback(() -> Text.literal("§cPlayer 2 could not be found"), false);
    }
    if (profile1 == null || profile2 == null) {
      return 1;
    }

    AuthorizedAccounts authorizedAccounts =
        new AuthorizedAccounts(List.of(profile1.getId(), profile2.getId()));
    PlayerLogger.getInstance().getAuthorizedAccountsRepository().add(authorizedAccounts);

    context
        .getSource()
        .sendFeedback(() -> Text.literal("Successfully added authorized accounts"), false);

    return 1;
  }
}
