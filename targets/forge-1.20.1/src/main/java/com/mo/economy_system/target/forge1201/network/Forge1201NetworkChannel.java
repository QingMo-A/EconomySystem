package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.common.network.AccountBalance;
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
import com.mo.economy_system.common.network.CreateDemandOrderMessage;
import com.mo.economy_system.common.network.CreateSalesOrderMessage;
import com.mo.economy_system.common.network.DeliverDemandOrderMessage;
import com.mo.economy_system.common.network.DeliveryBoxClaimMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataRequestMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataResponseMessage;
import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
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
import com.mo.economy_system.common.network.PlayerSummary;
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
import com.mo.economy_system.common.network.ShopItemSnapshot;
import com.mo.economy_system.common.network.TeleportToTerritoryMessage;
import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.network.TransferTerritoryOwnershipMessage;
import com.mo.economy_system.common.network.TransferMessage;
import com.mo.economy_system.common.network.UnlockTerritoryBuffMessage;
import com.mo.economy_system.common.network.UpdateTerritoryPermissionMessage;
import com.mo.economy_system.common.network.UpdateTerritoryRuleMessage;
import com.mo.economy_system.common.network.UpgradeTerritoryBuffMessage;
import com.mo.economy_system.core.economy_system.BalanceLogEntry;
import com.mo.economy_system.network.ClientFileCheckWireCodec;
import com.mo.economy_system.network.CheckedFileTransferWireCodec;
import com.mo.economy_system.network.DeliveryBoxWireCodec;
import com.mo.economy_system.network.MailboxWireCodec;
import com.mo.economy_system.network.TerritoryInviteWireCodec;
import com.mo.economy_system.network.TerritoryManagementWireCodec;
import com.mo.economy_system.network.TerritoryMemberRemovalWireCodec;
import com.mo.economy_system.network.TerritoryRemovalWireCodec;
import com.mo.economy_system.network.TerritoryTeleportWireCodec;
import com.mo.economy_system.platform.network.WireBuffer;
import com.mo.economy_system.protocol.EconomyProtocol;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Forge 1.20.1 wire adapter for common protocol messages ported so far. */
public final class Forge1201NetworkChannel {
  private static final SimpleChannel CHANNEL =
      NetworkRegistry.newSimpleChannel(
          new ResourceLocation(EconomyConstants.MOD_ID, "bridge"),
          () -> EconomyProtocol.VERSION,
          EconomyProtocol.VERSION::equals,
          EconomyProtocol.VERSION::equals);

  private static volatile boolean registered;

  private Forge1201NetworkChannel() {}

