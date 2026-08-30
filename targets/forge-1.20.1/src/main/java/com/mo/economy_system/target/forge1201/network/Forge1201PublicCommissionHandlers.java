package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.client.ClientPublicCommissionState;
import com.mo.economy_system.common.commission.PublicCommission;
import com.mo.economy_system.common.commission.PublicCommissionService;
import com.mo.economy_system.common.network.commission_public.*;
import com.mo.economy_system.target.forge1201.commission.Forge1201CommissionRuntime;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Forge server/client handlers for the server-wide public commission browser. */
final class Forge1201PublicCommissionHandlers {
  private Forge1201PublicCommissionHandlers() {}

  static void request(PublicCommissionDataRequestMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer player = context.getSender();
    if (player != null) context.enqueueWork(() -> sendData(player, message.requestId()));
    context.setPacketHandled(true);
  }

  static void submit(PublicCommissionSubmitMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer player = context.getSender();
    if (player != null) context.enqueueWork(() -> {
      try {
        PublicCommissionService.SubmitResult result = Forge1201CommissionRuntime.submitPublicItem(
            player, message.commissionId(), message.submissionId(), message.amount());
        Forge1201NetworkChannel.sendToPlayer(player, new PublicCommissionActionResponseMessage(
            message.requestId(), status(result.outcome()), result.acceptedAmount(), result.payout(),
            result.issue()));
      } catch (RuntimeException failure) {
        Forge1201NetworkChannel.sendToPlayer(player, new PublicCommissionActionResponseMessage(
            message.requestId(), PublicCommissionSubmitStatus.REJECTED, 0, 0, safeMessage(failure)));
      }
      sendData(player, message.requestId());
    });
    context.setPacketHandled(true);
  }

  static void data(PublicCommissionDataResponseMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPublicCommissionState.apply(message));
    context.setPacketHandled(true);
  }

  static void action(PublicCommissionActionResponseMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPublicCommissionState.applyAction(message));
    context.setPacketHandled(true);
  }

  private static void sendData(ServerPlayer player, long requestId) {
    long now = Math.max(1L, System.currentTimeMillis());
    try {
      Forge1201NetworkChannel.sendToPlayer(player,
          PublicCommissionDataResponseMessage.data(requestId, now,
              Forge1201CommissionRuntime.listPublic(player.server)));
    } catch (RuntimeException failure) {
      Forge1201NetworkChannel.sendToPlayer(player,
          PublicCommissionDataResponseMessage.error(requestId, now,
              "screen.commissions.public.sync_failed"));
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
