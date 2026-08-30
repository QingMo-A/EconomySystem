package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.protocol.EconomyMessageDirection;
import com.mo.economy_system.protocol.EconomyMessageSpec;
import com.mo.economy_system.protocol.EconomyMessageType;
import com.mo.economy_system.protocol.EconomyProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** NeoForge registration mechanics for the canonical common protocol. */
public final class NeoForge1211ProtocolRegistrar {
  private NeoForge1211ProtocolRegistrar() {}

  public static void register(RegisterPayloadHandlersEvent event) {
    NeoForge1211MessageBindings.registry();
    PayloadRegistrar registrar = event.registrar(EconomyProtocol.VERSION);
    bindCommon(
        registrar, EconomyMessages.BALANCE_REQUEST, NeoForge1211BalanceHandlers::handleRequest);
    bindCommon(
        registrar, EconomyMessages.BALANCE_RESPONSE, NeoForge1211BalanceHandlers::handleResponse);
    bindCommon(
        registrar,
        EconomyMessages.BALANCE_LOG_REQUEST,
        NeoForge1211BalanceLogHandlers::handleRequest);
    bindCommon(
        registrar,
        EconomyMessages.BALANCE_LOG_RESPONSE,
        NeoForge1211BalanceLogHandlers::handleResponse);
    bindCommon(registrar, EconomyMessages.TRANSFER, NeoForge1211TransferHandler::handle);
    bindCommon(
        registrar, EconomyMessages.SHOP_DATA_REQUEST, NeoForge1211ShopDataHandlers::handleRequest);
    bindCommon(
        registrar,
        EconomyMessages.SHOP_DATA_RESPONSE,
        NeoForge1211ShopDataHandlers::handleResponse);
    bindCommon(registrar, EconomyMessages.SHOP_BUY_ITEM, NeoForge1211ShopPurchaseHandler::handle);
    bindCommon(
        registrar, EconomyMessages.CREATE_SALES_ORDER, NeoForge1211CreateSalesOrderHandler::handle);
    bindCommon(
        registrar,
        EconomyMessages.CREATE_DEMAND_ORDER,
        NeoForge1211CreateDemandOrderHandler::handle);
    bindCommon(
        registrar, EconomyMessages.MARKET_DATA_REQUEST, NeoForge1211MarketDataHandlers::request);
    bindCommon(
        registrar, EconomyMessages.MARKET_DATA_RESPONSE, NeoForge1211MarketDataHandlers::response);
    bindCommon(
        registrar,
        EconomyMessages.PURCHASE_SALES_ORDER,
        NeoForge1211PurchaseSalesOrderHandler::handle);
    bindCommon(
        registrar,
        EconomyMessages.CONFIRM_DEMAND_ORDER,
        NeoForge1211ConfirmDemandOrderHandler::handle);
    bindCommon(
        registrar,
        EconomyMessages.DELIVER_DEMAND_ORDER,
        NeoForge1211DeliverDemandOrderHandler::handle);
    bindCommon(
        registrar, EconomyMessages.REMOVE_SALES_ORDER, NeoForge1211RemoveSalesOrderHandler::handle);
    bindCommon(
        registrar,
        EconomyMessages.REMOVE_DEMAND_ORDER,
        NeoForge1211RemoveDemandOrderHandler::handle);
    bindCommon(
        registrar,
        EconomyMessages.TERRITORY_DATA_REQUEST,
        NeoForge1211TerritoryDataHandlers::handleRequest);
    bindCommon(
        registrar,
        EconomyMessages.TERRITORY_DATA_RESPONSE,
        NeoForge1211TerritoryDataHandlers::handleResponse);
    bindCommon(
        registrar,
        EconomyMessages.TELEPORT_TO_TERRITORY,
        NeoForge1211TerritoryTeleportHandler::handle);
    bindCommon(
        registrar, EconomyMessages.INVITE_PLAYER, NeoForge1211TerritoryInviteHandler::handle);
    bindCommon(
        registrar, EconomyMessages.REMOVE_TERRITORY, NeoForge1211TerritoryRemovalHandler::handle);
    bindCommon(
        registrar,
        EconomyMessages.REMOVE_PLAYER,
        NeoForge1211TerritoryMemberRemovalHandler::handle);
    bindCommon(registrar, EconomyMessages.CHECK, NeoForge1211ClientFileCheckRequestHandler::handle);
    bindCommon(
        registrar,
        EconomyMessages.CHECK_RESULT_REQUEST,
        NeoForge1211ClientFileCheckResultRequestHandler::handle);
    bindCommon(
        registrar,
        EconomyMessages.CHECK_RESULT_RESPONSE,
        NeoForge1211ClientFileCheckResultResponseHandler::handle);
    bindCommon(registrar, EconomyMessages.GET, NeoForge1211CheckedFileTransferHandlers::request);
    bindCommon(registrar, EconomyMessages.GET_RESULT_REQUEST, NeoForge1211CheckedFileTransferHandlers::controlRequest);
    bindCommon(registrar, EconomyMessages.GET_RESULT_RESPONSE, NeoForge1211CheckedFileTransferHandlers::controlResponse);
    bindCommon(registrar, EconomyMessages.CHUNK, NeoForge1211CheckedFileTransferHandlers::chunkRequest);
    bindCommon(registrar, EconomyMessages.CHUNK_RESPONSE, NeoForge1211CheckedFileTransferHandlers::chunkResponse);
    bindCommon(
        registrar,
        EconomyMessages.DELIVERY_BOX_DATA_REQUEST,
        NeoForge1211DeliveryBoxHandlers::request);
    bindCommon(
        registrar,
        EconomyMessages.DELIVERY_BOX_DATA_RESPONSE,
        NeoForge1211DeliveryBoxHandlers::response);
    bindCommon(
        registrar,
        EconomyMessages.DELIVERY_BOX_CLAIM_ITEM,
        NeoForge1211DeliveryBoxHandlers::claim);
    bindCommon(
        registrar,
        EconomyMessages.SERVER_PLAYER_LIST_REQUEST,
        NeoForge1211PlayerListHandlers::handleRequest);
    bindCommon(
        registrar,
        EconomyMessages.SERVER_PLAYER_LIST_RESPONSE,
        NeoForge1211PlayerListHandlers::handleResponse);
    bindCommon(registrar, EconomyMessages.MODIFY_MODE, NeoForge1211TerritoryManagementHandlers::modifyMode);
    bindCommon(registrar, EconomyMessages.UNLOCK_TERRITORY_BUFF, NeoForge1211TerritoryManagementHandlers::unlockBuff);
    bindCommon(registrar, EconomyMessages.UPGRADE_TERRITORY_BUFF, NeoForge1211TerritoryManagementHandlers::upgradeBuff);
    bindCommon(registrar, EconomyMessages.SINGLE_TERRITORY_DATA_REQUEST, NeoForge1211TerritoryManagementHandlers::singleRequest);
    bindCommon(registrar, EconomyMessages.SINGLE_TERRITORY_DATA_RESPONSE, NeoForge1211TerritoryManagementHandlers::singleResponse);
    bindCommon(registrar, EconomyMessages.UPDATE_TERRITORY_PERMISSION, NeoForge1211TerritoryManagementHandlers::permission);
    bindCommon(registrar, EconomyMessages.TRANSFER_TERRITORY_OWNERSHIP, NeoForge1211TerritoryManagementHandlers::transfer);
    bindCommon(registrar, EconomyMessages.UPDATE_TERRITORY_RULE, NeoForge1211TerritoryManagementHandlers::rule);
    bindCommon(registrar, EconomyMessages.MAILBOX_DATA_REQUEST, NeoForge1211MailboxHandlers::request);
    bindCommon(registrar, EconomyMessages.MAILBOX_DATA_RESPONSE, NeoForge1211MailboxHandlers::response);
    bindCommon(registrar, EconomyMessages.MAILBOX_MARK_READ, NeoForge1211MailboxHandlers::markRead);
    bindCommon(registrar, EconomyMessages.MAILBOX_DELETE, NeoForge1211MailboxHandlers::delete);
    bindCommon(registrar, EconomyMessages.MAILBOX_CLAIM_ATTACHMENT, NeoForge1211MailboxHandlers::claimAttachment);
    bindCommon(registrar, EconomyMessages.MAILBOX_CLAIM_ALL, NeoForge1211MailboxHandlers::claimAll);
    bindCommon(registrar, EconomyMessages.MAILBOX_SEND_PLAYER, NeoForge1211MailboxHandlers::sendPlayer);
    bindCommon(registrar, EconomyMessages.MAILBOX_SEND_RESULT, NeoForge1211MailboxHandlers::sendResult);
    bindCommon(registrar, EconomyMessages.MAILBOX_NOTIFICATION, NeoForge1211MailboxHandlers::notification);
    bindCommon(registrar, EconomyMessages.COMMISSION_DATA_REQUEST, NeoForge1211CommissionHandlers::request);
    bindCommon(registrar, EconomyMessages.COMMISSION_DATA_RESPONSE, NeoForge1211CommissionHandlers::data);
    bindCommon(registrar, EconomyMessages.COMMISSION_SUBMIT, NeoForge1211CommissionHandlers::submit);
    bindCommon(registrar, EconomyMessages.COMMISSION_ACTION_RESPONSE, NeoForge1211CommissionHandlers::action);
    bindCommon(registrar, EconomyMessages.RECYCLE_DATA_REQUEST, NeoForge1211RecycleHandlers::request);
    bindCommon(registrar, EconomyMessages.RECYCLE_DATA_RESPONSE, NeoForge1211RecycleHandlers::data);
    bindCommon(registrar, EconomyMessages.RECYCLE_SUBMIT, NeoForge1211RecycleHandlers::submit);
    bindCommon(registrar, EconomyMessages.RECYCLE_ACTION_RESPONSE, NeoForge1211RecycleHandlers::action);
    bindCommon(registrar, EconomyMessages.PUBLIC_COMMISSION_DATA_REQUEST, NeoForge1211PublicCommissionHandlers::request);
    bindCommon(registrar, EconomyMessages.PUBLIC_COMMISSION_DATA_RESPONSE, NeoForge1211PublicCommissionHandlers::data);
    bindCommon(registrar, EconomyMessages.PUBLIC_COMMISSION_SUBMIT, NeoForge1211PublicCommissionHandlers::submit);
    bindCommon(registrar, EconomyMessages.PUBLIC_COMMISSION_ACTION_RESPONSE, NeoForge1211PublicCommissionHandlers::action);
  }

