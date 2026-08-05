package com.mo.economy_system.common.network;

import com.mo.economy_system.protocol.EconomyMessageType;
import com.mo.economy_system.protocol.EconomyProtocol;

/** Stable common message types migrated away from loader payload classes. */
public final class EconomyMessages {
  public static final EconomyMessageType<BalanceRequestMessage> BALANCE_REQUEST =
      new EconomyMessageType<>(EconomyProtocol.BALANCE_REQUEST, BalanceRequestMessage.class);

  public static final EconomyMessageType<BalanceResponseMessage> BALANCE_RESPONSE =
      new EconomyMessageType<>(EconomyProtocol.BALANCE_RESPONSE, BalanceResponseMessage.class);

  public static final EconomyMessageType<BalanceLogRequestMessage> BALANCE_LOG_REQUEST =
      new EconomyMessageType<>(EconomyProtocol.BALANCE_LOG_REQUEST, BalanceLogRequestMessage.class);

  public static final EconomyMessageType<BalanceLogResponseMessage> BALANCE_LOG_RESPONSE =
      new EconomyMessageType<>(
          EconomyProtocol.BALANCE_LOG_RESPONSE, BalanceLogResponseMessage.class);

  public static final EconomyMessageType<TransferMessage> TRANSFER =
      new EconomyMessageType<>(EconomyProtocol.TRANSFER, TransferMessage.class);

  public static final EconomyMessageType<ShopDataRequestMessage> SHOP_DATA_REQUEST =
      new EconomyMessageType<>(EconomyProtocol.SHOP_DATA_REQUEST, ShopDataRequestMessage.class);

  public static final EconomyMessageType<ShopDataResponseMessage> SHOP_DATA_RESPONSE =
      new EconomyMessageType<>(EconomyProtocol.SHOP_DATA_RESPONSE, ShopDataResponseMessage.class);

  public static final EconomyMessageType<ShopBuyItemMessage> SHOP_BUY_ITEM =
      new EconomyMessageType<>(EconomyProtocol.SHOP_BUY_ITEM, ShopBuyItemMessage.class);

  public static final EconomyMessageType<CreateSalesOrderMessage> CREATE_SALES_ORDER =
      new EconomyMessageType<>(EconomyProtocol.CREATE_SALES_ORDER, CreateSalesOrderMessage.class);

  public static final EconomyMessageType<CreateDemandOrderMessage> CREATE_DEMAND_ORDER =
      new EconomyMessageType<>(EconomyProtocol.CREATE_DEMAND_ORDER, CreateDemandOrderMessage.class);

