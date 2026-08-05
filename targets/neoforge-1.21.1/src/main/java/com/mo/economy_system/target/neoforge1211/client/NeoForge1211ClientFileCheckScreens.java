package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.check.ClientFileCheckClientResultDispatcher;
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
    ClientFileCheckTaskCoordinator.Session session =
        NeoForge1211ClientFileCheckClientRuntime.currentOrBegin(
            minecraft.getConnection(), minecraft.player.getUUID());
    ClientFileCheckTaskCoordinator.RequestIdentity identity = identity(message);
    ClientFileCheckConsentCoordinator.Decision decision =
        NeoForge1211ClientFileCheckClientRuntime.consent().receive(identity, session);
    if (decision == ClientFileCheckConsentCoordinator.Decision.DUPLICATE) return;
    if (decision == ClientFileCheckConsentCoordinator.Decision.BUSY) {
      dispatchBusy(
          message,
          identity,
          session,
          ClientFileCheckResult.failed(message.checkType(), "CONSENT_BUSY"));
      return;
    }
    minecraft.setScreen(new Screen_ClientFileCheckConsent(message, identity, session));
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

  static boolean dispatchTerminal(
      ClientFileCheckRequestMessage request,
      ClientFileCheckTaskCoordinator.RequestIdentity identity,
      ClientFileCheckTaskCoordinator.Session session,
      ClientFileCheckTaskCoordinator.TaskToken token,
      ClientFileCheckResult result) {
    Minecraft minecraft = Minecraft.getInstance();
    return ClientFileCheckClientResultDispatcher.terminal(
        NeoForge1211ClientFileCheckClientRuntime.tasks(),
        NeoForge1211ClientFileCheckClientRuntime.consent(),
        session,
        identity,
        token,
        minecraft::getConnection,
        () -> minecraft.player == null ? null : minecraft.player.getUUID(),
        result,
        value -> sendRaw(request, value),
        (stage, ignored, failure) -> {});
  }

  private static boolean dispatchBusy(
      ClientFileCheckRequestMessage request,
      ClientFileCheckTaskCoordinator.RequestIdentity identity,
      ClientFileCheckTaskCoordinator.Session session,
      ClientFileCheckResult result) {
    Minecraft minecraft = Minecraft.getInstance();
    return ClientFileCheckClientResultDispatcher.busy(
        NeoForge1211ClientFileCheckClientRuntime.tasks(),
        NeoForge1211ClientFileCheckClientRuntime.consent(),
        session,
        identity,
        minecraft::getConnection,
        () -> minecraft.player == null ? null : minecraft.player.getUUID(),
        result,
        value -> sendRaw(request, value),
        (stage, ignored, failure) -> {});
  }

  private static void sendRaw(ClientFileCheckRequestMessage request, ClientFileCheckResult result) {
    EconomySystem_NetworkManager.sendToServer(
        new ClientFileCheckResultRequestMessage(
            request.targetPlayerName(),
            request.targetPlayerId(),
            request.requesterPlayerName(),
            request.requesterPlayerId(),
            request.checkType(),
            ClientFileCheckResultJsonCodec.encode(result)));
    NeoForge1211ClientFileCheckClientRuntime.manifest().replace(request.requesterPlayerId(), result, System.nanoTime());
  }
}
