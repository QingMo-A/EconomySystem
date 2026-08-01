package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.network.packets.check_system.*;
import com.mo.economy_system.network.packets.economy_system.*;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_ConfirmDemandOrder;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_DeliverDemandOrder;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_RemoveDemandOrder;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_PurchaseSalesOrder;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_RemoveSalesOrder;
import com.mo.economy_system.network.packets.territory_system.*;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.protocol.EconomyMessageRegistry;
import com.mo.economy_system.protocol.EconomyMessageSpec;
import com.mo.economy_system.protocol.EconomyMessageType;
import com.mo.economy_system.protocol.EconomyProtocol;

/** Binds the transitional NeoForge payload classes to the common manifest. */
public final class NeoForge1211MessageBindings {
    private static final EconomyMessageRegistry REGISTRY = createRegistry();

    private NeoForge1211MessageBindings() {
    }

    public static EconomyMessageRegistry registry() {
        return REGISTRY;
    }

    private static EconomyMessageRegistry createRegistry() {
        EconomyMessageRegistry registry = new EconomyMessageRegistry();
        registry.register(EconomyMessages.BALANCE_REQUEST);
        registry.register(EconomyMessages.BALANCE_RESPONSE);
        registry.register(EconomyMessages.BALANCE_LOG_REQUEST);
        registry.register(EconomyMessages.BALANCE_LOG_RESPONSE);
        registry.register(EconomyMessages.TRANSFER);
        registry.register(EconomyMessages.SHOP_DATA_REQUEST);
        registry.register(EconomyMessages.SHOP_DATA_RESPONSE);
        registry.register(EconomyMessages.SHOP_BUY_ITEM);
        registry.register(EconomyMessages.CREATE_SALES_ORDER);
        registry.register(EconomyMessages.CREATE_DEMAND_ORDER);
        registry.register(EconomyMessages.MARKET_DATA_REQUEST);
        registry.register(EconomyMessages.MARKET_DATA_RESPONSE);
        bind(registry, EconomyProtocol.PURCHASE_SALES_ORDER, Packet_PurchaseSalesOrder.class);
        bind(registry, EconomyProtocol.CONFIRM_DEMAND_ORDER, Packet_ConfirmDemandOrder.class);
        bind(registry, EconomyProtocol.DELIVER_DEMAND_ORDER, Packet_DeliverDemandOrder.class);
        bind(registry, EconomyProtocol.REMOVE_SALES_ORDER, Packet_RemoveSalesOrder.class);
        bind(registry, EconomyProtocol.REMOVE_DEMAND_ORDER, Packet_RemoveDemandOrder.class);
        bind(registry, EconomyProtocol.TERRITORY_DATA_REQUEST, Packet_TerritoryDataRequest.class);
        bind(registry, EconomyProtocol.TERRITORY_DATA_RESPONSE, Packet_TerritoryDataResponse.class);
        bind(registry, EconomyProtocol.TELEPORT_TO_TERRITORY, Packet_TeleportToTerritory.class);
        bind(registry, EconomyProtocol.INVITE_PLAYER, Packet_InvitePlayer.class);
        bind(registry, EconomyProtocol.REMOVE_TERRITORY, Packet_RemoveTerritory.class);
        bind(registry, EconomyProtocol.REMOVE_PLAYER, Packet_RemovePlayer.class);
        bind(registry, EconomyProtocol.CHECK, Packet_Check.class);
        bind(registry, EconomyProtocol.CHECK_RESULT_REQUEST, Packet_CheckResultRequest.class);
        bind(registry, EconomyProtocol.CHECK_RESULT_RESPONSE, Packet_CheckResultResponse.class);
        bind(registry, EconomyProtocol.GET, Packet_Get.class);
        bind(registry, EconomyProtocol.GET_RESULT_REQUEST, Packet_GetResultRequest.class);
        bind(registry, EconomyProtocol.GET_RESULT_RESPONSE, Packet_GetResultResponse.class);
        bind(registry, EconomyProtocol.CHUNK, Packet_Chunk.class);
        bind(registry, EconomyProtocol.CHUNK_RESPONSE, Packet_ChunkResponse.class);
        bind(registry, EconomyProtocol.DELIVERY_BOX_DATA_REQUEST, Packet_DeliveryBoxDataRequest.class);
        bind(registry, EconomyProtocol.DELIVERY_BOX_DATA_RESPONSE, Packet_DeliveryBoxDataResponse.class);
        bind(registry, EconomyProtocol.DELIVERY_BOX_CLAIM_ITEM, Packet_DeliveryBoxClaimItem.class);
        registry.register(EconomyMessages.SERVER_PLAYER_LIST_REQUEST);
        registry.register(EconomyMessages.SERVER_PLAYER_LIST_RESPONSE);
        bind(registry, EconomyProtocol.MODIFY_MODE, Packet_ModifyMode.class);
        bind(registry, EconomyProtocol.UNLOCK_TERRITORY_BUFF, Packet_UnlockTerritoryBuff.class);
        bind(registry, EconomyProtocol.UPGRADE_TERRITORY_BUFF, Packet_UpgradeTerritoryBuff.class);
        bind(registry, EconomyProtocol.SINGLE_TERRITORY_DATA_REQUEST, Packet_SingleTerritoryDataRequest.class);
        bind(registry, EconomyProtocol.SINGLE_TERRITORY_DATA_RESPONSE, Packet_SingleTerritoryDataResponse.class);
        bind(registry, EconomyProtocol.UPDATE_TERRITORY_PERMISSION, Packet_UpdateTerritoryPermission.class);
        bind(registry, EconomyProtocol.TRANSFER_TERRITORY_OWNERSHIP, Packet_TransferTerritoryOwnership.class);
        bind(registry, EconomyProtocol.UPDATE_TERRITORY_RULE, Packet_UpdateTerritoryRule.class);
        EconomyProtocol.validateBindings(registry.values());
        registry.freeze();
        return registry;
    }

    private static <T extends EconomyNetworkMessage> void bind(
            EconomyMessageRegistry registry,
            EconomyMessageSpec spec,
            Class<T> messageClass
    ) {
        registry.register(new EconomyMessageType<>(spec, messageClass));
    }
}