  private static <T extends EconomyNetworkMessage> void bindCommon(
      PayloadRegistrar registrar, EconomyMessageType<T> messageType, CommonHandler<T> handler) {
    CustomPacketPayload.Type<NeoForge1211PayloadAdapters.Payload<T>> payloadType =
        NeoForge1211PayloadAdapters.payloadType(messageType);
    StreamCodec<RegistryFriendlyByteBuf, NeoForge1211PayloadAdapters.Payload<T>> codec =
        NeoForge1211PayloadAdapters.codec(messageType);
    if (messageType.direction() == EconomyMessageDirection.CLIENT_TO_SERVER) {
      registrar.playToServer(
          payloadType, codec, (payload, context) -> handler.handle(payload.message(), context));
    } else {
      registrar.playToClient(
          payloadType, codec, (payload, context) -> handler.handle(payload.message(), context));
    }
  }

  private static <T extends CustomPacketPayload> void bind(
      PayloadRegistrar registrar,
      EconomyMessageSpec spec,
      CustomPacketPayload.Type<T> payloadType,
      StreamCodec<RegistryFriendlyByteBuf, T> codec,
      Handler<T> handler) {
    if (!spec.id().equals(payloadType.id().toString())) {
      throw new IllegalStateException(
          "NeoForge payload id " + payloadType.id() + " does not match canonical id " + spec.id());
    }
    if (spec.direction() == EconomyMessageDirection.CLIENT_TO_SERVER) {
      registrar.playToServer(payloadType, codec, handler::handle);
    } else {
      registrar.playToClient(payloadType, codec, handler::handle);
    }
  }

  @FunctionalInterface
  private interface Handler<T extends CustomPacketPayload> {
    void handle(T message, IPayloadContext context);
  }

  @FunctionalInterface
  private interface CommonHandler<T extends EconomyNetworkMessage> {
    void handle(T message, IPayloadContext context);
  }
}
