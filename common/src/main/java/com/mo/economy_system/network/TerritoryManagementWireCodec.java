package com.mo.economy_system.network;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.ModifyTerritoryModeMessage;
import com.mo.economy_system.common.network.SingleTerritoryDataRequestMessage;
import com.mo.economy_system.common.network.SingleTerritoryDataResponseKind;
import com.mo.economy_system.common.network.SingleTerritoryDataResponseMessage;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.network.TransferTerritoryOwnershipMessage;
import com.mo.economy_system.common.network.UnlockTerritoryBuffMessage;
import com.mo.economy_system.common.network.UpdateTerritoryPermissionMessage;
import com.mo.economy_system.common.network.UpdateTerritoryRuleMessage;
import com.mo.economy_system.common.network.UpgradeTerritoryBuffMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

/** Strict, NBT-free management wire formats for protocols 36-43. */
public final class TerritoryManagementWireCodec {
  private TerritoryManagementWireCodec() {}

  public static void encodeModifyMode(ModifyTerritoryModeMessage message, FriendlyByteBuf buffer) {
    buffer.writeUUID(message.territoryId());
  }

  public static ModifyTerritoryModeMessage decodeModifyMode(FriendlyByteBuf buffer) {
    requireBytes(buffer, 16);
    ModifyTerritoryModeMessage result = new ModifyTerritoryModeMessage(buffer.readUUID());
    requireConsumed(buffer);
    return result;
  }

  public static void encodeUnlockBuff(UnlockTerritoryBuffMessage message, FriendlyByteBuf buffer) {
    buffer.writeUUID(message.territoryId());
    buffer.writeUtf(message.buffId(), EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH);
  }

  public static UnlockTerritoryBuffMessage decodeUnlockBuff(FriendlyByteBuf buffer) {
    requireBytes(buffer, 17);
    UnlockTerritoryBuffMessage result = new UnlockTerritoryBuffMessage(
        buffer.readUUID(), buffer.readUtf(EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH));
    requireConsumed(buffer);
    return result;
  }

  public static void encodeUpgradeBuff(UpgradeTerritoryBuffMessage message, FriendlyByteBuf buffer) {
    buffer.writeUUID(message.territoryId());
    buffer.writeUtf(message.buffId(), EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH);
  }

  public static UpgradeTerritoryBuffMessage decodeUpgradeBuff(FriendlyByteBuf buffer) {
    requireBytes(buffer, 17);
    UpgradeTerritoryBuffMessage result = new UpgradeTerritoryBuffMessage(
        buffer.readUUID(), buffer.readUtf(EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH));
    requireConsumed(buffer);
    return result;
  }

  public static void encodeSingleRequest(
      SingleTerritoryDataRequestMessage message, FriendlyByteBuf buffer) {
    buffer.writeUUID(message.territoryId());
    buffer.writeLong(message.requestId());
  }

  public static SingleTerritoryDataRequestMessage decodeSingleRequest(FriendlyByteBuf buffer) {
    requireBytes(buffer, 24);
    SingleTerritoryDataRequestMessage result =
        new SingleTerritoryDataRequestMessage(buffer.readUUID(), buffer.readLong());
    requireConsumed(buffer);
    return result;
  }

  public static void encodeSingleResponse(
      SingleTerritoryDataResponseMessage message, FriendlyByteBuf buffer) {
    buffer.writeUtf(message.kind().id(), 16);
    if (message.kind() == SingleTerritoryDataResponseKind.DATA) {
      TerritoryDataWireCodec.encodeResponse(
          TerritoryDataResponseMessage.data(
              message.requestId(), List.of(message.territory().orElseThrow()), List.of()),
          buffer);
    } else {
      buffer.writeLong(message.requestId());
    }
  }

  public static SingleTerritoryDataResponseMessage decodeSingleResponse(FriendlyByteBuf buffer) {
    SingleTerritoryDataResponseKind kind;
    try {
      kind = SingleTerritoryDataResponseKind.fromId(buffer.readUtf(16));
    } catch (RuntimeException failure) {
      throw new DecoderException("invalid single territory response kind", failure);
    }
    if (kind != SingleTerritoryDataResponseKind.DATA) {
      requireBytes(buffer, Long.BYTES);
      long requestId = buffer.readLong();
      requireConsumed(buffer);
      return SingleTerritoryDataResponseMessage.empty(kind, requestId);
    }
    TerritoryDataResponseMessage nested = TerritoryDataWireCodec.decodeResponse(buffer);
    if (nested.owned().size() != 1 || !nested.authorized().isEmpty()) {
      throw new DecoderException("invalid single territory data payload");
    }
    return SingleTerritoryDataResponseMessage.data(nested.requestId(), nested.owned().get(0));
  }

  public static void encodePermission(
      UpdateTerritoryPermissionMessage message, FriendlyByteBuf buffer) {
    buffer.writeUUID(message.territoryId());
    buffer.writeUUID(message.targetPlayerId());
    buffer.writeBoolean(message.allowed());
  }

  public static UpdateTerritoryPermissionMessage decodePermission(FriendlyByteBuf buffer) {
    requireBytes(buffer, 33);
    UpdateTerritoryPermissionMessage result = new UpdateTerritoryPermissionMessage(
        buffer.readUUID(), buffer.readUUID(), buffer.readBoolean());
    requireConsumed(buffer);
    return result;
  }

  public static void encodeTransfer(
      TransferTerritoryOwnershipMessage message, FriendlyByteBuf buffer) {
    buffer.writeUUID(message.territoryId());
    buffer.writeUUID(message.targetPlayerId());
  }

  public static TransferTerritoryOwnershipMessage decodeTransfer(FriendlyByteBuf buffer) {
    requireBytes(buffer, 32);
    TransferTerritoryOwnershipMessage result =
        new TransferTerritoryOwnershipMessage(buffer.readUUID(), buffer.readUUID());
    requireConsumed(buffer);
    return result;
  }

  public static void encodeRule(UpdateTerritoryRuleMessage message, FriendlyByteBuf buffer) {
    buffer.writeUUID(message.territoryId());
    buffer.writeUtf(message.action().id(), 32);
    buffer.writeUtf(message.level().id(), 32);
  }

  public static UpdateTerritoryRuleMessage decodeRule(FriendlyByteBuf buffer) {
    requireBytes(buffer, 18);
    try {
      UpdateTerritoryRuleMessage result = new UpdateTerritoryRuleMessage(
          buffer.readUUID(), RuleAction.fromId(buffer.readUtf(32)), RuleLevel.fromId(buffer.readUtf(32)));
      requireConsumed(buffer);
      return result;
    } catch (IllegalArgumentException failure) {
      throw new DecoderException("invalid territory rule", failure);
    }
  }

  private static void requireBytes(FriendlyByteBuf buffer, int count) {
    if (buffer.readableBytes() < count) throw new DecoderException("truncated territory management payload");
  }

  private static void requireConsumed(FriendlyByteBuf buffer) {
    if (buffer.isReadable()) throw new DecoderException("trailing territory management payload data");
  }
}
