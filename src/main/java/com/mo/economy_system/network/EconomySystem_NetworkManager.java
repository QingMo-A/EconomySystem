package com.mo.economy_system.network;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.packets.*;
import com.mo.economy_system.network.packets.check_system.*;
import com.mo.economy_system.network.packets.economy_system.*;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_ConfirmDemandOrder;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_DeliverDemandOrder;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_RemoveDemandOrder;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_CreateDemandOrder;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_CreateSalesOrder;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_PurchaseSalesOrder;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_RemoveSalesOrder;
import com.mo.economy_system.network.packets.playerattribute_system.strength_system.Packet_CantRun;
import com.mo.economy_system.network.packets.playerattribute_system.strength_system.Packet_SprintKeyPress;
import com.mo.economy_system.network.packets.playerattribute_system.strength_system.Packet_SyncStrengthData;
import com.mo.economy_system.network.packets.playerdata_system.Packet_LevelUpNotify;
import com.mo.economy_system.network.packets.playerdata_system.Packet_SyncPlayerData;
import com.mo.economy_system.network.packets.task_system.Packet_SyncFullTaskData;
import com.mo.economy_system.network.packets.territory_system.*;
import com.mo.economy_system.network.packets.tip_system.Packet_SendTipToClient;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class EconomySystem_NetworkManager {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EconomySystem.MODID, "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int packetId = 0;

        // 注册数据包
        INSTANCE.registerMessage(packetId++, Packet_BalanceRequest.class, Packet_BalanceRequest::encode, Packet_BalanceRequest::decode, Packet_BalanceRequest::handle);
        INSTANCE.registerMessage(packetId++, Packet_BalanceResponse.class, Packet_BalanceResponse::encode, Packet_BalanceResponse::decode, Packet_BalanceResponse::handle);
        INSTANCE.registerMessage(packetId++, Packet_Transfer.class, Packet_Transfer::encode, Packet_Transfer::decode, Packet_Transfer::handle);
        INSTANCE.registerMessage(packetId++, Packet_ShopDataRequest.class, Packet_ShopDataRequest::encode, Packet_ShopDataRequest::decode, Packet_ShopDataRequest::handle);
        INSTANCE.registerMessage(packetId++, Packet_ShopDataResponse.class, Packet_ShopDataResponse::encode, Packet_ShopDataResponse::decode, Packet_ShopDataResponse::handle);
        INSTANCE.registerMessage(packetId++, Packet_ShopBuyItem.class, Packet_ShopBuyItem::encode, Packet_ShopBuyItem::decode, Packet_ShopBuyItem::handle);
        INSTANCE.registerMessage(packetId++, Packet_CreateSalesOrder.class, Packet_CreateSalesOrder::encode, Packet_CreateSalesOrder::decode, Packet_CreateSalesOrder::handle);
        INSTANCE.registerMessage(packetId++, Packet_CreateDemandOrder.class, Packet_CreateDemandOrder::encode, Packet_CreateDemandOrder::decode, Packet_CreateDemandOrder::handle);
        INSTANCE.registerMessage(packetId++, Packet_MarketDataRequest.class, Packet_MarketDataRequest::encode, Packet_MarketDataRequest::decode, Packet_MarketDataRequest::handle);
        INSTANCE.registerMessage(packetId++, Packet_MarketDataResponse.class, Packet_MarketDataResponse::encode, Packet_MarketDataResponse::decode, Packet_MarketDataResponse::handle);
        INSTANCE.registerMessage(packetId++, Packet_PurchaseSalesOrder.class, Packet_PurchaseSalesOrder::encode, Packet_PurchaseSalesOrder::decode, Packet_PurchaseSalesOrder::handle);
        INSTANCE.registerMessage(packetId++, Packet_ConfirmDemandOrder.class, Packet_ConfirmDemandOrder::encode, Packet_ConfirmDemandOrder::decode, Packet_ConfirmDemandOrder::handle);
        INSTANCE.registerMessage(packetId++, Packet_DeliverDemandOrder.class, Packet_DeliverDemandOrder::encode, Packet_DeliverDemandOrder::decode, Packet_DeliverDemandOrder::handle);
        INSTANCE.registerMessage(packetId++, Packet_RemoveSalesOrder.class, Packet_RemoveSalesOrder::encode, Packet_RemoveSalesOrder::decode, Packet_RemoveSalesOrder::handle);
        INSTANCE.registerMessage(packetId++, Packet_RemoveDemandOrder.class, Packet_RemoveDemandOrder::encode, Packet_RemoveDemandOrder::decode, Packet_RemoveDemandOrder::handle);
        INSTANCE.registerMessage(packetId++, Packet_TerritoryDataRequest.class, Packet_TerritoryDataRequest::encode, Packet_TerritoryDataRequest::decode, Packet_TerritoryDataRequest::handle);
        INSTANCE.registerMessage(packetId++, Packet_TerritoryDataResponse.class, Packet_TerritoryDataResponse::encode, Packet_TerritoryDataResponse::decode, Packet_TerritoryDataResponse::handle);
        INSTANCE.registerMessage(packetId++, Packet_TeleportToTerritory.class, Packet_TeleportToTerritory::encode, Packet_TeleportToTerritory::decode, Packet_TeleportToTerritory::handle);
        INSTANCE.registerMessage(packetId++, Packet_InvitePlayer.class, Packet_InvitePlayer::encode, Packet_InvitePlayer::decode, Packet_InvitePlayer::handle);
        INSTANCE.registerMessage(packetId++, Packet_RemoveTerritory.class, Packet_RemoveTerritory::encode, Packet_RemoveTerritory::decode, Packet_RemoveTerritory::handle);
        INSTANCE.registerMessage(packetId++, Packet_RemovePlayer.class, Packet_RemovePlayer::encode, Packet_RemovePlayer::decode, Packet_RemovePlayer::handle);
        INSTANCE.registerMessage(packetId++, Packet_Check.class, Packet_Check::encode, Packet_Check::decode, Packet_Check::handle);
        INSTANCE.registerMessage(packetId++, Packet_CheckResultRequest.class, Packet_CheckResultRequest::encode, Packet_CheckResultRequest::decode, Packet_CheckResultRequest::handle);
        INSTANCE.registerMessage(packetId++, Packet_CheckResultResponse.class, Packet_CheckResultResponse::encode, Packet_CheckResultResponse::decode, Packet_CheckResultResponse::handle);
        INSTANCE.registerMessage(packetId++, Packet_Get.class, Packet_Get::encode, Packet_Get::decode, Packet_Get::handle);
        INSTANCE.registerMessage(packetId++, Packet_GetResultRequest.class, Packet_GetResultRequest::encode, Packet_GetResultRequest::decode, Packet_GetResultRequest::handle);
        INSTANCE.registerMessage(packetId++, Packet_GetResultResponse.class, Packet_GetResultResponse::encode, Packet_GetResultResponse::decode, Packet_GetResultResponse::handle);
        INSTANCE.registerMessage(packetId++, Packet_Chunk.class, Packet_Chunk::encode, Packet_Chunk::decode, Packet_Chunk::handle);
        INSTANCE.registerMessage(packetId++, Packet_ChunkResponse.class, Packet_ChunkResponse::encode, Packet_ChunkResponse::decode, Packet_ChunkResponse::handle);
        INSTANCE.registerMessage(packetId++, Packet_DeliveryBoxDataRequest.class, Packet_DeliveryBoxDataRequest::encode, Packet_DeliveryBoxDataRequest::decode, Packet_DeliveryBoxDataRequest::handle);
        INSTANCE.registerMessage(packetId++, Packet_DeliveryBoxDataResponse.class, Packet_DeliveryBoxDataResponse::encode, Packet_DeliveryBoxDataResponse::decode, Packet_DeliveryBoxDataResponse::handle);
        INSTANCE.registerMessage(packetId++, Packet_DeliveryBoxClaimItem.class, Packet_DeliveryBoxClaimItem::encode, Packet_DeliveryBoxClaimItem::decode, Packet_DeliveryBoxClaimItem::handle);
        INSTANCE.registerMessage(packetId++, Packet_ServerPlayerListRequest.class, Packet_ServerPlayerListRequest::encode, Packet_ServerPlayerListRequest::decode, Packet_ServerPlayerListRequest::handle);
        INSTANCE.registerMessage(packetId++, Packet_ServerPlayerListResponse.class, Packet_ServerPlayerListResponse::encode, Packet_ServerPlayerListResponse::decode, Packet_ServerPlayerListResponse::handle);
        INSTANCE.registerMessage(packetId++, Packet_ModifyMode.class, Packet_ModifyMode::encode, Packet_ModifyMode::decode, Packet_ModifyMode::handle);
        INSTANCE.registerMessage(packetId++, Packet_UnlockTerritoryBuff.class, Packet_UnlockTerritoryBuff::encode, Packet_UnlockTerritoryBuff::decode, Packet_UnlockTerritoryBuff::handle);
        INSTANCE.registerMessage(packetId++, Packet_UpgradeTerritoryBuff.class, Packet_UpgradeTerritoryBuff::encode, Packet_UpgradeTerritoryBuff::decode, Packet_UpgradeTerritoryBuff::handle);
        INSTANCE.registerMessage(packetId++, Packet_SingleTerritoryDataRequest.class, Packet_SingleTerritoryDataRequest::encode, Packet_SingleTerritoryDataRequest::decode, Packet_SingleTerritoryDataRequest::handle);
        INSTANCE.registerMessage(packetId++, Packet_SingleTerritoryDataResponse.class, Packet_SingleTerritoryDataResponse::encode, Packet_SingleTerritoryDataResponse::decode, Packet_SingleTerritoryDataResponse::handle);
        INSTANCE.registerMessage(packetId++, Packet_JoinMessage.class, Packet_JoinMessage::encode, Packet_JoinMessage::decode, Packet_JoinMessage::handle);
        INSTANCE.registerMessage(packetId++, Packet_OnlinePlayerCountRequest.class, Packet_OnlinePlayerCountRequest::encode, Packet_OnlinePlayerCountRequest::decode,Packet_OnlinePlayerCountRequest::handle);
        INSTANCE.registerMessage(packetId++, Packet_OnlinePlayerCountResponse.class, Packet_OnlinePlayerCountResponse::encode, Packet_OnlinePlayerCountResponse::decode, Packet_OnlinePlayerCountResponse::handle);
        INSTANCE.registerMessage(packetId++, Packet_SyncPlayerData.class, Packet_SyncPlayerData::encode, Packet_SyncPlayerData::decode, Packet_SyncPlayerData::handle);
        INSTANCE.registerMessage(packetId++, Packet_SyncFullTaskData.class, Packet_SyncFullTaskData::encode, Packet_SyncFullTaskData::decode, Packet_SyncFullTaskData::handle);
        INSTANCE.registerMessage(packetId++, Packet_SprintKeyPress.class, Packet_SprintKeyPress::encode, Packet_SprintKeyPress::decode, Packet_SprintKeyPress::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        INSTANCE.registerMessage(packetId++, Packet_CantRun.class, Packet_CantRun::encode, Packet_CantRun::decode, Packet_CantRun::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(packetId++, Packet_LevelUpNotify.class, Packet_LevelUpNotify::encode, Packet_LevelUpNotify::decode, Packet_LevelUpNotify::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(packetId++, Packet_SyncStrengthData.class, Packet_SyncStrengthData::encode, Packet_SyncStrengthData::decode, Packet_SyncStrengthData::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        INSTANCE.registerMessage(packetId++, Packet_SendTipToClient.class, Packet_SendTipToClient::encode, Packet_SendTipToClient::decode, Packet_SendTipToClient::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}