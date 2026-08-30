package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.client.ClientCommissionState;
import com.mo.economy_system.common.commission.CommissionService;
import com.mo.economy_system.common.network.CommissionActionResponseMessage;
import com.mo.economy_system.common.network.CommissionDataRequestMessage;
import com.mo.economy_system.common.network.CommissionDataResponseMessage;
import com.mo.economy_system.common.network.CommissionSubmitMessage;
import com.mo.economy_system.common.network.CommissionSubmitStatus;
import com.mo.economy_system.target.forge1201.commission.Forge1201CommissionRuntime;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Forge server/client handlers for the personal commission center protocol. */
final class Forge1201CommissionHandlers {
  private Forge1201CommissionHandlers() {}

  static void request(CommissionDataRequestMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer player = context.getSender();
    if (player != null) context.enqueueWork(() -> sendData(player, message.requestId()));
    context.setPacketHandled(true);
  }

  static void submit(CommissionSubmitMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer player = context.getSender();
    if (player != null) context.enqueueWork(() -> {
      try {
        Forge1201CommissionRuntime.SubmitFeedback feedback =
            Forge1201CommissionRuntime.submitItem(player, message.commissionId(),
                message.submissionId(), message.amount());
        CommissionSubmitStatus status = feedback.accepted()
            ? CommissionSubmitStatus.PROGRESSED : CommissionSubmitStatus.REJECTED;
        Forge1201NetworkChannel.sendToPlayer(player,
            new CommissionActionResponseMessage(message.requestId(), status, feedback.message()));
      } catch (RuntimeException failure) {
        Forge1201NetworkChannel.sendToPlayer(player,
            new CommissionActionResponseMessage(message.requestId(), CommissionSubmitStatus.REJECTED,
                safeMessage(failure)));
      }
      sendData(player, message.requestId());
    });
    context.setPacketHandled(true);
  }

  static void data(CommissionDataResponseMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCommissionState.apply(message));
    context.setPacketHandled(true);
  }

  static void action(CommissionActionResponseMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
      if (message.status() == CommissionSubmitStatus.REJECTED) {
        ClientCommissionState.applyActionError(message.requestId(), message.message());
      }
    });
    context.setPacketHandled(true);
  }

  private static void sendData(ServerPlayer player, long requestId) {
    try {
      CommissionService.RefreshView view = Forge1201CommissionRuntime.refresh(player);
      long now = Math.max(1L, System.currentTimeMillis());
      Forge1201NetworkChannel.sendToPlayer(player, CommissionDataResponseMessage.data(
          requestId, now, view.state().schedule().nextRefreshAt(),
          Forge1201CommissionRuntime.maxActivePersonalCommissions(), view.state().commissions()));
    } catch (RuntimeException failure) {
      Forge1201NetworkChannel.sendToPlayer(player, CommissionDataResponseMessage.error(
          requestId, Math.max(1L, System.currentTimeMillis()), "screen.commissions.sync_failed"));
    }
  }

  private static String safeMessage(RuntimeException failure) {
    String value = failure.getMessage();
    return value == null || value.isBlank() ? "commission request failed" : value;
  }
}
