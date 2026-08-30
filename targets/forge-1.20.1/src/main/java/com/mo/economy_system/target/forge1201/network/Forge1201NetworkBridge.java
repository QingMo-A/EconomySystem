package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.BalanceLogRequestMessage;
import com.mo.economy_system.common.network.BalanceLogResponseMessage;
import com.mo.economy_system.common.network.BalanceRequestMessage;
import com.mo.economy_system.common.network.BalanceResponseMessage;
import com.mo.economy_system.common.network.ClientFileCheckRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferChunkRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferChunkResponseMessage;
import com.mo.economy_system.common.network.ConfirmDemandOrderMessage;
import com.mo.economy_system.common.network.CommissionActionResponseMessage;
import com.mo.economy_system.common.network.CommissionDataRequestMessage;
import com.mo.economy_system.common.network.CommissionDataResponseMessage;
import com.mo.economy_system.common.network.CommissionSubmitMessage;
import com.mo.economy_system.common.network.CreateDemandOrderMessage;
import com.mo.economy_system.common.network.CreateSalesOrderMessage;
import com.mo.economy_system.common.network.DeliverDemandOrderMessage;
import com.mo.economy_system.common.network.DeliveryBoxClaimMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataRequestMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataResponseMessage;
import com.mo.economy_system.common.network.InvitePlayerMessage;
import com.mo.economy_system.common.network.MarketDataRequestMessage;
import com.mo.economy_system.common.network.MarketDataResponseMessage;
import com.mo.economy_system.common.network.MailboxClaimAllMessage;
import com.mo.economy_system.common.network.MailboxClaimAttachmentMessage;
import com.mo.economy_system.common.network.MailboxDataRequestMessage;
import com.mo.economy_system.common.network.MailboxDataResponseMessage;
import com.mo.economy_system.common.network.MailboxDeleteMessage;
import com.mo.economy_system.common.network.MailboxMarkReadMessage;
import com.mo.economy_system.common.network.MailboxNotificationMessage;
import com.mo.economy_system.common.network.MailboxSendPlayerMessage;
import com.mo.economy_system.common.network.MailboxSendResultMessage;
import com.mo.economy_system.common.network.ModifyTerritoryModeMessage;
import com.mo.economy_system.common.network.PurchaseSalesOrderMessage;
import com.mo.economy_system.common.network.RemoveDemandOrderMessage;
import com.mo.economy_system.common.network.RemoveSalesOrderMessage;
import com.mo.economy_system.common.network.RemoveTerritoryMemberMessage;
import com.mo.economy_system.common.network.RemoveTerritoryMessage;
import com.mo.economy_system.common.network.ServerPlayerListRequestMessage;
import com.mo.economy_system.common.network.ServerPlayerListResponseMessage;
import com.mo.economy_system.common.network.SingleTerritoryDataRequestMessage;
import com.mo.economy_system.common.network.SingleTerritoryDataResponseMessage;
import com.mo.economy_system.common.network.ShopBuyItemMessage;
import com.mo.economy_system.common.network.ShopDataRequestMessage;
import com.mo.economy_system.common.network.ShopDataResponseMessage;
import com.mo.economy_system.common.network.TeleportToTerritoryMessage;
import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.network.TransferTerritoryOwnershipMessage;
import com.mo.economy_system.common.network.TransferMessage;
import com.mo.economy_system.common.network.RecycleDataRequestMessage;
import com.mo.economy_system.common.network.RecycleDataResponseMessage;
import com.mo.economy_system.common.network.RecycleSubmitMessage;
import com.mo.economy_system.common.network.RecycleActionResponseMessage;
import com.mo.economy_system.common.network.UnlockTerritoryBuffMessage;
import com.mo.economy_system.common.network.UpdateTerritoryPermissionMessage;
import com.mo.economy_system.common.network.UpdateTerritoryRuleMessage;
import com.mo.economy_system.common.network.UpgradeTerritoryBuffMessage;
import com.mo.economy_system.platform.network.EconomyNetworkBridge;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.target.forge1201.Forge1201Platform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Forge delivery adapter. Codec registration is enabled message-by-message as the NeoForge 1.21.1
 * semantic protocol is moved into common.
 */
