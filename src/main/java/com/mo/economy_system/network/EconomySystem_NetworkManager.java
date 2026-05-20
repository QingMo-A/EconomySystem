package com.mo.economy_system.network;

import com.mo.economy_system.network.packets.Packet_ServerPlayerListRequest;
import com.mo.economy_system.network.packets.Packet_ServerPlayerListResponse;
import com.mo.economy_system.network.packets.check_system.*;
import com.mo.economy_system.network.packets.economy_system.*;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_ConfirmDemandOrder;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_CreateDemandOrder;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_DeliverDemandOrder;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_RemoveDemandOrder;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_CreateSalesOrder;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_PurchaseSalesOrder;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_RemoveSalesOrder;
import com.mo.economy_system.network.packets.territory_system.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class EconomySystem_NetworkManager {
    private static final String PROTOCOL_VERSION = "1";

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EconomySystem_NetworkManager::registerPayloadHandlers);
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(Packet_BalanceRequest.TYPE, Packet_BalanceRequest.STREAM_CODEC, Packet_BalanceRequest::handle);
        registrar.playToClient(Packet_BalanceResponse.TYPE, Packet_BalanceResponse.STREAM_CODEC, Packet_BalanceResponse::handle);
        registrar.playToServer(Packet_BalanceLogRequest.TYPE, Packet_BalanceLogRequest.STREAM_CODEC, Packet_BalanceLogRequest::handle);
        registrar.playToClient(Packet_BalanceLogResponse.TYPE, Packet_BalanceLogResponse.STREAM_CODEC, Packet_BalanceLogResponse::handle);
        registrar.playToServer(Packet_Transfer.TYPE, Packet_Transfer.STREAM_CODEC, Packet_Transfer::handle);
        registrar.playToServer(Packet_ShopDataRequest.TYPE, Packet_ShopDataRequest.STREAM_CODEC, Packet_ShopDataRequest::handle);
        registrar.playToClient(Packet_ShopDataResponse.TYPE, Packet_ShopDataResponse.STREAM_CODEC, Packet_ShopDataResponse::handle);
        registrar.playToServer(Packet_ShopBuyItem.TYPE, Packet_ShopBuyItem.STREAM_CODEC, Packet_ShopBuyItem::handle);
        registrar.playToServer(Packet_CreateSalesOrder.TYPE, Packet_CreateSalesOrder.STREAM_CODEC, Packet_CreateSalesOrder::handle);
        registrar.playToServer(Packet_CreateDemandOrder.TYPE, Packet_CreateDemandOrder.STREAM_CODEC, Packet_CreateDemandOrder::handle);
        registrar.playToServer(Packet_MarketDataRequest.TYPE, Packet_MarketDataRequest.STREAM_CODEC, Packet_MarketDataRequest::handle);
        registrar.playToClient(Packet_MarketDataResponse.TYPE, Packet_MarketDataResponse.STREAM_CODEC, Packet_MarketDataResponse::handle);
        registrar.playToServer(Packet_PurchaseSalesOrder.TYPE, Packet_PurchaseSalesOrder.STREAM_CODEC, Packet_PurchaseSalesOrder::handle);
        registrar.playToServer(Packet_ConfirmDemandOrder.TYPE, Packet_ConfirmDemandOrder.STREAM_CODEC, Packet_ConfirmDemandOrder::handle);
        registrar.playToServer(Packet_DeliverDemandOrder.TYPE, Packet_DeliverDemandOrder.STREAM_CODEC, Packet_DeliverDemandOrder::handle);
        registrar.playToServer(Packet_RemoveSalesOrder.TYPE, Packet_RemoveSalesOrder.STREAM_CODEC, Packet_RemoveSalesOrder::handle);
        registrar.playToServer(Packet_RemoveDemandOrder.TYPE, Packet_RemoveDemandOrder.STREAM_CODEC, Packet_RemoveDemandOrder::handle);
        registrar.playToServer(Packet_TerritoryDataRequest.TYPE, Packet_TerritoryDataRequest.STREAM_CODEC, Packet_TerritoryDataRequest::handle);
        registrar.playToClient(Packet_TerritoryDataResponse.TYPE, Packet_TerritoryDataResponse.STREAM_CODEC, Packet_TerritoryDataResponse::handle);
        registrar.playToServer(Packet_TeleportToTerritory.TYPE, Packet_TeleportToTerritory.STREAM_CODEC, Packet_TeleportToTerritory::handle);
        registrar.playToServer(Packet_InvitePlayer.TYPE, Packet_InvitePlayer.STREAM_CODEC, Packet_InvitePlayer::handle);
        registrar.playToServer(Packet_RemoveTerritory.TYPE, Packet_RemoveTerritory.STREAM_CODEC, Packet_RemoveTerritory::handle);
        registrar.playToServer(Packet_RemovePlayer.TYPE, Packet_RemovePlayer.STREAM_CODEC, Packet_RemovePlayer::handle);
        registrar.playToClient(Packet_Check.TYPE, Packet_Check.STREAM_CODEC, Packet_Check::handle);
        registrar.playToServer(Packet_CheckResultRequest.TYPE, Packet_CheckResultRequest.STREAM_CODEC, Packet_CheckResultRequest::handle);
        registrar.playToClient(Packet_CheckResultResponse.TYPE, Packet_CheckResultResponse.STREAM_CODEC, Packet_CheckResultResponse::handle);
        registrar.playToClient(Packet_Get.TYPE, Packet_Get.STREAM_CODEC, Packet_Get::handle);
        registrar.playToServer(Packet_GetResultRequest.TYPE, Packet_GetResultRequest.STREAM_CODEC, Packet_GetResultRequest::handle);
        registrar.playToClient(Packet_GetResultResponse.TYPE, Packet_GetResultResponse.STREAM_CODEC, Packet_GetResultResponse::handle);
        registrar.playToServer(Packet_Chunk.TYPE, Packet_Chunk.STREAM_CODEC, Packet_Chunk::handle);
        registrar.playToClient(Packet_ChunkResponse.TYPE, Packet_ChunkResponse.STREAM_CODEC, Packet_ChunkResponse::handle);
        registrar.playToServer(Packet_DeliveryBoxDataRequest.TYPE, Packet_DeliveryBoxDataRequest.STREAM_CODEC, Packet_DeliveryBoxDataRequest::handle);
        registrar.playToClient(Packet_DeliveryBoxDataResponse.TYPE, Packet_DeliveryBoxDataResponse.STREAM_CODEC, Packet_DeliveryBoxDataResponse::handle);
        registrar.playToServer(Packet_DeliveryBoxClaimItem.TYPE, Packet_DeliveryBoxClaimItem.STREAM_CODEC, Packet_DeliveryBoxClaimItem::handle);
        registrar.playToServer(Packet_ServerPlayerListRequest.TYPE, Packet_ServerPlayerListRequest.STREAM_CODEC, Packet_ServerPlayerListRequest::handle);
        registrar.playToClient(Packet_ServerPlayerListResponse.TYPE, Packet_ServerPlayerListResponse.STREAM_CODEC, Packet_ServerPlayerListResponse::handle);
        registrar.playToServer(Packet_ModifyMode.TYPE, Packet_ModifyMode.STREAM_CODEC, Packet_ModifyMode::handle);
        registrar.playToServer(Packet_UnlockTerritoryBuff.TYPE, Packet_UnlockTerritoryBuff.STREAM_CODEC, Packet_UnlockTerritoryBuff::handle);
        registrar.playToServer(Packet_UpgradeTerritoryBuff.TYPE, Packet_UpgradeTerritoryBuff.STREAM_CODEC, Packet_UpgradeTerritoryBuff::handle);
        registrar.playToServer(Packet_SingleTerritoryDataRequest.TYPE, Packet_SingleTerritoryDataRequest.STREAM_CODEC, Packet_SingleTerritoryDataRequest::handle);
        registrar.playToClient(Packet_SingleTerritoryDataResponse.TYPE, Packet_SingleTerritoryDataResponse.STREAM_CODEC, Packet_SingleTerritoryDataResponse::handle);
        registrar.playToServer(Packet_UpdateTerritoryPermission.TYPE, Packet_UpdateTerritoryPermission.STREAM_CODEC, Packet_UpdateTerritoryPermission::handle);
        registrar.playToServer(Packet_TransferTerritoryOwnership.TYPE, Packet_TransferTerritoryOwnership.STREAM_CODEC, Packet_TransferTerritoryOwnership::handle);
        registrar.playToServer(Packet_UpdateTerritoryRule.TYPE, Packet_UpdateTerritoryRule.STREAM_CODEC, Packet_UpdateTerritoryRule::handle);
    }

    public static void sendToClient(CustomPacketPayload packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToClient(ServerPlayer player, CustomPacketPayload packet) {
        sendToClient(packet, player);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }
}
