package com.mo.economy_system.target.neoforge1211.protocol;

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
import com.mo.economy_system.common.network.MailboxClaimAllMessage;
import com.mo.economy_system.common.network.MailboxClaimAttachmentMessage;
import com.mo.economy_system.common.network.MailboxDataRequestMessage;
import com.mo.economy_system.common.network.MailboxDataResponseMessage;
import com.mo.economy_system.common.network.MailboxDeleteMessage;
import com.mo.economy_system.common.network.MailboxMarkReadMessage;
import com.mo.economy_system.common.network.MailboxSendPlayerMessage;
import com.mo.economy_system.common.network.MailboxSendResultMessage;
import com.mo.economy_system.common.network.MailboxNotificationMessage;
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
import com.mo.economy_system.network.TerritoryDataWireCodec;
import com.mo.economy_system.network.TerritoryInviteWireCodec;
import com.mo.economy_system.network.TerritoryManagementWireCodec;
import com.mo.economy_system.network.TerritoryMemberRemovalWireCodec;
import com.mo.economy_system.network.TerritoryRemovalWireCodec;
import com.mo.economy_system.network.TerritoryTeleportWireCodec;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.platform.network.WireBuffer;
import com.mo.economy_system.protocol.EconomyMessageType;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Codecs for common messages migrated to the NeoForge 1.21.1 target. */
public final class NeoForge1211MessageCodecs {
  private static final Map<EconomyMessageType<?>, NeoForge1211MessageCodec<?>> CODECS =
      createCodecs();

  private NeoForge1211MessageCodecs() {}

  public static boolean supports(EconomyMessageType<?> messageType) {
    return CODECS.containsKey(messageType);
  }

  @SuppressWarnings("unchecked")
  public static <T extends EconomyNetworkMessage> NeoForge1211MessageCodec<T> codec(
      EconomyMessageType<T> messageType) {
    NeoForge1211MessageCodec<?> codec = CODECS.get(messageType);
    if (codec == null) {
      throw new IllegalArgumentException(
          "No NeoForge 1.21.1 common-message codec for " + messageType.id());
    }
    return (NeoForge1211MessageCodec<T>) codec;
  }

