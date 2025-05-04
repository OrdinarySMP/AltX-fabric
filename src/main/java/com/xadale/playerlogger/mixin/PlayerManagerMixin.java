package com.xadale.playerlogger.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.xadale.playerlogger.IpLogger;
import java.net.InetSocketAddress;
import java.util.UUID;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerLoginNetworkHandler.class, priority = 100)
public class PlayerManagerMixin {

  @Final @Shadow ClientConnection connection;

  @Inject(method = "tickVerify", at = @At("HEAD"))
  private void canJoin(GameProfile profile, CallbackInfo ci) {
    UUID uuid = profile.getId();
    String ip = null;
    try {
      if (this.connection.getAddress() instanceof InetSocketAddress inetAddr) {
        ip = inetAddr.getAddress().getHostAddress();
      }
    } catch (Exception e) {
      LogUtils.getLogger().info("ip gathering not successful");
      return;
    }

    if (ip == null) {
      return;
    }

    IpLogger.handleJoin(ip, uuid);
  }
}
