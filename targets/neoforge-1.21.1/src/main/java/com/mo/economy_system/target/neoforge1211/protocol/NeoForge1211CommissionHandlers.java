package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.client.ClientCommissionState;
import com.mo.economy_system.common.commission.CommissionService;
import com.mo.economy_system.common.network.CommissionActionResponseMessage;
import com.mo.economy_system.common.network.CommissionDataRequestMessage;
import com.mo.economy_system.common.network.CommissionDataResponseMessage;
import com.mo.economy_system.common.network.CommissionSubmitMessage;
import com.mo.economy_system.common.network.CommissionSubmitStatus;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.target.neoforge1211.commission.NeoForge1211CommissionRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** NeoForge server/client handlers for the personal commission center protocol. */
public final class NeoForge1211CommissionHandlers {
  private NeoForge1211CommissionHandlers() {}

  public static void request(CommissionDataRequestMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (context.player() instanceof ServerPlayer player) sendData(player, message.requestId());
    });
  }

  public static void submit(CommissionSubmitMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer player)) return;
      try {
        CommissionService.SubmitResult result = NeoForge1211CommissionRuntime.submitItem(
            player, message.commissionId(), message.submissionId(), message.amount());
        EconomySystem_NetworkManager.sendToClient(player, new CommissionActionResponseMessage(
            message.requestId(), status(result.outcome()), result.issue()));
      } catch (RuntimeException failure) {
        EconomySystem_NetworkManager.sendToClient(player, new CommissionActionResponseMessage(
            message.requestId(), CommissionSubmitStatus.REJECTED, safeMessage(failure)));
      }
      sendData(player, message.requestId());
    });
  }

  public static void data(CommissionDataResponseMessage message, IPayloadContext context) {
    context.enqueueWork(() -> ClientCommissionState.apply(message));
  }

  public static void action(CommissionActionResponseMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (message.status() == CommissionSubmitStatus.REJECTED) {
        ClientCommissionState.applyActionError(message.requestId(), message.message());
      }
    });
  }

  private static void sendData(ServerPlayer player, long requestId) {
    try {
      CommissionService.RefreshView view = NeoForge1211CommissionRuntime.refresh(player);
      long now = Math.max(1L, System.currentTimeMillis());
      EconomySystem_NetworkManager.sendToClient(player, CommissionDataResponseMessage.data(
          requestId, now, view.state().schedule().nextRefreshAt(),
          NeoForge1211CommissionRuntime.maxActivePersonalCommissions(player.server),
          view.state().commissions()));
    } catch (RuntimeException failure) {
      EconomySystem_NetworkManager.sendToClient(player, CommissionDataResponseMessage.error(
          requestId, Math.max(1L, System.currentTimeMillis()), "screen.commissions.sync_failed"));
    }
  }

  private static CommissionSubmitStatus status(CommissionService.SubmitOutcome outcome) {
    return switch (outcome) {
      case PROGRESSED -> CommissionSubmitStatus.PROGRESSED;
      case COMPLETED, REWARD_PENDING_MAIL, REWARD_DELIVERY_RETRY -> CommissionSubmitStatus.COMPLETED;
      case ALREADY_COMPLETED -> CommissionSubmitStatus.ALREADY_COMPLETED;
      default -> CommissionSubmitStatus.REJECTED;
    };
  }

  private static String safeMessage(RuntimeException failure) {
    String value = failure.getMessage();
    return value == null || value.isBlank() ? "commission request failed" : value;
  }
}