  public static synchronized void register() {
    if (registered) {
      return;
    }

    CHANNEL
        .messageBuilder(
            ClientFileCheckRequestMessage.class,
            EconomyMessages.CHECK.discriminator(),
            NetworkDirection.PLAY_TO_CLIENT)
        .encoder(targetEncoder(ClientFileCheckWireCodec::encodeRequest))
        .decoder(targetDecoder(ClientFileCheckWireCodec::decodeRequest))
        .consumerMainThread(Forge1201ClientFileCheckRequestHandler::handle)
        .add();
    CHANNEL
        .messageBuilder(
            ClientFileCheckResultRequestMessage.class,
            EconomyMessages.CHECK_RESULT_REQUEST.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(ClientFileCheckWireCodec::encodeResultRequest))
        .decoder(targetDecoder(ClientFileCheckWireCodec::decodeResultRequest))
        .consumerMainThread(Forge1201ClientFileCheckResultRequestHandler::handle)
        .add();
    CHANNEL
        .messageBuilder(
            ClientFileCheckResultResponseMessage.class,
            EconomyMessages.CHECK_RESULT_RESPONSE.discriminator(),
            NetworkDirection.PLAY_TO_CLIENT)
        .encoder(targetEncoder(ClientFileCheckWireCodec::encodeResultResponse))
        .decoder(targetDecoder(ClientFileCheckWireCodec::decodeResultResponse))
        .consumerMainThread(Forge1201ClientFileCheckResultResponseHandler::handle)
        .add();
    CHANNEL.messageBuilder(CheckedFileTransferRequestMessage.class,EconomyMessages.GET.discriminator(),NetworkDirection.PLAY_TO_CLIENT).encoder(targetEncoder(CheckedFileTransferWireCodec::encodeRequest)).decoder(targetDecoder(CheckedFileTransferWireCodec::decodeRequest)).consumerMainThread(Forge1201CheckedFileTransferHandlers::request).add();
    CHANNEL.messageBuilder(CheckedFileTransferControlRequestMessage.class,EconomyMessages.GET_RESULT_REQUEST.discriminator(),NetworkDirection.PLAY_TO_SERVER).encoder(targetEncoder(CheckedFileTransferWireCodec::encodeControlRequest)).decoder(targetDecoder(CheckedFileTransferWireCodec::decodeControlRequest)).consumerMainThread(Forge1201CheckedFileTransferHandlers::controlRequest).add();
    CHANNEL.messageBuilder(CheckedFileTransferControlResponseMessage.class,EconomyMessages.GET_RESULT_RESPONSE.discriminator(),NetworkDirection.PLAY_TO_CLIENT).encoder(targetEncoder(CheckedFileTransferWireCodec::encodeControlResponse)).decoder(targetDecoder(CheckedFileTransferWireCodec::decodeControlResponse)).consumerMainThread(Forge1201CheckedFileTransferHandlers::controlResponse).add();
    CHANNEL.messageBuilder(CheckedFileTransferChunkRequestMessage.class,EconomyMessages.CHUNK.discriminator(),NetworkDirection.PLAY_TO_SERVER).encoder(targetEncoder(CheckedFileTransferWireCodec::encodeChunkRequest)).decoder(targetDecoder(CheckedFileTransferWireCodec::decodeChunkRequest)).consumerMainThread(Forge1201CheckedFileTransferHandlers::chunkRequest).add();
    CHANNEL.messageBuilder(CheckedFileTransferChunkResponseMessage.class,EconomyMessages.CHUNK_RESPONSE.discriminator(),NetworkDirection.PLAY_TO_CLIENT).encoder(targetEncoder(CheckedFileTransferWireCodec::encodeChunkResponse)).decoder(targetDecoder(CheckedFileTransferWireCodec::decodeChunkResponse)).consumerMainThread(Forge1201CheckedFileTransferHandlers::chunkResponse).add();

    CHANNEL
        .messageBuilder(
            BalanceRequestMessage.class,
            EconomyMessages.BALANCE_REQUEST.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(Forge1201NetworkChannel::encodeBalanceRequest)
        .decoder(Forge1201NetworkChannel::decodeBalanceRequest)
        .consumerMainThread(Forge1201BalanceHandlers::handleRequest)
        .add();

    CHANNEL
        .messageBuilder(
            BalanceResponseMessage.class,
            EconomyMessages.BALANCE_RESPONSE.discriminator(),
            NetworkDirection.PLAY_TO_CLIENT)
        .encoder(Forge1201NetworkChannel::encodeBalanceResponse)
        .decoder(Forge1201NetworkChannel::decodeBalanceResponse)
        .consumerMainThread(Forge1201BalanceHandlers::handleResponse)
        .add();

    CHANNEL
        .messageBuilder(
            BalanceLogRequestMessage.class,
            EconomyMessages.BALANCE_LOG_REQUEST.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(Forge1201NetworkChannel::encodeBalanceLogRequest)
        .decoder(Forge1201NetworkChannel::decodeBalanceLogRequest)
        .consumerMainThread(Forge1201BalanceLogHandlers::handleRequest)
        .add();

    CHANNEL
        .messageBuilder(
            BalanceLogResponseMessage.class,
            EconomyMessages.BALANCE_LOG_RESPONSE.discriminator(),
            NetworkDirection.PLAY_TO_CLIENT)
        .encoder(Forge1201NetworkChannel::encodeBalanceLogResponse)
        .decoder(Forge1201NetworkChannel::decodeBalanceLogResponse)
        .consumerMainThread(Forge1201BalanceLogHandlers::handleResponse)
        .add();

    CHANNEL
        .messageBuilder(
            TransferMessage.class,
            EconomyMessages.TRANSFER.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(Forge1201NetworkChannel::encodeTransfer)
        .decoder(Forge1201NetworkChannel::decodeTransfer)
        .consumerMainThread(Forge1201TransferHandler::handle)
        .add();

    CHANNEL
        .messageBuilder(
            ShopDataRequestMessage.class,
            EconomyMessages.SHOP_DATA_REQUEST.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(Forge1201NetworkChannel::encodeShopDataRequest)
        .decoder(Forge1201NetworkChannel::decodeShopDataRequest)
        .consumerMainThread(Forge1201ShopDataHandlers::handleRequest)
        .add();

    CHANNEL
        .messageBuilder(
            ShopDataResponseMessage.class,
            EconomyMessages.SHOP_DATA_RESPONSE.discriminator(),
            NetworkDirection.PLAY_TO_CLIENT)
        .encoder(Forge1201NetworkChannel::encodeShopDataResponse)
        .decoder(Forge1201NetworkChannel::decodeShopDataResponse)
        .consumerMainThread(Forge1201ShopDataHandlers::handleResponse)
        .add();

    CHANNEL
        .messageBuilder(
            ShopBuyItemMessage.class,
            EconomyMessages.SHOP_BUY_ITEM.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(Forge1201NetworkChannel::encodeShopBuyItem)
        .decoder(Forge1201NetworkChannel::decodeShopBuyItem)
        .consumerMainThread(Forge1201ShopPurchaseHandler::handle)
        .add();

    CHANNEL
        .messageBuilder(
            CreateSalesOrderMessage.class,
            EconomyMessages.CREATE_SALES_ORDER.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(Forge1201CreateSalesOrderCodec::encode)
        .decoder(Forge1201CreateSalesOrderCodec::decode)
        .consumerMainThread(Forge1201CreateSalesOrderHandler::handle)
        .add();

    CHANNEL
        .messageBuilder(
            CreateDemandOrderMessage.class,
            EconomyMessages.CREATE_DEMAND_ORDER.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(Forge1201CreateDemandOrderCodec::encode)
        .decoder(Forge1201CreateDemandOrderCodec::decode)
        .consumerMainThread(Forge1201CreateDemandOrderHandler::handle)
        .add();

    CHANNEL
        .messageBuilder(
            MarketDataRequestMessage.class,
            EconomyMessages.MARKET_DATA_REQUEST.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(Forge1201MarketDataCodec::encodeRequest)
        .decoder(Forge1201MarketDataCodec::decodeRequest)
        .consumerMainThread(Forge1201MarketDataHandlers::request)
        .add();
    CHANNEL
        .messageBuilder(
            MarketDataResponseMessage.class,
            EconomyMessages.MARKET_DATA_RESPONSE.discriminator(),
            NetworkDirection.PLAY_TO_CLIENT)
        .encoder(Forge1201MarketDataCodec::encodeResponse)
        .decoder(Forge1201MarketDataCodec::decodeResponse)
        .consumerMainThread(Forge1201MarketDataHandlers::response)
        .add();
    CHANNEL
        .messageBuilder(
            PurchaseSalesOrderMessage.class,
            EconomyMessages.PURCHASE_SALES_ORDER.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(Forge1201PurchaseSalesOrderCodec::encode)
        .decoder(Forge1201PurchaseSalesOrderCodec::decode)
        .consumerMainThread(Forge1201PurchaseSalesOrderHandler::handle)
        .add();
    CHANNEL
        .messageBuilder(
            RemoveSalesOrderMessage.class,
            EconomyMessages.REMOVE_SALES_ORDER.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(Forge1201RemoveSalesOrderCodec::encode)
        .decoder(Forge1201RemoveSalesOrderCodec::decode)
        .consumerMainThread(Forge1201RemoveSalesOrderHandler::handle)
        .add();
    CHANNEL
        .messageBuilder(
            ConfirmDemandOrderMessage.class,
            EconomyMessages.CONFIRM_DEMAND_ORDER.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(Forge1201ConfirmDemandOrderCodec::encode)
        .decoder(Forge1201ConfirmDemandOrderCodec::decode)
        .consumerMainThread(Forge1201ConfirmDemandOrderHandler::handle)
        .add();
    CHANNEL
        .messageBuilder(
            DeliverDemandOrderMessage.class,
            EconomyMessages.DELIVER_DEMAND_ORDER.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(Forge1201DeliverDemandOrderCodec::encode)
        .decoder(Forge1201DeliverDemandOrderCodec::decode)
        .consumerMainThread(Forge1201DeliverDemandOrderHandler::handle)
        .add();
    CHANNEL
        .messageBuilder(
            RemoveDemandOrderMessage.class,
            EconomyMessages.REMOVE_DEMAND_ORDER.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(Forge1201RemoveDemandOrderCodec::encode)
        .decoder(Forge1201RemoveDemandOrderCodec::decode)
        .consumerMainThread(Forge1201RemoveDemandOrderHandler::handle)
        .add();
    CHANNEL
        .messageBuilder(
            TerritoryDataRequestMessage.class,
            EconomyMessages.TERRITORY_DATA_REQUEST.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(Forge1201TerritoryDataCodec::encodeRequest)
        .decoder(Forge1201TerritoryDataCodec::decodeRequest)
        .consumerMainThread(Forge1201TerritoryDataHandlers::handleRequest)
        .add();
    CHANNEL
        .messageBuilder(
            TerritoryDataResponseMessage.class,
            EconomyMessages.TERRITORY_DATA_RESPONSE.discriminator(),
            NetworkDirection.PLAY_TO_CLIENT)
        .encoder(Forge1201TerritoryDataCodec::encodeResponse)
        .decoder(Forge1201TerritoryDataCodec::decodeResponse)
        .consumerMainThread(Forge1201TerritoryDataClientDispatch::handle)
        .add();
    CHANNEL
        .messageBuilder(
            TeleportToTerritoryMessage.class,
            EconomyMessages.TELEPORT_TO_TERRITORY.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(TerritoryTeleportWireCodec::encode))
        .decoder(targetDecoder(TerritoryTeleportWireCodec::decode))
        .consumerMainThread(Forge1201TerritoryTeleportHandler::handle)
        .add();
    CHANNEL
        .messageBuilder(
            InvitePlayerMessage.class,
            EconomyProtocol.INVITE_PLAYER.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(TerritoryInviteWireCodec::encode))
        .decoder(targetDecoder(TerritoryInviteWireCodec::decode))
        .consumerMainThread(Forge1201TerritoryInviteHandler::handle)
        .add();
    CHANNEL
        .messageBuilder(
            RemoveTerritoryMessage.class,
            EconomyMessages.REMOVE_TERRITORY.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(TerritoryRemovalWireCodec::encode))
        .decoder(targetDecoder(TerritoryRemovalWireCodec::decode))
        .consumerMainThread(Forge1201TerritoryRemovalHandler::handle)
        .add();
    CHANNEL
        .messageBuilder(
            RemoveTerritoryMemberMessage.class,
            EconomyMessages.REMOVE_PLAYER.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(TerritoryMemberRemovalWireCodec::encode))
        .decoder(targetDecoder(TerritoryMemberRemovalWireCodec::decode))
        .consumerMainThread(Forge1201TerritoryMemberRemovalHandler::handle)
        .add();

    CHANNEL
        .messageBuilder(
            DeliveryBoxDataRequestMessage.class,
            EconomyMessages.DELIVERY_BOX_DATA_REQUEST.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(DeliveryBoxWireCodec::encodeRequest))
        .decoder(targetDecoder(DeliveryBoxWireCodec::decodeRequest))
        .consumerMainThread(Forge1201DeliveryBoxHandlers::request)
        .add();
    CHANNEL
        .messageBuilder(
            DeliveryBoxDataResponseMessage.class,
            EconomyMessages.DELIVERY_BOX_DATA_RESPONSE.discriminator(),
            NetworkDirection.PLAY_TO_CLIENT)
        .encoder(targetEncoder(DeliveryBoxWireCodec::encodeResponse))
        .decoder(targetDecoder(DeliveryBoxWireCodec::decodeResponse))
        .consumerMainThread(Forge1201DeliveryBoxHandlers::response)
        .add();
    CHANNEL
        .messageBuilder(
            DeliveryBoxClaimMessage.class,
            EconomyMessages.DELIVERY_BOX_CLAIM_ITEM.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(DeliveryBoxWireCodec::encodeClaim))
        .decoder(targetDecoder(DeliveryBoxWireCodec::decodeClaim))
        .consumerMainThread(Forge1201DeliveryBoxHandlers::claim)
        .add();
    CHANNEL.messageBuilder(MailboxDataRequestMessage.class, EconomyMessages.MAILBOX_DATA_REQUEST.discriminator(), NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(MailboxWireCodec::encodeRequest)).decoder(targetDecoder(MailboxWireCodec::decodeRequest))
        .consumerMainThread(Forge1201MailboxHandlers::request).add();
    CHANNEL.messageBuilder(MailboxDataResponseMessage.class, EconomyMessages.MAILBOX_DATA_RESPONSE.discriminator(), NetworkDirection.PLAY_TO_CLIENT)
        .encoder(targetEncoder(MailboxWireCodec::encodeResponse)).decoder(targetDecoder(MailboxWireCodec::decodeResponse))
        .consumerMainThread(Forge1201MailboxHandlers::response).add();
    CHANNEL.messageBuilder(MailboxMarkReadMessage.class, EconomyMessages.MAILBOX_MARK_READ.discriminator(), NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(MailboxWireCodec::encodeMarkRead)).decoder(targetDecoder(MailboxWireCodec::decodeMarkRead))
        .consumerMainThread(Forge1201MailboxHandlers::markRead).add();
    CHANNEL.messageBuilder(MailboxDeleteMessage.class, EconomyMessages.MAILBOX_DELETE.discriminator(), NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(MailboxWireCodec::encodeDelete)).decoder(targetDecoder(MailboxWireCodec::decodeDelete))
        .consumerMainThread(Forge1201MailboxHandlers::delete).add();
    CHANNEL.messageBuilder(MailboxClaimAttachmentMessage.class, EconomyMessages.MAILBOX_CLAIM_ATTACHMENT.discriminator(), NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(MailboxWireCodec::encodeClaimAttachment)).decoder(targetDecoder(MailboxWireCodec::decodeClaimAttachment))
        .consumerMainThread(Forge1201MailboxHandlers::claimAttachment).add();
    CHANNEL.messageBuilder(MailboxClaimAllMessage.class, EconomyMessages.MAILBOX_CLAIM_ALL.discriminator(), NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(MailboxWireCodec::encodeClaimAll)).decoder(targetDecoder(MailboxWireCodec::decodeClaimAll))
        .consumerMainThread(Forge1201MailboxHandlers::claimAll).add();
    CHANNEL.messageBuilder(MailboxSendPlayerMessage.class, EconomyMessages.MAILBOX_SEND_PLAYER.discriminator(), NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(MailboxWireCodec::encodeSendPlayer)).decoder(targetDecoder(MailboxWireCodec::decodeSendPlayer))
        .consumerMainThread(Forge1201MailboxHandlers::sendPlayer).add();
    CHANNEL.messageBuilder(MailboxSendResultMessage.class, EconomyMessages.MAILBOX_SEND_RESULT.discriminator(), NetworkDirection.PLAY_TO_CLIENT)
        .encoder(targetEncoder(MailboxWireCodec::encodeSendResult)).decoder(targetDecoder(MailboxWireCodec::decodeSendResult))
        .consumerMainThread(Forge1201MailboxHandlers::sendResult).add();
    CHANNEL.messageBuilder(MailboxNotificationMessage.class, EconomyMessages.MAILBOX_NOTIFICATION.discriminator(), NetworkDirection.PLAY_TO_CLIENT)
        .encoder(targetEncoder(MailboxWireCodec::encodeNotification)).decoder(targetDecoder(MailboxWireCodec::decodeNotification))
        .consumerMainThread(Forge1201MailboxHandlers::notification).add();

    CHANNEL
        .messageBuilder(
            ServerPlayerListRequestMessage.class,
            EconomyMessages.SERVER_PLAYER_LIST_REQUEST.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(Forge1201NetworkChannel::encodePlayerListRequest)
        .decoder(Forge1201NetworkChannel::decodePlayerListRequest)
        .consumerMainThread(Forge1201PlayerListHandlers::handleRequest)
        .add();

    CHANNEL
        .messageBuilder(
            ServerPlayerListResponseMessage.class,
            EconomyMessages.SERVER_PLAYER_LIST_RESPONSE.discriminator(),
            NetworkDirection.PLAY_TO_CLIENT)
        .encoder(Forge1201NetworkChannel::encodePlayerListResponse)
        .decoder(Forge1201NetworkChannel::decodePlayerListResponse)
        .consumerMainThread(Forge1201PlayerListHandlers::handleResponse)
        .add();

    CHANNEL
        .messageBuilder(
            ModifyTerritoryModeMessage.class,
            EconomyMessages.MODIFY_MODE.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(TerritoryManagementWireCodec::encodeModifyMode))
        .decoder(targetDecoder(TerritoryManagementWireCodec::decodeModifyMode))
        .consumerMainThread(Forge1201TerritoryManagementHandlers::modifyMode)
        .add();
    CHANNEL
        .messageBuilder(
            UnlockTerritoryBuffMessage.class,
            EconomyMessages.UNLOCK_TERRITORY_BUFF.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(TerritoryManagementWireCodec::encodeUnlockBuff))
        .decoder(targetDecoder(TerritoryManagementWireCodec::decodeUnlockBuff))
        .consumerMainThread(Forge1201TerritoryManagementHandlers::unlockBuff)
        .add();
    CHANNEL
        .messageBuilder(
            UpgradeTerritoryBuffMessage.class,
            EconomyMessages.UPGRADE_TERRITORY_BUFF.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(TerritoryManagementWireCodec::encodeUpgradeBuff))
        .decoder(targetDecoder(TerritoryManagementWireCodec::decodeUpgradeBuff))
        .consumerMainThread(Forge1201TerritoryManagementHandlers::upgradeBuff)
        .add();
    CHANNEL
        .messageBuilder(
            SingleTerritoryDataRequestMessage.class,
            EconomyMessages.SINGLE_TERRITORY_DATA_REQUEST.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(TerritoryManagementWireCodec::encodeSingleRequest))
        .decoder(targetDecoder(TerritoryManagementWireCodec::decodeSingleRequest))
        .consumerMainThread(Forge1201TerritoryManagementHandlers::singleRequest)
        .add();
    CHANNEL
        .messageBuilder(
            SingleTerritoryDataResponseMessage.class,
            EconomyMessages.SINGLE_TERRITORY_DATA_RESPONSE.discriminator(),
            NetworkDirection.PLAY_TO_CLIENT)
        .encoder(targetEncoder(TerritoryManagementWireCodec::encodeSingleResponse))
        .decoder(targetDecoder(TerritoryManagementWireCodec::decodeSingleResponse))
        .consumerMainThread(Forge1201TerritoryManagementHandlers::singleResponse)
        .add();
    CHANNEL
        .messageBuilder(
            UpdateTerritoryPermissionMessage.class,
            EconomyMessages.UPDATE_TERRITORY_PERMISSION.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(TerritoryManagementWireCodec::encodePermission))
        .decoder(targetDecoder(TerritoryManagementWireCodec::decodePermission))
        .consumerMainThread(Forge1201TerritoryManagementHandlers::permission)
        .add();
    CHANNEL
        .messageBuilder(
            TransferTerritoryOwnershipMessage.class,
            EconomyMessages.TRANSFER_TERRITORY_OWNERSHIP.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(TerritoryManagementWireCodec::encodeTransfer))
        .decoder(targetDecoder(TerritoryManagementWireCodec::decodeTransfer))
        .consumerMainThread(Forge1201TerritoryManagementHandlers::transfer)
        .add();
    CHANNEL
        .messageBuilder(
            UpdateTerritoryRuleMessage.class,
            EconomyMessages.UPDATE_TERRITORY_RULE.discriminator(),
            NetworkDirection.PLAY_TO_SERVER)
        .encoder(targetEncoder(TerritoryManagementWireCodec::encodeRule))
        .decoder(targetDecoder(TerritoryManagementWireCodec::decodeRule))
        .consumerMainThread(Forge1201TerritoryManagementHandlers::rule)
        .add();

    registered = true;
  }

  static void sendToServer(BalanceRequestMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(ClientFileCheckResultRequestMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }
  static void sendToServer(CheckedFileTransferControlRequestMessage message){requireRegistered();CHANNEL.sendToServer(message);}
  static void sendToServer(CheckedFileTransferChunkRequestMessage message){requireRegistered();CHANNEL.sendToServer(message);}

  static void sendToServer(BalanceLogRequestMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(TransferMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(ShopDataRequestMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(ShopBuyItemMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(CreateSalesOrderMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(CreateDemandOrderMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(MarketDataRequestMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(PurchaseSalesOrderMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(RemoveSalesOrderMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(ConfirmDemandOrderMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(DeliverDemandOrderMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(RemoveDemandOrderMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(TerritoryDataRequestMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(TeleportToTerritoryMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(InvitePlayerMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(RemoveTerritoryMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(RemoveTerritoryMemberMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(DeliveryBoxDataRequestMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(DeliveryBoxClaimMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(MailboxDataRequestMessage message) { requireRegistered(); CHANNEL.sendToServer(message); }
  static void sendToServer(MailboxMarkReadMessage message) { requireRegistered(); CHANNEL.sendToServer(message); }
  static void sendToServer(MailboxDeleteMessage message) { requireRegistered(); CHANNEL.sendToServer(message); }
  static void sendToServer(MailboxClaimAttachmentMessage message) { requireRegistered(); CHANNEL.sendToServer(message); }
  static void sendToServer(MailboxClaimAllMessage message) { requireRegistered(); CHANNEL.sendToServer(message); }
  static void sendToServer(MailboxSendPlayerMessage message) { requireRegistered(); CHANNEL.sendToServer(message); }

  static void sendToServer(ServerPlayerListRequestMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(ModifyTerritoryModeMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(UnlockTerritoryBuffMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(UpgradeTerritoryBuffMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(SingleTerritoryDataRequestMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(UpdateTerritoryPermissionMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(TransferTerritoryOwnershipMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToServer(UpdateTerritoryRuleMessage message) {
    requireRegistered();
    CHANNEL.sendToServer(message);
  }

  static void sendToPlayer(ServerPlayer player, BalanceResponseMessage message) {
    requireRegistered();
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
  }

  static void sendToPlayer(ServerPlayer player, ClientFileCheckRequestMessage message) {
    requireRegistered();
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
  }

  static void sendToPlayer(ServerPlayer player, ClientFileCheckResultResponseMessage message) {
    requireRegistered();
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
  }
  static void sendToPlayer(ServerPlayer player,CheckedFileTransferRequestMessage message){requireRegistered();CHANNEL.send(PacketDistributor.PLAYER.with(()->player),message);}
  static void sendToPlayer(ServerPlayer player,CheckedFileTransferControlResponseMessage message){requireRegistered();CHANNEL.send(PacketDistributor.PLAYER.with(()->player),message);}
  static void sendToPlayer(ServerPlayer player,CheckedFileTransferChunkResponseMessage message){requireRegistered();CHANNEL.send(PacketDistributor.PLAYER.with(()->player),message);}

  static void sendToPlayer(ServerPlayer player, BalanceLogResponseMessage message) {
    requireRegistered();
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
  }

  static void sendToPlayer(ServerPlayer player, ShopDataResponseMessage message) {
    requireRegistered();
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
  }

  static void sendToPlayer(ServerPlayer player, MarketDataResponseMessage message) {
    requireRegistered();
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
  }

  static void sendToPlayer(ServerPlayer player, ServerPlayerListResponseMessage message) {
    requireRegistered();
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
  }

  static void sendToPlayer(ServerPlayer player, TerritoryDataResponseMessage message) {
    requireRegistered();
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
  }

  static void sendToPlayer(ServerPlayer player, DeliveryBoxDataResponseMessage message) {
    requireRegistered();
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
  }

  static void sendToPlayer(ServerPlayer player, MailboxDataResponseMessage message) {
    requireRegistered();
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
  }

  static void sendToPlayer(ServerPlayer player, MailboxSendResultMessage message) {
    requireRegistered();
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
  }

  static void sendToPlayer(ServerPlayer player, MailboxNotificationMessage message) {
    requireRegistered();
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
  }

  static void sendToPlayer(ServerPlayer player, SingleTerritoryDataResponseMessage message) {
    requireRegistered();
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
  }

  private static void encodeBalanceRequest(BalanceRequestMessage message, FriendlyByteBuf buffer) {
    buffer.writeBoolean(message.includeAccountList());
  }

  private static BalanceRequestMessage decodeBalanceRequest(FriendlyByteBuf buffer) {
    return new BalanceRequestMessage(buffer.readBoolean());
  }

  private static void encodeBalanceResponse(
      BalanceResponseMessage message, FriendlyByteBuf buffer) {
    if (message.accounts().size() > EconomyNetworkLimits.MAX_ACCOUNT_ENTRIES) {
      throw new IllegalArgumentException(
          "Balance response has too many accounts: " + message.accounts().size());
    }

    buffer.writeInt(message.balance());
    buffer.writeInt(message.accounts().size());
    for (AccountBalance account : message.accounts()) {
      buffer.writeUtf(account.playerName(), EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH);
      buffer.writeInt(account.balance());
    }
  }

  private static BalanceResponseMessage decodeBalanceResponse(FriendlyByteBuf buffer) {
    int balance = buffer.readInt();
    int size = buffer.readInt();
    if (size < 0 || size > EconomyNetworkLimits.MAX_ACCOUNT_ENTRIES) {
      throw new DecoderException("Invalid balance account count: " + size);
    }

    List<AccountBalance> accounts = new ArrayList<>(size);
    for (int index = 0; index < size; index++) {
      accounts.add(
          new AccountBalance(
              buffer.readUtf(EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH), buffer.readInt()));
    }
    return new BalanceResponseMessage(balance, accounts);
  }

  private static void encodeBalanceLogRequest(
      BalanceLogRequestMessage message, FriendlyByteBuf buffer) {
    if (!isValidBalanceLogRequestMetadata(message.offset(), message.limit())) {
      throw new IllegalArgumentException(
          "Invalid balance-log request page: offset="
              + message.offset()
              + ", limit="
              + message.limit());
    }
    buffer.writeUtf(message.category(), EconomyNetworkLimits.MAX_BALANCE_LOG_CATEGORY_LENGTH);
    buffer.writeInt(message.offset());
    buffer.writeInt(message.limit());
  }

  private static BalanceLogRequestMessage decodeBalanceLogRequest(FriendlyByteBuf buffer) {
    String category = buffer.readUtf(EconomyNetworkLimits.MAX_BALANCE_LOG_CATEGORY_LENGTH);
    int offset = buffer.readInt();
    int limit = buffer.readInt();
    if (!isValidBalanceLogRequestMetadata(offset, limit)) {
      throw new DecoderException(
          "Invalid balance-log request page: offset=" + offset + ", limit=" + limit);
    }
    return new BalanceLogRequestMessage(category, offset, limit);
  }

  private static void encodeBalanceLogResponse(
      BalanceLogResponseMessage message, FriendlyByteBuf buffer) {
    int size = message.logs().size();
    if (!isValidBalanceLogResponseMetadata(
        message.offset(), message.limit(), message.total(), size)) {
      throw new IllegalArgumentException(
          "Invalid balance-log response page: offset="
              + message.offset()
              + ", limit="
              + message.limit()
              + ", total="
              + message.total()
              + ", size="
              + size);
    }

    buffer.writeUtf(message.category(), EconomyNetworkLimits.MAX_BALANCE_LOG_CATEGORY_LENGTH);
    buffer.writeInt(message.offset());
    buffer.writeInt(message.limit());
    buffer.writeInt(message.total());
    buffer.writeInt(size);
    for (BalanceLogEntry log : message.logs()) {
      buffer.writeLong(log.timeMillis());
      buffer.writeUtf(
          log.category() == null ? "系统" : log.category(),
          EconomyNetworkLimits.MAX_BALANCE_LOG_CATEGORY_LENGTH);
      buffer.writeUtf(
          log.reason() == null ? "" : log.reason(),
          EconomyNetworkLimits.MAX_BALANCE_LOG_REASON_LENGTH);
      buffer.writeInt(log.delta());
      buffer.writeInt(log.beforeBalance());
      buffer.writeInt(log.afterBalance());
    }
  }

  private static BalanceLogResponseMessage decodeBalanceLogResponse(FriendlyByteBuf buffer) {
    String category = buffer.readUtf(EconomyNetworkLimits.MAX_BALANCE_LOG_CATEGORY_LENGTH);
    int offset = buffer.readInt();
    int limit = buffer.readInt();
    int total = buffer.readInt();
    int size = buffer.readInt();
    if (!isValidBalanceLogResponseMetadata(offset, limit, total, size)) {
      throw new DecoderException(
          "Invalid balance-log response page: offset="
              + offset
              + ", limit="
              + limit
              + ", total="
              + total
              + ", size="
              + size);
    }

    List<BalanceLogEntry> logs = new ArrayList<>(size);
    for (int index = 0; index < size; index++) {
      logs.add(
          new BalanceLogEntry(
              buffer.readLong(),
              buffer.readUtf(EconomyNetworkLimits.MAX_BALANCE_LOG_CATEGORY_LENGTH),
              buffer.readUtf(EconomyNetworkLimits.MAX_BALANCE_LOG_REASON_LENGTH),
              buffer.readInt(),
              buffer.readInt(),
              buffer.readInt()));
    }
    return new BalanceLogResponseMessage(category, offset, limit, total, logs);
  }

  private static void encodeTransfer(TransferMessage message, FriendlyByteBuf buffer) {
    buffer.writeUUID(message.targetPlayerId());
    buffer.writeInt(message.amount());
  }

  private static TransferMessage decodeTransfer(FriendlyByteBuf buffer) {
    return new TransferMessage(buffer.readUUID(), buffer.readInt());
  }

  private static void encodeShopDataRequest(
      ShopDataRequestMessage message, FriendlyByteBuf buffer) {
    // Empty payload.
  }

  private static ShopDataRequestMessage decodeShopDataRequest(FriendlyByteBuf buffer) {
    return ShopDataRequestMessage.INSTANCE;
  }

  private static void encodeShopBuyItem(ShopBuyItemMessage message, FriendlyByteBuf buffer) {
    buffer.writeUtf(message.shopItemId(), EconomyNetworkLimits.MAX_SHOP_ITEM_ID_LENGTH);
    buffer.writeInt(message.quantity());
  }

  private static ShopBuyItemMessage decodeShopBuyItem(FriendlyByteBuf buffer) {
    return new ShopBuyItemMessage(
        buffer.readUtf(EconomyNetworkLimits.MAX_SHOP_ITEM_ID_LENGTH), buffer.readInt());
  }

  private static void encodeShopDataResponse(
      ShopDataResponseMessage message, FriendlyByteBuf buffer) {
    if (message.items().size() > EconomyNetworkLimits.MAX_SHOP_ENTRIES) {
      throw new IllegalArgumentException(
          "Shop response has too many entries: " + message.items().size());
    }
    buffer.writeInt(message.items().size());
    for (ShopItemSnapshot item : message.items()) {
      encodeShopItem(item, buffer);
    }
  }

  private static ShopDataResponseMessage decodeShopDataResponse(FriendlyByteBuf buffer) {
    int size = buffer.readInt();
    if (size < 0 || size > EconomyNetworkLimits.MAX_SHOP_ENTRIES) {
      throw new DecoderException("Invalid shop entry count: " + size);
    }
    List<ShopItemSnapshot> items = new ArrayList<>(size);
    for (int index = 0; index < size; index++) {
      items.add(decodeShopItem(buffer));
    }
    return new ShopDataResponseMessage(items);
  }

  private static void encodeShopItem(ShopItemSnapshot item, FriendlyByteBuf buffer) {
    buffer.writeUtf(item.shopItemId(), EconomyNetworkLimits.MAX_SHOP_ITEM_ID_LENGTH);
    buffer.writeUtf(item.itemId(), EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH);
    buffer.writeInt(item.basePrice());
    buffer.writeInt(item.currentPrice());
    buffer.writeInt(item.lastPrice());
    buffer.writeUtf(item.description(), EconomyNetworkLimits.MAX_SHOP_DESCRIPTION_LENGTH);
    buffer.writeDouble(item.fluctuationFactor());
    buffer.writeUtf(item.nbt(), EconomyNetworkLimits.MAX_ITEM_DATA_LENGTH);
    buffer.writeUtf(item.itemData(), EconomyNetworkLimits.MAX_ITEM_DATA_LENGTH);
    buffer.writeInt(item.recentDemand());
    buffer.writeInt(item.virtualStock());
    buffer.writeInt(item.maxVirtualStock());
  }

  private static ShopItemSnapshot decodeShopItem(FriendlyByteBuf buffer) {
    return new ShopItemSnapshot(
        buffer.readUtf(EconomyNetworkLimits.MAX_SHOP_ITEM_ID_LENGTH),
        buffer.readUtf(EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readUtf(EconomyNetworkLimits.MAX_SHOP_DESCRIPTION_LENGTH),
        buffer.readDouble(),
        buffer.readUtf(EconomyNetworkLimits.MAX_ITEM_DATA_LENGTH),
        buffer.readUtf(EconomyNetworkLimits.MAX_ITEM_DATA_LENGTH),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readInt());
  }

  private static void encodePlayerListRequest(
      ServerPlayerListRequestMessage message, FriendlyByteBuf buffer) {
    // Empty payload.
  }

  private static ServerPlayerListRequestMessage decodePlayerListRequest(FriendlyByteBuf buffer) {
    return ServerPlayerListRequestMessage.INSTANCE;
  }

  private static void encodePlayerListResponse(
      ServerPlayerListResponseMessage message, FriendlyByteBuf buffer) {
    if (message.players().size() > EconomyNetworkLimits.MAX_PLAYER_LIST_ENTRIES) {
      throw new IllegalArgumentException(
          "Player list response has too many entries: " + message.players().size());
    }

    buffer.writeInt(message.players().size());
    for (PlayerSummary player : message.players()) {
      buffer.writeUUID(player.playerId());
      buffer.writeUtf(player.playerName(), EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH);
    }
  }

  private static ServerPlayerListResponseMessage decodePlayerListResponse(FriendlyByteBuf buffer) {
    int size = buffer.readInt();
    if (size < 0 || size > EconomyNetworkLimits.MAX_PLAYER_LIST_ENTRIES) {
      throw new DecoderException("Invalid player list entry count: " + size);
    }

    List<PlayerSummary> players = new ArrayList<>(size);
    for (int index = 0; index < size; index++) {
      players.add(
          new PlayerSummary(
              buffer.readUUID(), buffer.readUtf(EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH)));
    }
    return new ServerPlayerListResponseMessage(players);
  }

  private static boolean isValidBalanceLogRequestMetadata(int offset, int limit) {
    return offset >= 0 && limit >= 1 && limit <= EconomyNetworkLimits.MAX_BALANCE_LOG_ENTRIES;
  }

  private static boolean isValidBalanceLogResponseMetadata(
      int offset, int limit, int total, int size) {
    if (!isValidBalanceLogRequestMetadata(offset, limit)
        || total < 0
        || size < 0
        || size > EconomyNetworkLimits.MAX_BALANCE_LOG_ENTRIES
        || size > limit
        || size > total) {
      return false;
    }
    return size == 0 || (long) offset + size <= total;
  }

  private static <T> BiConsumer<T, FriendlyByteBuf> targetEncoder(
      BiConsumer<T, WireBuffer> encoder) {
    return (message, buffer) -> encoder.accept(message, Forge1201WireBuffer.wrap(buffer));
  }

  private static <T> Function<FriendlyByteBuf, T> targetDecoder(
      Function<WireBuffer, T> decoder) {
    return buffer -> decoder.apply(Forge1201WireBuffer.wrap(buffer));
  }

  private static void requireRegistered() {
    if (!registered) {
      throw new IllegalStateException("Forge 1.20.1 network channel is not registered");
    }
  }
}
