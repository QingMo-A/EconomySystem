package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.check.ClientFileCheckConsentCoordinator;
import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckResultJsonCodec;
import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.network.ClientFileCheckRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultResponseMessage;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.client.Minecraft;

public final class NeoForge1211ClientFileCheckScreens {
  private NeoForge1211ClientFileCheckScreens() {}

  public static void openConsent(ClientFileCheckRequestMessage message) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || !minecraft.player.getUUID().equals(message.targetPlayerId()))
      return;
    if (minecraft.getConnection() == null) return;
    NeoForge1211ClientFileCheckClientRuntime.currentOrBegin(
        minecraft.getConnection(), minecraft.player.getUUID());
    ClientFileCheckTaskCoordinator.RequestIdentity identity = identity(message);
    ClientFileCheckConsentCoordinator.Decision decision =
        NeoForge1211ClientFileCheckClientRuntime.consent().receive(identity);
    if (decision == ClientFileCheckConsentCoordinator.Decision.DUPLICATE) return;
    if (decision == ClientFileCheckConsentCoordinator.Decision.BUSY) {
      send(message, ClientFileCheckResult.failed(message.checkType(), "CONSENT_BUSY"));
      return;
    }
    minecraft.setScreen(new Screen_ClientFileCheckConsent(message, identity));
  }

  public static void openResult(ClientFileCheckResultResponseMessage message) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || !minecraft.player.getUUID().equals(message.requesterPlayerId()))
      return;
    if (minecraft.getConnection() == null) return;
    NeoForge1211ClientFileCheckClientRuntime.currentOrBegin(
        minecraft.getConnection(), minecraft.player.getUUID());
    try {
      ClientFileCheckResult result = ClientFileCheckResultJsonCodec.decode(message.resultJson());
      if (result.checkType() == message.checkType())
        minecraft.setScreen(new Screen_ClientFileCheckResult(message, result));
    } catch (RuntimeException ignored) {
      // Fail closed.
    }
  }

  static ClientFileCheckTaskCoordinator.RequestIdentity identity(ClientFileCheckRequestMessage m) {
    return new ClientFileCheckTaskCoordinator.RequestIdentity(
        m.targetPlayerId(), m.requesterPlayerId(), m.checkType());
  }

  static void send(ClientFileCheckRequestMessage request, ClientFileCheckResult result) {
    EconomySystem_NetworkManager.sendToServer(
        new ClientFileCheckResultRequestMessage(
            request.targetPlayerName(),
            request.targetPlayerId(),
            request.requesterPlayerName(),
            request.requesterPlayerId(),
            request.checkType(),
            ClientFileCheckResultJsonCodec.encode(result)));
  }
}