  private static Map<EconomyMessageType<?>, NeoForge1211MessageCodec<?>> createCodecs() {
    Map<EconomyMessageType<?>, NeoForge1211MessageCodec<?>> codecs = new HashMap<>();
    register(
        codecs,
        EconomyMessages.CHECK,
        new NeoForge1211MessageCodec<>() {
          public void encode(
              ClientFileCheckRequestMessage message, RegistryFriendlyByteBuf buffer) {
            ClientFileCheckWireCodec.encodeRequest(message, wire(buffer));
          }

          public ClientFileCheckRequestMessage decode(RegistryFriendlyByteBuf buffer) {
            return ClientFileCheckWireCodec.decodeRequest(wire(buffer));
          }
        });
    register(
        codecs,
        EconomyMessages.CHECK_RESULT_REQUEST,
        new NeoForge1211MessageCodec<>() {
          public void encode(
              ClientFileCheckResultRequestMessage message, RegistryFriendlyByteBuf buffer) {
            ClientFileCheckWireCodec.encodeResultRequest(message, wire(buffer));
          }

          public ClientFileCheckResultRequestMessage decode(RegistryFriendlyByteBuf buffer) {
            return ClientFileCheckWireCodec.decodeResultRequest(wire(buffer));
          }
        });
    register(
        codecs,
        EconomyMessages.CHECK_RESULT_RESPONSE,
        new NeoForge1211MessageCodec<>() {
          public void encode(
              ClientFileCheckResultResponseMessage message, RegistryFriendlyByteBuf buffer) {
            ClientFileCheckWireCodec.encodeResultResponse(message, wire(buffer));
          }

          public ClientFileCheckResultResponseMessage decode(RegistryFriendlyByteBuf buffer) {
            return ClientFileCheckWireCodec.decodeResultResponse(wire(buffer));
          }
        });
    register(codecs, EconomyMessages.GET, new NeoForge1211MessageCodec<>() {
      public void encode(CheckedFileTransferRequestMessage m, RegistryFriendlyByteBuf b) { CheckedFileTransferWireCodec.encodeRequest(m, wire(b)); }
      public CheckedFileTransferRequestMessage decode(RegistryFriendlyByteBuf b) { return CheckedFileTransferWireCodec.decodeRequest(wire(b)); }
    });
    register(codecs, EconomyMessages.GET_RESULT_REQUEST, new NeoForge1211MessageCodec<>() {
      public void encode(CheckedFileTransferControlRequestMessage m, RegistryFriendlyByteBuf b) { CheckedFileTransferWireCodec.encodeControlRequest(m, wire(b)); }
      public CheckedFileTransferControlRequestMessage decode(RegistryFriendlyByteBuf b) { return CheckedFileTransferWireCodec.decodeControlRequest(wire(b)); }
    });
    register(codecs, EconomyMessages.GET_RESULT_RESPONSE, new NeoForge1211MessageCodec<>() {
      public void encode(CheckedFileTransferControlResponseMessage m, RegistryFriendlyByteBuf b) { CheckedFileTransferWireCodec.encodeControlResponse(m, wire(b)); }
      public CheckedFileTransferControlResponseMessage decode(RegistryFriendlyByteBuf b) { return CheckedFileTransferWireCodec.decodeControlResponse(wire(b)); }
    });
    register(codecs, EconomyMessages.CHUNK, new NeoForge1211MessageCodec<>() {
      public void encode(CheckedFileTransferChunkRequestMessage m, RegistryFriendlyByteBuf b) { CheckedFileTransferWireCodec.encodeChunkRequest(m, wire(b)); }
      public CheckedFileTransferChunkRequestMessage decode(RegistryFriendlyByteBuf b) { return CheckedFileTransferWireCodec.decodeChunkRequest(wire(b)); }
    });
    register(codecs, EconomyMessages.CHUNK_RESPONSE, new NeoForge1211MessageCodec<>() {
      public void encode(CheckedFileTransferChunkResponseMessage m, RegistryFriendlyByteBuf b) { CheckedFileTransferWireCodec.encodeChunkResponse(m, wire(b)); }
      public CheckedFileTransferChunkResponseMessage decode(RegistryFriendlyByteBuf b) { return CheckedFileTransferWireCodec.decodeChunkResponse(wire(b)); }
    });
    register(codecs, EconomyMessages.DELIVERY_BOX_DATA_REQUEST, new NeoForge1211MessageCodec<>() {
      public void encode(DeliveryBoxDataRequestMessage message, RegistryFriendlyByteBuf buffer) {
        DeliveryBoxWireCodec.encodeRequest(message, wire(buffer));
      }
      public DeliveryBoxDataRequestMessage decode(RegistryFriendlyByteBuf buffer) {
        return DeliveryBoxWireCodec.decodeRequest(wire(buffer));
      }
    });
    register(codecs, EconomyMessages.DELIVERY_BOX_DATA_RESPONSE, new NeoForge1211MessageCodec<>() {
      public void encode(DeliveryBoxDataResponseMessage message, RegistryFriendlyByteBuf buffer) {
        DeliveryBoxWireCodec.encodeResponse(message, wire(buffer));
      }
      public DeliveryBoxDataResponseMessage decode(RegistryFriendlyByteBuf buffer) {
        return DeliveryBoxWireCodec.decodeResponse(wire(buffer));
      }
    });
    register(codecs, EconomyMessages.DELIVERY_BOX_CLAIM_ITEM, new NeoForge1211MessageCodec<>() {
      public void encode(DeliveryBoxClaimMessage message, RegistryFriendlyByteBuf buffer) {
        DeliveryBoxWireCodec.encodeClaim(message, wire(buffer));
      }
      public DeliveryBoxClaimMessage decode(RegistryFriendlyByteBuf buffer) {
        return DeliveryBoxWireCodec.decodeClaim(wire(buffer));
      }
    });
    register(codecs, EconomyMessages.MAILBOX_DATA_REQUEST, new NeoForge1211MessageCodec<>() {
      public void encode(MailboxDataRequestMessage message, RegistryFriendlyByteBuf buffer) { MailboxWireCodec.encodeRequest(message, wire(buffer)); }
      public MailboxDataRequestMessage decode(RegistryFriendlyByteBuf buffer) { return MailboxWireCodec.decodeRequest(wire(buffer)); }
    });
    register(codecs, EconomyMessages.MAILBOX_DATA_RESPONSE, new NeoForge1211MessageCodec<>() {
      public void encode(MailboxDataResponseMessage message, RegistryFriendlyByteBuf buffer) { MailboxWireCodec.encodeResponse(message, wire(buffer)); }
      public MailboxDataResponseMessage decode(RegistryFriendlyByteBuf buffer) { return MailboxWireCodec.decodeResponse(wire(buffer)); }
    });
    register(codecs, EconomyMessages.MAILBOX_MARK_READ, new NeoForge1211MessageCodec<>() {
      public void encode(MailboxMarkReadMessage message, RegistryFriendlyByteBuf buffer) { MailboxWireCodec.encodeMarkRead(message, wire(buffer)); }
      public MailboxMarkReadMessage decode(RegistryFriendlyByteBuf buffer) { return MailboxWireCodec.decodeMarkRead(wire(buffer)); }
    });
    register(codecs, EconomyMessages.MAILBOX_DELETE, new NeoForge1211MessageCodec<>() {
      public void encode(MailboxDeleteMessage message, RegistryFriendlyByteBuf buffer) { MailboxWireCodec.encodeDelete(message, wire(buffer)); }
      public MailboxDeleteMessage decode(RegistryFriendlyByteBuf buffer) { return MailboxWireCodec.decodeDelete(wire(buffer)); }
    });
    register(codecs, EconomyMessages.MAILBOX_CLAIM_ATTACHMENT, new NeoForge1211MessageCodec<>() {
      public void encode(MailboxClaimAttachmentMessage message, RegistryFriendlyByteBuf buffer) { MailboxWireCodec.encodeClaimAttachment(message, wire(buffer)); }
      public MailboxClaimAttachmentMessage decode(RegistryFriendlyByteBuf buffer) { return MailboxWireCodec.decodeClaimAttachment(wire(buffer)); }
    });
    register(codecs, EconomyMessages.MAILBOX_CLAIM_ALL, new NeoForge1211MessageCodec<>() {
      public void encode(MailboxClaimAllMessage message, RegistryFriendlyByteBuf buffer) { MailboxWireCodec.encodeClaimAll(message, wire(buffer)); }
      public MailboxClaimAllMessage decode(RegistryFriendlyByteBuf buffer) { return MailboxWireCodec.decodeClaimAll(wire(buffer)); }
    });
    register(codecs, EconomyMessages.MAILBOX_SEND_PLAYER, new NeoForge1211MessageCodec<>() {
      public void encode(MailboxSendPlayerMessage message, RegistryFriendlyByteBuf buffer) { MailboxWireCodec.encodeSendPlayer(message, wire(buffer)); }
      public MailboxSendPlayerMessage decode(RegistryFriendlyByteBuf buffer) { return MailboxWireCodec.decodeSendPlayer(wire(buffer)); }
    });
    register(codecs, EconomyMessages.MAILBOX_SEND_RESULT, new NeoForge1211MessageCodec<>() {
      public void encode(MailboxSendResultMessage message, RegistryFriendlyByteBuf buffer) { MailboxWireCodec.encodeSendResult(message, wire(buffer)); }
      public MailboxSendResultMessage decode(RegistryFriendlyByteBuf buffer) { return MailboxWireCodec.decodeSendResult(wire(buffer)); }
    });
    register(codecs, EconomyMessages.MAILBOX_NOTIFICATION, new NeoForge1211MessageCodec<>() {
      public void encode(MailboxNotificationMessage message, RegistryFriendlyByteBuf buffer) { MailboxWireCodec.encodeNotification(message, wire(buffer)); }
      public MailboxNotificationMessage decode(RegistryFriendlyByteBuf buffer) { return MailboxWireCodec.decodeNotification(wire(buffer)); }
    });
    register(codecs, EconomyMessages.MODIFY_MODE, new NeoForge1211MessageCodec<>() {
      public void encode(ModifyTerritoryModeMessage message, RegistryFriendlyByteBuf buffer) {
        TerritoryManagementWireCodec.encodeModifyMode(message, wire(buffer));
      }
      public ModifyTerritoryModeMessage decode(RegistryFriendlyByteBuf buffer) {
        return TerritoryManagementWireCodec.decodeModifyMode(wire(buffer));
      }
    });
    register(codecs, EconomyMessages.UNLOCK_TERRITORY_BUFF, new NeoForge1211MessageCodec<>() {
      public void encode(UnlockTerritoryBuffMessage message, RegistryFriendlyByteBuf buffer) {
        TerritoryManagementWireCodec.encodeUnlockBuff(message, wire(buffer));
      }
      public UnlockTerritoryBuffMessage decode(RegistryFriendlyByteBuf buffer) {
        return TerritoryManagementWireCodec.decodeUnlockBuff(wire(buffer));
      }
    });
    register(codecs, EconomyMessages.UPGRADE_TERRITORY_BUFF, new NeoForge1211MessageCodec<>() {
      public void encode(UpgradeTerritoryBuffMessage message, RegistryFriendlyByteBuf buffer) {
        TerritoryManagementWireCodec.encodeUpgradeBuff(message, wire(buffer));
      }
      public UpgradeTerritoryBuffMessage decode(RegistryFriendlyByteBuf buffer) {
        return TerritoryManagementWireCodec.decodeUpgradeBuff(wire(buffer));
      }
    });
    register(codecs, EconomyMessages.SINGLE_TERRITORY_DATA_REQUEST, new NeoForge1211MessageCodec<>() {
      public void encode(SingleTerritoryDataRequestMessage message, RegistryFriendlyByteBuf buffer) {
        TerritoryManagementWireCodec.encodeSingleRequest(message, wire(buffer));
      }
      public SingleTerritoryDataRequestMessage decode(RegistryFriendlyByteBuf buffer) {
        return TerritoryManagementWireCodec.decodeSingleRequest(wire(buffer));
      }
    });
    register(codecs, EconomyMessages.SINGLE_TERRITORY_DATA_RESPONSE, new NeoForge1211MessageCodec<>() {
      public void encode(SingleTerritoryDataResponseMessage message, RegistryFriendlyByteBuf buffer) {
        TerritoryManagementWireCodec.encodeSingleResponse(message, wire(buffer));
      }
      public SingleTerritoryDataResponseMessage decode(RegistryFriendlyByteBuf buffer) {
        return TerritoryManagementWireCodec.decodeSingleResponse(wire(buffer));
      }
    });
    register(codecs, EconomyMessages.UPDATE_TERRITORY_PERMISSION, new NeoForge1211MessageCodec<>() {
      public void encode(UpdateTerritoryPermissionMessage message, RegistryFriendlyByteBuf buffer) {
        TerritoryManagementWireCodec.encodePermission(message, wire(buffer));
      }
      public UpdateTerritoryPermissionMessage decode(RegistryFriendlyByteBuf buffer) {
        return TerritoryManagementWireCodec.decodePermission(wire(buffer));
      }
    });
    register(codecs, EconomyMessages.TRANSFER_TERRITORY_OWNERSHIP, new NeoForge1211MessageCodec<>() {
      public void encode(TransferTerritoryOwnershipMessage message, RegistryFriendlyByteBuf buffer) {
        TerritoryManagementWireCodec.encodeTransfer(message, wire(buffer));
      }
      public TransferTerritoryOwnershipMessage decode(RegistryFriendlyByteBuf buffer) {
        return TerritoryManagementWireCodec.decodeTransfer(wire(buffer));
      }
    });
    register(codecs, EconomyMessages.UPDATE_TERRITORY_RULE, new NeoForge1211MessageCodec<>() {
      public void encode(UpdateTerritoryRuleMessage message, RegistryFriendlyByteBuf buffer) {
        TerritoryManagementWireCodec.encodeRule(message, wire(buffer));
      }
      public UpdateTerritoryRuleMessage decode(RegistryFriendlyByteBuf buffer) {
        return TerritoryManagementWireCodec.decodeRule(wire(buffer));
      }
    });
    register(
        codecs,
        EconomyMessages.INVITE_PLAYER,
        new NeoForge1211MessageCodec<>() {
          public void encode(InvitePlayerMessage message, RegistryFriendlyByteBuf buffer) {
            TerritoryInviteWireCodec.encode(message, wire(buffer));
          }

          public InvitePlayerMessage decode(RegistryFriendlyByteBuf buffer) {
            return TerritoryInviteWireCodec.decode(wire(buffer));
          }
        });
    register(
        codecs,
        EconomyMessages.REMOVE_TERRITORY,
        new NeoForge1211MessageCodec<>() {
          public void encode(RemoveTerritoryMessage message, RegistryFriendlyByteBuf buffer) {
            TerritoryRemovalWireCodec.encode(message, wire(buffer));
          }

          public RemoveTerritoryMessage decode(RegistryFriendlyByteBuf buffer) {
            return TerritoryRemovalWireCodec.decode(wire(buffer));
          }
        });
    register(
        codecs,
        EconomyMessages.REMOVE_PLAYER,
        new NeoForge1211MessageCodec<>() {
          public void encode(RemoveTerritoryMemberMessage message, RegistryFriendlyByteBuf buffer) {
            TerritoryMemberRemovalWireCodec.encode(message, wire(buffer));
          }

          public RemoveTerritoryMemberMessage decode(RegistryFriendlyByteBuf buffer) {
            return TerritoryMemberRemovalWireCodec.decode(wire(buffer));
          }
        });
    register(
        codecs,
        EconomyMessages.BALANCE_REQUEST,
        new NeoForge1211MessageCodec<>() {
          @Override
          public void encode(BalanceRequestMessage message, RegistryFriendlyByteBuf buffer) {
            buffer.writeBoolean(message.includeAccountList());
          }

          @Override
          public BalanceRequestMessage decode(RegistryFriendlyByteBuf buffer) {
            return new BalanceRequestMessage(buffer.readBoolean());
          }
        });
    register(
        codecs,
        EconomyMessages.BALANCE_RESPONSE,
        new NeoForge1211MessageCodec<>() {
          @Override
          public void encode(BalanceResponseMessage message, RegistryFriendlyByteBuf buffer) {
            if (message.accounts().size() > EconomyNetworkLimits.MAX_ACCOUNT_ENTRIES) {
              throw new IllegalArgumentException(
                  "Balance response has too many accounts: " + message.accounts().size());
            }
            // Preserve the NeoForge 1.21.1 wire order exactly.
            buffer.writeInt(message.balance());
            buffer.writeInt(message.accounts().size());
            for (AccountBalance account : message.accounts()) {
              buffer.writeUtf(account.playerName(), EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH);
              buffer.writeInt(account.balance());
            }
          }

          @Override
          public BalanceResponseMessage decode(RegistryFriendlyByteBuf buffer) {
            int balance = buffer.readInt();
            int size = buffer.readInt();
            if (size < 0 || size > EconomyNetworkLimits.MAX_ACCOUNT_ENTRIES) {
              throw new DecoderException("Invalid balance account count: " + size);
            }
            List<AccountBalance> accounts = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
              accounts.add(
                  new AccountBalance(
                      buffer.readUtf(EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH),
                      buffer.readInt()));
            }
            return new BalanceResponseMessage(balance, accounts);
          }
        });
    register(
        codecs,
        EconomyMessages.BALANCE_LOG_REQUEST,
        new NeoForge1211MessageCodec<>() {
          @Override
          public void encode(BalanceLogRequestMessage message, RegistryFriendlyByteBuf buffer) {
            if (!isValidBalanceLogRequestMetadata(message.offset(), message.limit())) {
              throw new IllegalArgumentException(
                  "Invalid balance-log request page: offset="
                      + message.offset()
                      + ", limit="
                      + message.limit());
            }
            buffer.writeUtf(
                message.category(), EconomyNetworkLimits.MAX_BALANCE_LOG_CATEGORY_LENGTH);
            buffer.writeInt(message.offset());
            buffer.writeInt(message.limit());
          }

          @Override
          public BalanceLogRequestMessage decode(RegistryFriendlyByteBuf buffer) {
            String category = buffer.readUtf(EconomyNetworkLimits.MAX_BALANCE_LOG_CATEGORY_LENGTH);
            int offset = buffer.readInt();
            int limit = buffer.readInt();
            if (!isValidBalanceLogRequestMetadata(offset, limit)) {
              throw new DecoderException(
                  "Invalid balance-log request page: offset=" + offset + ", limit=" + limit);
            }
            return new BalanceLogRequestMessage(category, offset, limit);
          }
        });
    register(
        codecs,
        EconomyMessages.BALANCE_LOG_RESPONSE,
        new NeoForge1211MessageCodec<>() {
          @Override
          public void encode(BalanceLogResponseMessage message, RegistryFriendlyByteBuf buffer) {
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

            buffer.writeUtf(
                message.category(), EconomyNetworkLimits.MAX_BALANCE_LOG_CATEGORY_LENGTH);
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

          @Override
          public BalanceLogResponseMessage decode(RegistryFriendlyByteBuf buffer) {
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
        });
    register(
        codecs,
        EconomyMessages.TRANSFER,
        new NeoForge1211MessageCodec<>() {
          @Override
          public void encode(TransferMessage message, RegistryFriendlyByteBuf buffer) {
            buffer.writeUUID(message.targetPlayerId());
            buffer.writeInt(message.amount());
          }

          @Override
          public TransferMessage decode(RegistryFriendlyByteBuf buffer) {
            return new TransferMessage(buffer.readUUID(), buffer.readInt());
          }
        });
    register(
        codecs,
        EconomyMessages.SHOP_DATA_REQUEST,
        new NeoForge1211MessageCodec<>() {
          @Override
          public void encode(ShopDataRequestMessage message, RegistryFriendlyByteBuf buffer) {
            // Empty payload.
          }

          @Override
          public ShopDataRequestMessage decode(RegistryFriendlyByteBuf buffer) {
            return ShopDataRequestMessage.INSTANCE;
          }
        });
    register(
        codecs,
        EconomyMessages.SHOP_DATA_RESPONSE,
        new NeoForge1211MessageCodec<>() {
          @Override
          public void encode(ShopDataResponseMessage message, RegistryFriendlyByteBuf buffer) {
            if (message.items().size() > EconomyNetworkLimits.MAX_SHOP_ENTRIES) {
              throw new IllegalArgumentException(
                  "Shop response has too many entries: " + message.items().size());
            }
            buffer.writeInt(message.items().size());
            for (ShopItemSnapshot item : message.items()) {
              encodeShopItem(item, buffer);
            }
          }

          @Override
          public ShopDataResponseMessage decode(RegistryFriendlyByteBuf buffer) {
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
        });
    register(
        codecs,
        EconomyMessages.SHOP_BUY_ITEM,
        new NeoForge1211MessageCodec<>() {
          @Override
          public void encode(ShopBuyItemMessage message, RegistryFriendlyByteBuf buffer) {
            buffer.writeUtf(message.shopItemId(), EconomyNetworkLimits.MAX_SHOP_ITEM_ID_LENGTH);
            buffer.writeInt(message.quantity());
          }

          @Override
          public ShopBuyItemMessage decode(RegistryFriendlyByteBuf buffer) {
            return new ShopBuyItemMessage(
                buffer.readUtf(EconomyNetworkLimits.MAX_SHOP_ITEM_ID_LENGTH), buffer.readInt());
          }
        });
    register(
        codecs,
        EconomyMessages.CREATE_SALES_ORDER,
        new NeoForge1211MessageCodec<>() {
          @Override
          public void encode(CreateSalesOrderMessage message, RegistryFriendlyByteBuf buffer) {
            buffer.writeInt(message.slot());
            buffer.writeInt(message.quantity());
            buffer.writeInt(message.unitPrice());
          }

          @Override
          public CreateSalesOrderMessage decode(RegistryFriendlyByteBuf buffer) {
            int slot = buffer.readInt();
            int quantity = buffer.readInt();
            int unitPrice = buffer.readInt();
            if (slot < 0 || quantity <= 0 || unitPrice <= 0)
              throw new DecoderException("Invalid create sales order request");
            return new CreateSalesOrderMessage(slot, quantity, unitPrice);
          }
        });
    register(
        codecs,
        EconomyMessages.CREATE_DEMAND_ORDER,
        new NeoForge1211MessageCodec<>() {
          @Override
          public void encode(CreateDemandOrderMessage message, RegistryFriendlyByteBuf buffer) {
            validateDemand(message.itemId(), message.quantity(), message.unitPrice());
            buffer.writeUtf(message.itemId(), EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH);
            buffer.writeInt(message.quantity());
            buffer.writeInt(message.unitPrice());
          }

          @Override
          public CreateDemandOrderMessage decode(RegistryFriendlyByteBuf buffer) {
            String itemId = buffer.readUtf(EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH);
            int quantity = buffer.readInt();
            int unitPrice = buffer.readInt();
            validateDemand(itemId, quantity, unitPrice);
            return new CreateDemandOrderMessage(itemId, quantity, unitPrice);
          }
        });
    register(codecs, EconomyMessages.MARKET_DATA_REQUEST, NeoForge1211MarketDataCodec.REQUEST);
    register(codecs, EconomyMessages.MARKET_DATA_RESPONSE, NeoForge1211MarketDataCodec.RESPONSE);
    register(
        codecs,
        EconomyMessages.PURCHASE_SALES_ORDER,
        new NeoForge1211MessageCodec<>() {
          @Override
          public void encode(PurchaseSalesOrderMessage message, RegistryFriendlyByteBuf buffer) {
            buffer.writeUUID(message.tradeId());
            buffer.writeVarInt(message.quantity());
          }

          @Override
          public PurchaseSalesOrderMessage decode(RegistryFriendlyByteBuf buffer) {
            return new PurchaseSalesOrderMessage(buffer.readUUID(), buffer.readVarInt());
          }
        });
    register(
        codecs,
        EconomyMessages.REMOVE_SALES_ORDER,
        new NeoForge1211MessageCodec<>() {
          @Override
          public void encode(RemoveSalesOrderMessage message, RegistryFriendlyByteBuf buffer) {
            buffer.writeUUID(message.tradeId());
          }

          @Override
          public RemoveSalesOrderMessage decode(RegistryFriendlyByteBuf buffer) {
            return new RemoveSalesOrderMessage(buffer.readUUID());
          }
        });
    register(
        codecs,
        EconomyMessages.CONFIRM_DEMAND_ORDER,
        new NeoForge1211MessageCodec<>() {
          @Override
          public void encode(ConfirmDemandOrderMessage message, RegistryFriendlyByteBuf buffer) {
            buffer.writeUUID(message.tradeId());
          }

          @Override
          public ConfirmDemandOrderMessage decode(RegistryFriendlyByteBuf buffer) {
            return new ConfirmDemandOrderMessage(buffer.readUUID());
          }
        });
    register(
        codecs,
        EconomyMessages.DELIVER_DEMAND_ORDER,
        new NeoForge1211MessageCodec<>() {
          @Override
          public void encode(DeliverDemandOrderMessage message, RegistryFriendlyByteBuf buffer) {
            buffer.writeUUID(message.tradeId());
            buffer.writeVarInt(message.quantity());
          }

          @Override
          public DeliverDemandOrderMessage decode(RegistryFriendlyByteBuf buffer) {
            return new DeliverDemandOrderMessage(buffer.readUUID(), buffer.readVarInt());
          }
        });
    register(
        codecs,
        EconomyMessages.REMOVE_DEMAND_ORDER,
        new NeoForge1211MessageCodec<>() {
          public void encode(RemoveDemandOrderMessage message, RegistryFriendlyByteBuf buffer) {
            buffer.writeUUID(message.tradeId());
          }

          public RemoveDemandOrderMessage decode(RegistryFriendlyByteBuf buffer) {
            return new RemoveDemandOrderMessage(buffer.readUUID());
          }
        });
    register(
        codecs,
        EconomyMessages.TERRITORY_DATA_REQUEST,
        new NeoForge1211MessageCodec<>() {
          public void encode(TerritoryDataRequestMessage message, RegistryFriendlyByteBuf buffer) {
            TerritoryDataWireCodec.encodeRequest(message, wire(buffer));
          }

          public TerritoryDataRequestMessage decode(RegistryFriendlyByteBuf buffer) {
            return TerritoryDataWireCodec.decodeRequest(wire(buffer));
          }
        });
    register(
        codecs,
        EconomyMessages.TERRITORY_DATA_RESPONSE,
        new NeoForge1211MessageCodec<>() {
          public void encode(TerritoryDataResponseMessage message, RegistryFriendlyByteBuf buffer) {
            TerritoryDataWireCodec.encodeResponse(message, wire(buffer));
          }

          public TerritoryDataResponseMessage decode(RegistryFriendlyByteBuf buffer) {
            return TerritoryDataWireCodec.decodeResponse(wire(buffer));
          }
        });
    register(
        codecs,
        EconomyMessages.TELEPORT_TO_TERRITORY,
        new NeoForge1211MessageCodec<>() {
          public void encode(TeleportToTerritoryMessage message, RegistryFriendlyByteBuf buffer) {
            TerritoryTeleportWireCodec.encode(message, wire(buffer));
          }

          public TeleportToTerritoryMessage decode(RegistryFriendlyByteBuf buffer) {
            return TerritoryTeleportWireCodec.decode(wire(buffer));
          }
        });
    register(
        codecs,
        EconomyMessages.SERVER_PLAYER_LIST_REQUEST,
        new NeoForge1211MessageCodec<>() {
          @Override
          public void encode(
              ServerPlayerListRequestMessage message, RegistryFriendlyByteBuf buffer) {
            // Empty payload.
          }

          @Override
          public ServerPlayerListRequestMessage decode(RegistryFriendlyByteBuf buffer) {
            return ServerPlayerListRequestMessage.INSTANCE;
          }
        });
    register(
        codecs,
        EconomyMessages.SERVER_PLAYER_LIST_RESPONSE,
        new NeoForge1211MessageCodec<>() {
          @Override
          public void encode(
              ServerPlayerListResponseMessage message, RegistryFriendlyByteBuf buffer) {
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

          @Override
          public ServerPlayerListResponseMessage decode(RegistryFriendlyByteBuf buffer) {
            int size = buffer.readInt();
            if (size < 0 || size > EconomyNetworkLimits.MAX_PLAYER_LIST_ENTRIES) {
              throw new DecoderException("Invalid player list size: " + size);
            }
            List<PlayerSummary> players = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
              players.add(
                  new PlayerSummary(
                      buffer.readUUID(),
                      buffer.readUtf(EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH)));
            }
            return new ServerPlayerListResponseMessage(players);
          }
        });
    return Map.copyOf(codecs);
  }

  private static void encodeShopItem(ShopItemSnapshot item, RegistryFriendlyByteBuf buffer) {
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

  private static ShopItemSnapshot decodeShopItem(RegistryFriendlyByteBuf buffer) {
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

  private static boolean isValidBalanceLogRequestMetadata(int offset, int limit) {
    return offset >= 0 && limit >= 1 && limit <= EconomyNetworkLimits.MAX_BALANCE_LOG_ENTRIES;
  }

  private static void validateDemand(String itemId, int quantity, int totalPrice) {
    if (itemId == null
        || itemId.isBlank()
        || itemId.length() > EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH
        || quantity <= 0
        || totalPrice <= 0) throw new DecoderException("Invalid create demand order request");
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

  private static WireBuffer wire(RegistryFriendlyByteBuf buffer) {
    return NeoForge1211WireBuffer.wrap(buffer);
  }

  private static <T extends EconomyNetworkMessage> void register(
      Map<EconomyMessageType<?>, NeoForge1211MessageCodec<?>> codecs,
      EconomyMessageType<T> messageType,
      NeoForge1211MessageCodec<T> codec) {
    if (codecs.put(messageType, codec) != null) {
      throw new IllegalStateException("Duplicate NeoForge codec for " + messageType.id());
    }
  }
}