public final class Forge1201NetworkBridge implements EconomyNetworkBridge {
  @Override
  public void sendToServer(EconomyNetworkMessage message) {
    if(message instanceof CheckedFileTransferControlRequestMessage value){Forge1201NetworkChannel.sendToServer(value);return;}
    if(message instanceof CheckedFileTransferChunkRequestMessage value){Forge1201NetworkChannel.sendToServer(value);return;}
    if (message.getClass() == ClientFileCheckResultRequestMessage.class) {
      Forge1201NetworkChannel.sendToServer((ClientFileCheckResultRequestMessage) message);
      return;
    }
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
      Forge1201NetworkChannel.sendToServer((TerritoryDataRequestMessage) message);
      return;
    }
    if (message.getClass() == TeleportToTerritoryMessage.class) {
      Forge1201NetworkChannel.sendToServer((TeleportToTerritoryMessage) message);
      return;
    }
    if (message.getClass() == InvitePlayerMessage.class) {
      Forge1201NetworkChannel.sendToServer((InvitePlayerMessage) message);
      return;
    }
    if (message.getClass() == RemoveTerritoryMessage.class) {
      Forge1201NetworkChannel.sendToServer((RemoveTerritoryMessage) message);
      return;
    }
    if (message.getClass() == RemoveTerritoryMemberMessage.class) {
      Forge1201NetworkChannel.sendToServer((RemoveTerritoryMemberMessage) message);
      return;
    }
    if (message.getClass() == DeliveryBoxDataRequestMessage.class) {
      Forge1201NetworkChannel.sendToServer((DeliveryBoxDataRequestMessage) message);
      return;
    }
    if (message.getClass() == DeliveryBoxClaimMessage.class) {
      Forge1201NetworkChannel.sendToServer((DeliveryBoxClaimMessage) message);
      return;
    }
    if (message instanceof MailboxDataRequestMessage value) { Forge1201NetworkChannel.sendToServer(value); return; }
    if (message instanceof MailboxMarkReadMessage value) { Forge1201NetworkChannel.sendToServer(value); return; }
    if (message instanceof MailboxDeleteMessage value) { Forge1201NetworkChannel.sendToServer(value); return; }
    if (message instanceof MailboxClaimAttachmentMessage value) { Forge1201NetworkChannel.sendToServer(value); return; }
    if (message instanceof MailboxClaimAllMessage value) { Forge1201NetworkChannel.sendToServer(value); return; }
    if (message instanceof MailboxSendPlayerMessage value) { Forge1201NetworkChannel.sendToServer(value); return; }
    if (message instanceof CommissionDataRequestMessage value) { Forge1201NetworkChannel.sendToServer(value); return; }
    if (message instanceof CommissionSubmitMessage value) { Forge1201NetworkChannel.sendToServer(value); return; }
    if (message instanceof RecycleDataRequestMessage value) { Forge1201NetworkChannel.sendToServer(value); return; }
    if (message instanceof RecycleSubmitMessage value) { Forge1201NetworkChannel.sendToServer(value); return; }
    if (message.getClass() == ModifyTerritoryModeMessage.class) {
      Forge1201NetworkChannel.sendToServer((ModifyTerritoryModeMessage) message);
      return;
    }
    if (message.getClass() == UnlockTerritoryBuffMessage.class) {
      Forge1201NetworkChannel.sendToServer((UnlockTerritoryBuffMessage) message);
      return;
    }
    if (message.getClass() == UpgradeTerritoryBuffMessage.class) {
      Forge1201NetworkChannel.sendToServer((UpgradeTerritoryBuffMessage) message);
      return;
    }
    if (message.getClass() == SingleTerritoryDataRequestMessage.class) {
      Forge1201NetworkChannel.sendToServer((SingleTerritoryDataRequestMessage) message);
      return;
    }
    if (message.getClass() == UpdateTerritoryPermissionMessage.class) {
      Forge1201NetworkChannel.sendToServer((UpdateTerritoryPermissionMessage) message);
      return;
    }
    if (message.getClass() == TransferTerritoryOwnershipMessage.class) {
      Forge1201NetworkChannel.sendToServer((TransferTerritoryOwnershipMessage) message);
      return;
    }
    if (message.getClass() == UpdateTerritoryRuleMessage.class) {
      Forge1201NetworkChannel.sendToServer((UpdateTerritoryRuleMessage) message);
      return;
    }
    throw notPorted(message);
  }

  @Override
  public void sendToPlayer(UUID playerId, EconomyNetworkMessage message) {
    MinecraftServer server = Forge1201Platform.activeServer();
    if (server == null) return;
    ServerPlayer player = server.getPlayerList().getPlayer(playerId);
    if (player == null) return;
    if(message instanceof CheckedFileTransferRequestMessage value){Forge1201NetworkChannel.sendToPlayer(player,value);return;}
    if(message instanceof CheckedFileTransferControlResponseMessage value){Forge1201NetworkChannel.sendToPlayer(player,value);return;}
    if(message instanceof CheckedFileTransferChunkResponseMessage value){Forge1201NetworkChannel.sendToPlayer(player,value);return;}
    if (message.getClass() == ClientFileCheckRequestMessage.class) {
      Forge1201NetworkChannel.sendToPlayer(player, (ClientFileCheckRequestMessage) message);
      return;
    }
    if (message.getClass() == ClientFileCheckResultResponseMessage.class) {
      Forge1201NetworkChannel.sendToPlayer(player, (ClientFileCheckResultResponseMessage) message);
      return;
    }
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
    if (message.getClass() == DeliveryBoxDataResponseMessage.class) {
      Forge1201NetworkChannel.sendToPlayer(player, (DeliveryBoxDataResponseMessage) message);
      return;
    }
    if (message instanceof MailboxDataResponseMessage value) {
      Forge1201NetworkChannel.sendToPlayer(player, value);
      return;
    }
    if (message instanceof MailboxSendResultMessage value) {
      Forge1201NetworkChannel.sendToPlayer(player, value);
      return;
    }
    if (message instanceof MailboxNotificationMessage value) {
      Forge1201NetworkChannel.sendToPlayer(player, value);
      return;
    }
    if (message instanceof CommissionDataResponseMessage value) {
      Forge1201NetworkChannel.sendToPlayer(player, value);
      return;
    }
    if (message instanceof CommissionActionResponseMessage value) {
      Forge1201NetworkChannel.sendToPlayer(player, value);
      return;
    }
    if (message instanceof RecycleDataResponseMessage value) { Forge1201NetworkChannel.sendToPlayer(player, value); return; }
    if (message instanceof RecycleActionResponseMessage value) { Forge1201NetworkChannel.sendToPlayer(player, value); return; }
    if (message.getClass() == SingleTerritoryDataResponseMessage.class) {
      Forge1201NetworkChannel.sendToPlayer(player, (SingleTerritoryDataResponseMessage) message);
      return;
    }
    throw notPorted(message);
  }

  private static UnsupportedOperationException notPorted(EconomyNetworkMessage message) {
    return new UnsupportedOperationException(
        "Forge 1.20.1 codec is not registered yet for " + message.getClass().getName());
  }
}
