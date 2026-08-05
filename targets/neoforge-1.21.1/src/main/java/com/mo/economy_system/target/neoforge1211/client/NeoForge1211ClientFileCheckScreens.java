package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckResultJsonCodec;
import com.mo.economy_system.common.network.ClientFileCheckRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultResponseMessage;
import net.minecraft.client.Minecraft;

public final class NeoForge1211ClientFileCheckScreens {
  private NeoForge1211ClientFileCheckScreens() {}

  public static void openConsent(ClientFileCheckRequestMessage message) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || !minecraft.player.getUUID().equals(message.targetPlayerId()))
      return;
    if (minecraft.screen instanceof Screen_ClientFileCheckConsent) return;
    minecraft.setScreen(new Screen_ClientFileCheckConsent(message));
  }

  public static void openResult(ClientFileCheckResultResponseMessage message) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || !minecraft.player.getUUID().equals(message.requesterPlayerId()))
      return;
    try {
      ClientFileCheckResult result = ClientFileCheckResultJsonCodec.decode(message.resultJson());
      if (result.checkType() == message.checkType())
        minecraft.setScreen(new Screen_ClientFileCheckResult(message, result));
    } catch (RuntimeException ignored) {
      // Fail closed.
    }
  }
}
