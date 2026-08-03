package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.network.packets.check_system.*;
import com.mo.economy_system.network.packets.economy_system.*;
import com.mo.economy_system.network.packets.territory_system.*;
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
    bindCommon(registrar, EconomyMessages.REMOVE_DEMAND_ORDER, NeoForge1211RemoveDemandOrderHandler::handle);
    bindCommon(
        registrar, EconomyMessages.TERRITORY_DATA_REQUEST,
        NeoForge1211TerritoryDataHandlers::handleRequest);
    bindCommon(
        registrar, EconomyMessages.TERRITORY_DATA_RESPONSE,
        NeoForge1211TerritoryDataHandlers::handleResponse);
    bindCommon(registrar, EconomyMessages.TELEPORT_TO_TERRITORY,
        NeoForge1211TerritoryTeleportHandler::handle);
    bind(
        registrar,
        EconomyProtocol.INVITE_PLAYER,
        Packet_InvitePlayer.TYPE,
        Packet_InvitePlayer.STREAM_CODEC,
        Packet_InvitePlayer::handle);
    bind(
        registrar,
        EconomyProtocol.REMOVE_TERRITORY,
        Packet_RemoveTerritory.TYPE,
        Packet_RemoveTerritory.STREAM_CODEC,
        Packet_RemoveTerritory::handle);
    bind(
        registrar,
        EconomyProtocol.REMOVE_PLAYER,
        Packet_RemovePlayer.TYPE,
        Packet_RemovePlayer.STREAM_CODEC,
        Packet_RemovePlayer::handle);
    bind(
        registrar,
        EconomyProtocol.CHECK,
        Packet_Check.TYPE,
        Packet_Check.STREAM_CODEC,
        Packet_Check::handle);
    bind(
        registrar,
        EconomyProtocol.CHECK_RESULT_REQUEST,
        Packet_CheckResultRequest.TYPE,
        Packet_CheckResultRequest.STREAM_CODEC,
        Packet_CheckResultRequest::handle);
    bind(
        registrar,
        EconomyProtocol.CHECK_RESULT_RESPONSE,
        Packet_CheckResultResponse.TYPE,
        Packet_CheckResultResponse.STREAM_CODEC,
        Packet_CheckResultResponse::handle);
    bind(
        registrar,
        EconomyProtocol.GET,
        Packet_Get.TYPE,
        Packet_Get.STREAM_CODEC,
        Packet_Get::handle);
    bind(
        registrar,
        EconomyProtocol.GET_RESULT_REQUEST,
        Packet_GetResultRequest.TYPE,
        Packet_GetResultRequest.STREAM_CODEC,
        Packet_GetResultRequest::handle);
    bind(
        registrar,
        EconomyProtocol.GET_RESULT_RESPONSE,
        Packet_GetResultResponse.TYPE,
        Packet_GetResultResponse.STREAM_CODEC,
        Packet_GetResultResponse::handle);
    bind(
        registrar,
        EconomyProtocol.CHUNK,
        Packet_Chunk.TYPE,
        Packet_Chunk.STREAM_CODEC,
        Packet_Chunk::handle);
    bind(
        registrar,
        EconomyProtocol.CHUNK_RESPONSE,
        Packet_ChunkResponse.TYPE,
        Packet_ChunkResponse.STREAM_CODEC,
        Packet_ChunkResponse::handle);
    bind(
        registrar,
        EconomyProtocol.DELIVERY_BOX_DATA_REQUEST,
        Packet_DeliveryBoxDataRequest.TYPE,
        Packet_DeliveryBoxDataRequest.STREAM_CODEC,
        Packet_DeliveryBoxDataRequest::handle);
    bind(
        registrar,
        EconomyProtocol.DELIVERY_BOX_DATA_RESPONSE,
        Packet_DeliveryBoxDataResponse.TYPE,
        Packet_DeliveryBoxDataResponse.STREAM_CODEC,
        Packet_DeliveryBoxDataResponse::handle);
    bind(
        registrar,
        EconomyProtocol.DELIVERY_BOX_CLAIM_ITEM,
        Packet_DeliveryBoxClaimItem.TYPE,
        Packet_DeliveryBoxClaimItem.STREAM_CODEC,
        Packet_DeliveryBoxClaimItem::handle);
    bindCommon(
        registrar,
        EconomyMessages.SERVER_PLAYER_LIST_REQUEST,
        NeoForge1211PlayerListHandlers::handleRequest);
    bindCommon(
        registrar,
        EconomyMessages.SERVER_PLAYER_LIST_RESPONSE,
        NeoForge1211PlayerListHandlers::handleResponse);
    bind(
        registrar,
        EconomyProtocol.MODIFY_MODE,
        Packet_ModifyMode.TYPE,
        Packet_ModifyMode.STREAM_CODEC,
        Packet_ModifyMode::handle);
    bind(
        registrar,
        EconomyProtocol.UNLOCK_TERRITORY_BUFF,
        Packet_UnlockTerritoryBuff.TYPE,
        Packet_UnlockTerritoryBuff.STREAM_CODEC,
        Packet_UnlockTerritoryBuff::handle);
    bind(
        registrar,
        EconomyProtocol.UPGRADE_TERRITORY_BUFF,
        Packet_UpgradeTerritoryBuff.TYPE,
        Packet_UpgradeTerritoryBuff.STREAM_CODEC,
        Packet_UpgradeTerritoryBuff::handle);
    bind(
        registrar,
        EconomyProtocol.SINGLE_TERRITORY_DATA_REQUEST,
        Packet_SingleTerritoryDataRequest.TYPE,
        Packet_SingleTerritoryDataRequest.STREAM_CODEC,
        Packet_SingleTerritoryDataRequest::handle);
    bind(
        registrar,
        EconomyProtocol.SINGLE_TERRITORY_DATA_RESPONSE,
        Packet_SingleTerritoryDataResponse.TYPE,
        Packet_SingleTerritoryDataResponse.STREAM_CODEC,
        Packet_SingleTerritoryDataResponse::handle);
    bind(
        registrar,
        EconomyProtocol.UPDATE_TERRITORY_PERMISSION,
        Packet_UpdateTerritoryPermission.TYPE,
        Packet_UpdateTerritoryPermission.STREAM_CODEC,
        Packet_UpdateTerritoryPermission::handle);
    bind(
        registrar,
        EconomyProtocol.TRANSFER_TERRITORY_OWNERSHIP,
        Packet_TransferTerritoryOwnership.TYPE,
        Packet_TransferTerritoryOwnership.STREAM_CODEC,
        Packet_TransferTerritoryOwnership::handle);
    bind(
        registrar,
        EconomyProtocol.UPDATE_TERRITORY_RULE,
        Packet_UpdateTerritoryRule.TYPE,
        Packet_UpdateTerritoryRule.STREAM_CODEC,
        Packet_UpdateTerritoryRule::handle);
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
