package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.client.ClientDeliveryBoxState;
import com.mo.economy_system.common.delivery.DeliveryBoxClaimResult;
import com.mo.economy_system.common.delivery.DeliveryBoxClaimService;
import com.mo.economy_system.common.delivery.DeliveryBoxQueryService;
import com.mo.economy_system.common.network.DeliveryBoxClaimMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataRequestMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataResponseMessage;
import com.mo.economy_system.common.network.DeliveryBoxResponseKind;
import com.mo.economy_system.core.economy_system.delivery_box.DeliveryBoxSavedData;
import com.mo.economy_system.core.economy_system.delivery_box.DeliveryItem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.target.neoforge1211.NeoForge1211Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NeoForge1211DeliveryBoxHandlers {
  private NeoForge1211DeliveryBoxHandlers() {}

  public static void request(DeliveryBoxDataRequestMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer player)) return;
      DeliveryBoxSavedData data = DeliveryBoxSavedData.getInstance(player.serverLevel());
      try {
        EconomySystem_NetworkManager.sendToClient(
            player, DeliveryBoxQueryService.query(message, player.getUUID(), data.ledger()));
      } catch (RuntimeException failure) {
        EconomySystem.LOGGER.error(
            "Delivery query failed player={} request={}",
            player.getUUID(), message.requestId(), failure);
        try {
          EconomySystem_NetworkManager.sendToClient(
              player, DeliveryBoxDataResponseMessage.error(message.requestId()));
        } catch (RuntimeException sendFailure) {
          EconomySystem.LOGGER.error("Delivery error response failed player={}", player.getUUID(), sendFailure);
        }
      }
    });
  }

  public static void response(DeliveryBoxDataResponseMessage message, IPayloadContext context) {
    context.enqueueWork(() -> ClientDeliveryBoxState.update(message));
  }

  public static void claim(DeliveryBoxClaimMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer player)) return;
      DeliveryBoxSavedData data = DeliveryBoxSavedData.getInstance(player.serverLevel());
      NeoForge1211TransactionalInventoryAdapter inventory =
          new NeoForge1211TransactionalInventoryAdapter(player);
      DeliveryBoxClaimResult result = DeliveryBoxClaimService.claim(
          message,
          new DeliveryBoxClaimService.Context(
              player.getUUID(),
              data.ledger(),
              entry -> NeoForge1211Platform.nativeItemStacks()
                  .restoreSnapshot(entry.item(), player.registryAccess())
                  .orElseThrow(),
              inventory,
              data::markDirty,
              (owner, entry, stage, outcome, error) -> EconomySystem.LOGGER.error(
                  "Delivery claim failed owner={} entry={} stage={} result={}",
                  owner, entry, stage, outcome, error)));
      player.sendSystemMessage(Component.translatable(feedbackKey(result)));
      try {
        EconomySystem_NetworkManager.sendToClient(
            player,
            result == DeliveryBoxClaimResult.STATE_UNKNOWN
                ? DeliveryBoxDataResponseMessage.error(message.requestId())
                : DeliveryBoxDataResponseMessage.data(
                    message.requestId(), data.ledger().list(player.getUUID())));
      } catch (RuntimeException failure) {
        EconomySystem.LOGGER.error("Delivery claim refresh failed player={}", player.getUUID(), failure);
      }
    });
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
