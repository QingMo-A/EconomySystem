package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.BalanceLogRequestMessage;
import com.mo.economy_system.common.network.BalanceLogResponseMessage;
import com.mo.economy_system.common.network.BalanceRequestMessage;
import com.mo.economy_system.common.network.BalanceResponseMessage;
import com.mo.economy_system.common.network.ConfirmDemandOrderMessage;
import com.mo.economy_system.common.network.CreateDemandOrderMessage;
import com.mo.economy_system.common.network.CreateSalesOrderMessage;
import com.mo.economy_system.common.network.DeliverDemandOrderMessage;
import com.mo.economy_system.common.network.MarketDataRequestMessage;
import com.mo.economy_system.common.network.MarketDataResponseMessage;
import com.mo.economy_system.common.network.PurchaseSalesOrderMessage;
import com.mo.economy_system.common.network.RemoveSalesOrderMessage;
import com.mo.economy_system.common.network.RemoveDemandOrderMessage;
import com.mo.economy_system.common.network.ServerPlayerListRequestMessage;
import com.mo.economy_system.common.network.ServerPlayerListResponseMessage;
import com.mo.economy_system.common.network.ShopBuyItemMessage;
import com.mo.economy_system.common.network.ShopDataRequestMessage;
import com.mo.economy_system.common.network.ShopDataResponseMessage;
import com.mo.economy_system.common.network.TransferMessage;
import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.client.ClientTerritoryState;
import com.mo.economy_system.platform.network.EconomyNetworkBridge;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import net.minecraft.server.level.ServerPlayer;

/**
 * Forge delivery adapter. Codec registration is enabled message-by-message as the NeoForge 1.21.1
 * semantic protocol is moved into common.
 */
public final class Forge1201NetworkBridge implements EconomyNetworkBridge {
  @Override
  public void sendToServer(EconomyNetworkMessage message) {
    if (message.getClass() == BalanceRequestMessage.class) {
      Forge1201NetworkChannel.sendToServer((BalanceRequestMessage) message);
      return;
    }
    if (message.getClass() == BalanceLogRequestMessage.class) {
      Forge1201NetworkChannel.sendToServer((BalanceLogRequestMessage) message);
      return;
    }
    if (message.getClass() == TransferMessage.class) {
      Forge1201NetworkChannel.sendToServer((TransferMessage) message);
      return;
    }
    if (message.getClass() == ShopDataRequestMessage.class) {
      Forge1201NetworkChannel.sendToServer((ShopDataRequestMessage) message);
      return;
    }
    if (message.getClass() == ShopBuyItemMessage.class) {
      Forge1201NetworkChannel.sendToServer((ShopBuyItemMessage) message);
      return;
    }
    if (message.getClass() == CreateSalesOrderMessage.class) {
      Forge1201NetworkChannel.sendToServer((CreateSalesOrderMessage) message);
      return;
    }
    if (message.getClass() == CreateDemandOrderMessage.class) {
      Forge1201NetworkChannel.sendToServer((CreateDemandOrderMessage) message);
      return;
    }
    if (message.getClass() == MarketDataRequestMessage.class) {
      Forge1201NetworkChannel.sendToServer((MarketDataRequestMessage) message);
      return;
    }
    if (message.getClass() == PurchaseSalesOrderMessage.class) {
      Forge1201NetworkChannel.sendToServer((PurchaseSalesOrderMessage) message);
      return;
    }
    if (message.getClass() == DeliverDemandOrderMessage.class) {
      Forge1201NetworkChannel.sendToServer((DeliverDemandOrderMessage) message);
      return;
    }
    if (message.getClass() == RemoveSalesOrderMessage.class) {
      Forge1201NetworkChannel.sendToServer((RemoveSalesOrderMessage) message);
      return;
    }
    if (message.getClass() == RemoveDemandOrderMessage.class) {
      Forge1201NetworkChannel.sendToServer((RemoveDemandOrderMessage) message);
      return;
    }
    if (message.getClass() == ConfirmDemandOrderMessage.class) {
      Forge1201NetworkChannel.sendToServer((ConfirmDemandOrderMessage) message);
      return;
    }
    if (message.getClass() == ServerPlayerListRequestMessage.class) {
      Forge1201NetworkChannel.sendToServer((ServerPlayerListRequestMessage) message);
      return;
    }
    if (message.getClass() == TerritoryDataRequestMessage.class) {
      TerritoryDataRequestMessage request = (TerritoryDataRequestMessage) message;
      ClientTerritoryState.begin(request.requestId());
      Forge1201NetworkChannel.sendToServer(request);
      return;
    }
    throw notPorted(message);
  }

  @Override
  public void sendToPlayer(ServerPlayer player, EconomyNetworkMessage message) {
    if (message.getClass() == BalanceResponseMessage.class) {
      Forge1201NetworkChannel.sendToPlayer(player, (BalanceResponseMessage) message);
      return;
    }
    if (message.getClass() == BalanceLogResponseMessage.class) {
      Forge1201NetworkChannel.sendToPlayer(player, (BalanceLogResponseMessage) message);
      return;
    }
    if (message.getClass() == ShopDataResponseMessage.class) {
      Forge1201NetworkChannel.sendToPlayer(player, (ShopDataResponseMessage) message);
      return;
    }
    if (message.getClass() == MarketDataResponseMessage.class) {
      Forge1201NetworkChannel.sendToPlayer(player, (MarketDataResponseMessage) message);
      return;
    }
    if (message.getClass() == ServerPlayerListResponseMessage.class) {
      Forge1201NetworkChannel.sendToPlayer(player, (ServerPlayerListResponseMessage) message);
      return;
    }
    if (message.getClass() == TerritoryDataResponseMessage.class) {
      Forge1201NetworkChannel.sendToPlayer(player, (TerritoryDataResponseMessage) message);
      return;
    }
    throw notPorted(message);
  }

  private static UnsupportedOperationException notPorted(EconomyNetworkMessage message) {
    return new UnsupportedOperationException(
        "Forge 1.20.1 codec is not registered yet for " + message.getClass().getName());
  }
}
