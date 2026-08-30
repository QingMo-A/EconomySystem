package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.client.ClientPublicCommissionState;
import com.mo.economy_system.common.commission.PublicCommissionService;
import com.mo.economy_system.common.network.commission_public.*;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.target.neoforge1211.commission.NeoForge1211CommissionRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** NeoForge server/client handlers for the server-wide public commission browser. */
public final class NeoForge1211PublicCommissionHandlers {
  private NeoForge1211PublicCommissionHandlers() {}

  public static void request(PublicCommissionDataRequestMessage message, IPayloadContext context) {
    context.enqueueWork(() -> { if (context.player() instanceof ServerPlayer player) sendData(player, message.requestId()); });
  }

  public static void submit(PublicCommissionSubmitMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer player)) return;
      try {
        PublicCommissionService.SubmitResult result = NeoForge1211CommissionRuntime.submitPublicItem(
            player, message.commissionId(), message.submissionId(), message.amount());
        EconomySystem_NetworkManager.sendToClient(player, new PublicCommissionActionResponseMessage(
            message.requestId(), status(result.outcome()), result.acceptedAmount(), result.payout(), result.issue()));
      } catch (RuntimeException failure) {
        EconomySystem_NetworkManager.sendToClient(player, new PublicCommissionActionResponseMessage(
            message.requestId(), PublicCommissionSubmitStatus.REJECTED, 0, 0, safeMessage(failure)));
      }
      sendData(player, message.requestId());
    });
  }

  public static void data(PublicCommissionDataResponseMessage message, IPayloadContext context) {
    context.enqueueWork(() -> ClientPublicCommissionState.apply(message));
  }

  public static void action(PublicCommissionActionResponseMessage message, IPayloadContext context) {
    context.enqueueWork(() -> ClientPublicCommissionState.applyAction(message));
  }

  private static void sendData(ServerPlayer player, long requestId) {
    long now = Math.max(1L, System.currentTimeMillis());
    try {
      EconomySystem_NetworkManager.sendToClient(player, PublicCommissionDataResponseMessage.data(
          requestId, now, NeoForge1211CommissionRuntime.listPublic(player.server)));
    } catch (RuntimeException failure) {
      EconomySystem_NetworkManager.sendToClient(player, PublicCommissionDataResponseMessage.error(
          requestId, now, "screen.commissions.public.sync_failed"));
    }
  }

  private static PublicCommissionSubmitStatus status(PublicCommissionService.SubmitOutcome outcome) {
    return switch (outcome) {
      case ACCEPTED -> PublicCommissionSubmitStatus.ACCEPTED;
      case PARTIAL -> PublicCommissionSubmitStatus.PARTIAL;
      case COMPLETED -> PublicCommissionSubmitStatus.COMPLETED;
      case DUPLICATE -> PublicCommissionSubmitStatus.DUPLICATE;
      case EXPIRED -> PublicCommissionSubmitStatus.EXPIRED;
      case NOT_FOUND -> PublicCommissionSubmitStatus.NOT_FOUND;
      case UNAVAILABLE -> PublicCommissionSubmitStatus.UNAVAILABLE;
      case DELIVERY_RETRY -> PublicCommissionSubmitStatus.DELIVERY_RETRY;
    };
  }

  private static String safeMessage(RuntimeException failure) {
    String value = failure.getMessage();
    return value == null || value.isBlank() ? "public commission request failed" : value;
  }
}
