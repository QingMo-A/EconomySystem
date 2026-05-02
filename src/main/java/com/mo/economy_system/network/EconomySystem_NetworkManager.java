package com.mo.economy_system.network;

import com.mo.economy_system.network.packets.*;
import com.mo.economy_system.network.packets.check_system.*;
import com.mo.economy_system.network.packets.economy_system.*;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_ConfirmDemandOrder;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_CreateDemandOrder;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_DeliverDemandOrder;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_RemoveDemandOrder;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_CreateSalesOrder;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_PurchaseSalesOrder;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_RemoveSalesOrder;
import com.mo.economy_system.network.packets.notice_system.Packet_MarkNoticeReadRequest;
import com.mo.economy_system.network.packets.notice_system.Packet_NoticeCheckResponse;
import com.mo.economy_system.network.packets.notice_system.Packet_NoticeListRequest;
import com.mo.economy_system.network.packets.notice_system.Packet_NoticeListResponse;
import com.mo.economy_system.network.packets.npc_system.Packet_NpcInteractionRequest;
import com.mo.economy_system.network.packets.npc_system.Packet_OpenNpcDialogueGUI;
import com.mo.economy_system.network.packets.playerattribute_system.courage_system.Packet_SyncCourageData;
import com.mo.economy_system.network.packets.playerattribute_system.death_system.Packet_DeathScreenData;
import com.mo.economy_system.network.packets.playerattribute_system.death_system.Packet_KeepInventoryRequest;
import com.mo.economy_system.network.packets.playerattribute_system.death_system.Packet_KeepInventoryResponse;
import com.mo.economy_system.network.packets.playerattribute_system.death_system.Packet_NormalRespawnRequest;
import com.mo.economy_system.network.packets.playerattribute_system.death_system.Packet_NormalRespawnResponse;
import com.mo.economy_system.network.packets.playerattribute_system.death_system.Packet_OpenRevivalCharmGUI;
import com.mo.economy_system.network.packets.playerattribute_system.death_system.Packet_RevivalRequest;
import com.mo.economy_system.network.packets.playerattribute_system.death_system.Packet_SyncRespawnPointData;
import com.mo.economy_system.network.packets.playerattribute_system.infection_system.Packet_SyncInfectionData;
import com.mo.economy_system.network.packets.playerattribute_system.limb_system.Packet_SyncLimbInjury;
import com.mo.economy_system.network.packets.playerattribute_system.strength_system.Packet_CantRun;
import com.mo.economy_system.network.packets.playerattribute_system.strength_system.Packet_SyncStrengthData;
import com.mo.economy_system.network.packets.playerdata_system.Packet_LevelUpNotify;
import com.mo.economy_system.network.packets.playerdata_system.Packet_RequestAllPlayerData;
import com.mo.economy_system.network.packets.playerdata_system.Packet_RequestPlayerStats;
import com.mo.economy_system.network.packets.playerdata_system.Packet_SyncPlayerData;
import com.mo.economy_system.network.packets.playerdata_system.Packet_SyncPlayerStats;
import com.mo.economy_system.network.packets.playerdata_system.Packet_VanillaAdvancementNotify;
import com.mo.economy_system.network.packets.storybook_system.Packet_OpenStoryBookGUI;
import com.mo.economy_system.network.packets.storybook_system.Packet_OpenStoryFragmentGUI;
import com.mo.economy_system.network.packets.storybook_system.Packet_UpdateStoryBookOrder;
import com.mo.economy_system.network.packets.task_system.Packet_SyncCompleteTask;
import com.mo.economy_system.network.packets.task_system.Packet_SyncFullTaskData;
import com.mo.economy_system.network.packets.territory_system.*;
import com.mo.economy_system.network.packets.tip_system.Packet_SendTipToClient;
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
        registrar.playToClient(Packet_SystemMessage.TYPE, Packet_SystemMessage.STREAM_CODEC, Packet_SystemMessage::handle);
        registrar.playToServer(Packet_OnlinePlayerCountRequest.TYPE, Packet_OnlinePlayerCountRequest.STREAM_CODEC, Packet_OnlinePlayerCountRequest::handle);
        registrar.playToClient(Packet_OnlinePlayerCountResponse.TYPE, Packet_OnlinePlayerCountResponse.STREAM_CODEC, Packet_OnlinePlayerCountResponse::handle);
        registrar.playToClient(Packet_SyncPlayerData.TYPE, Packet_SyncPlayerData.STREAM_CODEC, Packet_SyncPlayerData::handle);
        registrar.playToClient(Packet_SyncFullTaskData.TYPE, Packet_SyncFullTaskData.STREAM_CODEC, Packet_SyncFullTaskData::handle);
        registrar.playToServer(Packet_SyncCompleteTask.TYPE, Packet_SyncCompleteTask.STREAM_CODEC, Packet_SyncCompleteTask::handle);
        registrar.playToClient(Packet_CantRun.TYPE, Packet_CantRun.STREAM_CODEC, Packet_CantRun::handle);
        registrar.playToClient(Packet_LevelUpNotify.TYPE, Packet_LevelUpNotify.STREAM_CODEC, Packet_LevelUpNotify::handle);
        registrar.playToClient(Packet_VanillaAdvancementNotify.TYPE, Packet_VanillaAdvancementNotify.STREAM_CODEC, Packet_VanillaAdvancementNotify::handle);
        registrar.playToClient(Packet_SyncStrengthData.TYPE, Packet_SyncStrengthData.STREAM_CODEC, Packet_SyncStrengthData::handle);
        registrar.playToClient(Packet_SendTipToClient.TYPE, Packet_SendTipToClient.STREAM_CODEC, Packet_SendTipToClient::handle);
        registrar.playToClient(Packet_SyncCourageData.TYPE, Packet_SyncCourageData.STREAM_CODEC, Packet_SyncCourageData::handle);
        registrar.playToClient(Packet_SyncInfectionData.TYPE, Packet_SyncInfectionData.STREAM_CODEC, Packet_SyncInfectionData::handle);
        registrar.playToClient(Packet_SyncRespawnPointData.TYPE, Packet_SyncRespawnPointData.STREAM_CODEC, Packet_SyncRespawnPointData::handle);
        registrar.playToServer(Packet_KeepInventoryRequest.TYPE, Packet_KeepInventoryRequest.STREAM_CODEC, Packet_KeepInventoryRequest::handle);
        registrar.playToClient(Packet_KeepInventoryResponse.TYPE, Packet_KeepInventoryResponse.STREAM_CODEC, Packet_KeepInventoryResponse::handle);
        registrar.playToServer(Packet_NormalRespawnRequest.TYPE, Packet_NormalRespawnRequest.STREAM_CODEC, Packet_NormalRespawnRequest::handle);
        registrar.playToClient(Packet_NormalRespawnResponse.TYPE, Packet_NormalRespawnResponse.STREAM_CODEC, Packet_NormalRespawnResponse::handle);
        registrar.playToClient(Packet_DeathScreenData.TYPE, Packet_DeathScreenData.STREAM_CODEC, Packet_DeathScreenData::handle);
        registrar.playToServer(Packet_RequestAllPlayerData.TYPE, Packet_RequestAllPlayerData.STREAM_CODEC, Packet_RequestAllPlayerData::handle);
        registrar.playToServer(Packet_RequestPlayerStats.TYPE, Packet_RequestPlayerStats.STREAM_CODEC, Packet_RequestPlayerStats::handle);
        registrar.playToClient(Packet_SyncPlayerStats.TYPE, Packet_SyncPlayerStats.STREAM_CODEC, Packet_SyncPlayerStats::handle);
        registrar.playToClient(com.mo.economy_system.network.packets.login_system.Packet_PlayerLoginRequest.TYPE, com.mo.economy_system.network.packets.login_system.Packet_PlayerLoginRequest.STREAM_CODEC, com.mo.economy_system.network.packets.login_system.Packet_PlayerLoginRequest::handle);
        registrar.playToServer(com.mo.economy_system.network.packets.login_system.Packet_PlayerLoginResponse.TYPE, com.mo.economy_system.network.packets.login_system.Packet_PlayerLoginResponse.STREAM_CODEC, com.mo.economy_system.network.packets.login_system.Packet_PlayerLoginResponse::handle);
        registrar.playToClient(com.mo.economy_system.network.packets.login_system.Packet_PlayerLoginResult.TYPE, com.mo.economy_system.network.packets.login_system.Packet_PlayerLoginResult.STREAM_CODEC, com.mo.economy_system.network.packets.login_system.Packet_PlayerLoginResult::handle);
        registrar.playToClient(Packet_NoticeCheckResponse.TYPE, Packet_NoticeCheckResponse.STREAM_CODEC, Packet_NoticeCheckResponse::handle);
        registrar.playToServer(Packet_NoticeListRequest.TYPE, Packet_NoticeListRequest.STREAM_CODEC, Packet_NoticeListRequest::handle);
        registrar.playToClient(Packet_NoticeListResponse.TYPE, Packet_NoticeListResponse.STREAM_CODEC, Packet_NoticeListResponse::handle);
        registrar.playToServer(Packet_MarkNoticeReadRequest.TYPE, Packet_MarkNoticeReadRequest.STREAM_CODEC, Packet_MarkNoticeReadRequest::handle);
        registrar.playToClient(Packet_SyncLimbInjury.TYPE, Packet_SyncLimbInjury.STREAM_CODEC, Packet_SyncLimbInjury::handle);
        registrar.playToClient(Packet_OpenRevivalCharmGUI.TYPE, Packet_OpenRevivalCharmGUI.STREAM_CODEC, Packet_OpenRevivalCharmGUI::handle);
        registrar.playToServer(Packet_RevivalRequest.TYPE, Packet_RevivalRequest.STREAM_CODEC, Packet_RevivalRequest::handle);
        registrar.playToClient(Packet_OpenStoryBookGUI.TYPE, Packet_OpenStoryBookGUI.STREAM_CODEC, Packet_OpenStoryBookGUI::handle);
        registrar.playToClient(Packet_OpenStoryFragmentGUI.TYPE, Packet_OpenStoryFragmentGUI.STREAM_CODEC, Packet_OpenStoryFragmentGUI::handle);
        registrar.playToServer(Packet_UpdateStoryBookOrder.TYPE, Packet_UpdateStoryBookOrder.STREAM_CODEC, Packet_UpdateStoryBookOrder::handle);
        registrar.playToClient(Packet_OpenNpcDialogueGUI.TYPE, Packet_OpenNpcDialogueGUI.STREAM_CODEC, Packet_OpenNpcDialogueGUI::handle);
        registrar.playToServer(Packet_NpcInteractionRequest.TYPE, Packet_NpcInteractionRequest.STREAM_CODEC, Packet_NpcInteractionRequest::handle);
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
