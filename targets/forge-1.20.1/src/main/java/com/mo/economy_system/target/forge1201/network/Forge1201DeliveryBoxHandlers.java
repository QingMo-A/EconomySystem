package com.mo.economy_system.target.forge1201.network;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.delivery.DeliveryBoxClaimResult;
import com.mo.economy_system.common.delivery.DeliveryBoxClaimService;
import com.mo.economy_system.common.delivery.DeliveryBoxQueryService;
import com.mo.economy_system.common.network.DeliveryBoxClaimMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataRequestMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataResponseMessage;
import com.mo.economy_system.platform.EconomyServices;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

final class Forge1201DeliveryBoxHandlers {
  private static final Logger LOGGER = LogUtils.getLogger();

  private Forge1201DeliveryBoxHandlers() {}

  static void request(
      DeliveryBoxDataRequestMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer player = context.getSender();
    if (player != null) context.enqueueWork(() -> {
      Forge1201DeliveryBoxSavedData data = Forge1201DeliveryBoxSavedData.get(player.serverLevel());
      try {
        Forge1201NetworkChannel.sendToPlayer(
            player, DeliveryBoxQueryService.query(message, player.getUUID(), data.ledger()));
      } catch (RuntimeException failure) {
        LOGGER.error("Delivery query failed player={} request={}",
            player.getUUID(), message.requestId(), failure);
        try {
          Forge1201NetworkChannel.sendToPlayer(
              player, DeliveryBoxDataResponseMessage.error(message.requestId()));
        } catch (RuntimeException sendFailure) {
          LOGGER.error("Delivery error response failed player={}", player.getUUID(), sendFailure);
        }
      }
    });
    context.setPacketHandled(true);
  }

  static void response(
      DeliveryBoxDataResponseMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    DistExecutor.unsafeRunWhenOn(
        Dist.CLIENT, () -> () -> Forge1201DeliveryBoxClientState.apply(message));
    context.setPacketHandled(true);
  }

  static void claim(DeliveryBoxClaimMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer player = context.getSender();
    if (player != null) context.enqueueWork(() -> {
      Forge1201DeliveryBoxSavedData data = Forge1201DeliveryBoxSavedData.get(player.serverLevel());
      DeliveryBoxClaimResult result = DeliveryBoxClaimService.claim(
          message,
          new DeliveryBoxClaimService.Context(
              player.getUUID(),
              data.ledger(),
              entry -> EconomyServices.platform()
                  .itemStacks()
                  .restoreSnapshot(entry.item(), player.serverLevel().registryAccess())
                  .orElseThrow(),
              new Forge1201TransactionalInventoryAdapter(player),
              data::markDirty,
              (owner, entry, stage, outcome, error) -> LOGGER.error(
                  "Delivery claim failed owner={} entry={} stage={} result={}",
                  owner, entry, stage, outcome, error)));
      player.sendSystemMessage(Component.translatable(feedbackKey(result)));
      try {
        Forge1201NetworkChannel.sendToPlayer(
            player,
            result == DeliveryBoxClaimResult.STATE_UNKNOWN
                ? DeliveryBoxDataResponseMessage.error(message.requestId())
                : DeliveryBoxDataResponseMessage.data(
                    message.requestId(), data.ledger().list(player.getUUID())));
      } catch (RuntimeException failure) {
        LOGGER.error("Delivery claim refresh failed player={}", player.getUUID(), failure);
      }
    });
    context.setPacketHandled(true);
  }

  private static String feedbackKey(DeliveryBoxClaimResult result) {
    return switch (result) {
      case SUCCESS -> "message.delivery.claim.success";
      case NOT_FOUND -> "message.delivery.claim.not_found";
      case INVENTORY_FULL -> "message.delivery.claim.inventory_full";
      case ITEM_RESTORE_FAILED, INVALID_ENTRY -> "message.delivery.claim.invalid_item";
      case PERSIST_FAILED -> "message.delivery.claim.persist_failed";
      case ROLLBACK_FAILED, STATE_UNKNOWN -> "message.delivery.claim.state_unknown";
      case INVENTORY_FAILED -> "message.delivery.claim.inventory_failed";
    };
  }
}