  public static final EconomyMessageType<MarketDataRequestMessage> MARKET_DATA_REQUEST =
      new EconomyMessageType<>(EconomyProtocol.MARKET_DATA_REQUEST, MarketDataRequestMessage.class);
  public static final EconomyMessageType<MarketDataResponseMessage> MARKET_DATA_RESPONSE =
      new EconomyMessageType<>(
          EconomyProtocol.MARKET_DATA_RESPONSE, MarketDataResponseMessage.class);
  public static final EconomyMessageType<PurchaseSalesOrderMessage> PURCHASE_SALES_ORDER =
      new EconomyMessageType<>(
          EconomyProtocol.PURCHASE_SALES_ORDER, PurchaseSalesOrderMessage.class);
  public static final EconomyMessageType<RemoveSalesOrderMessage> REMOVE_SALES_ORDER =
      new EconomyMessageType<>(EconomyProtocol.REMOVE_SALES_ORDER, RemoveSalesOrderMessage.class);
  public static final EconomyMessageType<ConfirmDemandOrderMessage> CONFIRM_DEMAND_ORDER =
      new EconomyMessageType<>(
          EconomyProtocol.CONFIRM_DEMAND_ORDER, ConfirmDemandOrderMessage.class);
  public static final EconomyMessageType<DeliverDemandOrderMessage> DELIVER_DEMAND_ORDER =
      new EconomyMessageType<>(
          EconomyProtocol.DELIVER_DEMAND_ORDER, DeliverDemandOrderMessage.class);
  public static final EconomyMessageType<RemoveDemandOrderMessage> REMOVE_DEMAND_ORDER =
      new EconomyMessageType<>(EconomyProtocol.REMOVE_DEMAND_ORDER, RemoveDemandOrderMessage.class);
  public static final EconomyMessageType<TerritoryDataRequestMessage> TERRITORY_DATA_REQUEST =
      new EconomyMessageType<>(
          EconomyProtocol.TERRITORY_DATA_REQUEST, TerritoryDataRequestMessage.class);
  public static final EconomyMessageType<TerritoryDataResponseMessage> TERRITORY_DATA_RESPONSE =
      new EconomyMessageType<>(
          EconomyProtocol.TERRITORY_DATA_RESPONSE, TerritoryDataResponseMessage.class);
  public static final EconomyMessageType<TeleportToTerritoryMessage> TELEPORT_TO_TERRITORY =
      new EconomyMessageType<>(
          EconomyProtocol.TELEPORT_TO_TERRITORY, TeleportToTerritoryMessage.class);
  public static final EconomyMessageType<InvitePlayerMessage> INVITE_PLAYER =
      new EconomyMessageType<>(EconomyProtocol.INVITE_PLAYER, InvitePlayerMessage.class);
  public static final EconomyMessageType<RemoveTerritoryMessage> REMOVE_TERRITORY =
      new EconomyMessageType<>(EconomyProtocol.REMOVE_TERRITORY, RemoveTerritoryMessage.class);
  public static final EconomyMessageType<RemoveTerritoryMemberMessage> REMOVE_PLAYER =
      new EconomyMessageType<>(EconomyProtocol.REMOVE_PLAYER, RemoveTerritoryMemberMessage.class);
  public static final EconomyMessageType<ClientFileCheckRequestMessage> CHECK =
      new EconomyMessageType<>(EconomyProtocol.CHECK, ClientFileCheckRequestMessage.class);
  public static final EconomyMessageType<ClientFileCheckResultRequestMessage> CHECK_RESULT_REQUEST =
      new EconomyMessageType<>(
          EconomyProtocol.CHECK_RESULT_REQUEST, ClientFileCheckResultRequestMessage.class);
  public static final EconomyMessageType<ClientFileCheckResultResponseMessage>
      CHECK_RESULT_RESPONSE =
          new EconomyMessageType<>(
              EconomyProtocol.CHECK_RESULT_RESPONSE, ClientFileCheckResultResponseMessage.class);
  public static final EconomyMessageType<CheckedFileTransferRequestMessage> GET =
      new EconomyMessageType<>(EconomyProtocol.GET, CheckedFileTransferRequestMessage.class);
  public static final EconomyMessageType<CheckedFileTransferControlRequestMessage> GET_RESULT_REQUEST =
      new EconomyMessageType<>(EconomyProtocol.GET_RESULT_REQUEST, CheckedFileTransferControlRequestMessage.class);
  public static final EconomyMessageType<CheckedFileTransferControlResponseMessage> GET_RESULT_RESPONSE =
      new EconomyMessageType<>(EconomyProtocol.GET_RESULT_RESPONSE, CheckedFileTransferControlResponseMessage.class);
  public static final EconomyMessageType<CheckedFileTransferChunkRequestMessage> CHUNK =
      new EconomyMessageType<>(EconomyProtocol.CHUNK, CheckedFileTransferChunkRequestMessage.class);
  public static final EconomyMessageType<CheckedFileTransferChunkResponseMessage> CHUNK_RESPONSE =
      new EconomyMessageType<>(EconomyProtocol.CHUNK_RESPONSE, CheckedFileTransferChunkResponseMessage.class);

  public static final EconomyMessageType<ServerPlayerListRequestMessage>
      SERVER_PLAYER_LIST_REQUEST =
          new EconomyMessageType<>(
              EconomyProtocol.SERVER_PLAYER_LIST_REQUEST, ServerPlayerListRequestMessage.class);

  public static final EconomyMessageType<ServerPlayerListResponseMessage>
      SERVER_PLAYER_LIST_RESPONSE =
          new EconomyMessageType<>(
              EconomyProtocol.SERVER_PLAYER_LIST_RESPONSE, ServerPlayerListResponseMessage.class);

  private EconomyMessages() {}
}
