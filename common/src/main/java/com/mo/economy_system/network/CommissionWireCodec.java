package com.mo.economy_system.network;

import com.mo.economy_system.common.commission.CommissionInstance;
import com.mo.economy_system.common.commission.CommissionRewardSnapshot;
import com.mo.economy_system.common.commission.CommissionStatus;
import com.mo.economy_system.common.commission.CommissionType;
import com.mo.economy_system.common.network.CommissionActionResponseMessage;
import com.mo.economy_system.common.network.CommissionDataResponseKind;
import com.mo.economy_system.common.network.CommissionDataResponseMessage;
import com.mo.economy_system.common.network.CommissionDataRequestMessage;
import com.mo.economy_system.common.network.CommissionSubmitMessage;
import com.mo.economy_system.common.network.CommissionSubmitStatus;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.platform.network.WireBuffer;
import java.util.ArrayList;
import java.util.List;

/** Shared field order and bounds for the personal commission UI protocol. */
public final class CommissionWireCodec {
  private CommissionWireCodec() {}

  public static void encodeDataRequest(CommissionDataRequestMessage message, WireBuffer buffer) {
    buffer.writeLong(message.requestId());
  }

  public static CommissionDataRequestMessage decodeDataRequest(WireBuffer buffer) {
    return new CommissionDataRequestMessage(buffer.readLong());
  }

  public static void encodeSubmit(CommissionSubmitMessage message, WireBuffer buffer) {
    buffer.writeLong(message.requestId());
    buffer.writeUuid(message.commissionId());
    buffer.writeUuid(message.submissionId());
    buffer.writeInt(message.amount());
  }

  public static CommissionSubmitMessage decodeSubmit(WireBuffer buffer) {
    return new CommissionSubmitMessage(buffer.readLong(), buffer.readUuid(), buffer.readUuid(), buffer.readInt());
  }

  public static void encodeDataResponse(CommissionDataResponseMessage message, WireBuffer buffer) {
    buffer.writeInt(message.kind().ordinal());
    buffer.writeLong(message.requestId());
    buffer.writeLong(message.serverNowMillis());
    buffer.writeLong(message.nextRefreshAt());
    buffer.writeInt(message.maxActivePersonalCommissions());
    buffer.writeUtf(message.errorKey(), EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH);
    buffer.writeInt(message.commissions().size());
    for (CommissionInstance commission : message.commissions()) encodeCommission(commission, buffer);
  }

  public static CommissionDataResponseMessage decodeDataResponse(WireBuffer buffer) {
    CommissionDataResponseKind kind = enumValue(buffer.readInt(), CommissionDataResponseKind.values(), "commission response kind");
    long requestId = buffer.readLong();
    long now = buffer.readLong();
    long next = buffer.readLong();
    int maxActive = buffer.readInt();
    String error = buffer.readUtf(EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH);
    int size = buffer.readInt();
    if (size < 0 || size > EconomyNetworkLimits.MAX_COMMISSION_ENTRIES) {
      throw new IllegalArgumentException("invalid commission response size: " + size);
    }
    List<CommissionInstance> commissions = new ArrayList<>(size);
    for (int index = 0; index < size; index++) commissions.add(decodeCommission(buffer));
    return new CommissionDataResponseMessage(kind, requestId, now, next, maxActive, commissions, error);
  }

  public static void encodeActionResponse(CommissionActionResponseMessage message, WireBuffer buffer) {
    buffer.writeLong(message.requestId());
    buffer.writeInt(message.status().ordinal());
    buffer.writeUtf(message.message(), EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH);
  }

  public static CommissionActionResponseMessage decodeActionResponse(WireBuffer buffer) {
    long requestId = buffer.readLong();
    CommissionSubmitStatus status = enumValue(buffer.readInt(), CommissionSubmitStatus.values(), "commission action status");
    return new CommissionActionResponseMessage(requestId, status,
        buffer.readUtf(EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH));
  }

  private static void encodeCommission(CommissionInstance value, WireBuffer buffer) {
    buffer.writeUuid(value.commissionId());
    buffer.writeUuid(value.ownerPlayerId());
    buffer.writeUtf(value.templateId(), EconomyNetworkLimits.MAX_COMMISSION_TEMPLATE_LENGTH);
    buffer.writeUtf(value.type().id(), 64);
    buffer.writeUtf(value.requesterId(), EconomyNetworkLimits.MAX_COMMISSION_TEMPLATE_LENGTH);
    buffer.writeUtf(value.requesterName(), EconomyNetworkLimits.MAX_COMMISSION_REQUESTER_LENGTH);
    buffer.writeUtf(value.targetSnapshot(), EconomyNetworkLimits.MAX_COMMISSION_TARGET_LENGTH);
    buffer.writeInt(value.requiredAmount());
    buffer.writeInt(value.progress());
    buffer.writeUtf(value.rewardSnapshot().currencyId(), 64);
    buffer.writeInt(value.rewardSnapshot().amount());
    buffer.writeUtf(value.rewardSnapshot().description(), EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH);
    buffer.writeLong(value.generatedAt());
    buffer.writeLong(value.expiresAt());
    buffer.writeInt(value.status().ordinal());
    buffer.writeUtf(value.text(), EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH);
  }

  private static CommissionInstance decodeCommission(WireBuffer buffer) {
    return new CommissionInstance(
        buffer.readUuid(), buffer.readUuid(),
        buffer.readUtf(EconomyNetworkLimits.MAX_COMMISSION_TEMPLATE_LENGTH),
        CommissionType.fromId(buffer.readUtf(64)),
        buffer.readUtf(EconomyNetworkLimits.MAX_COMMISSION_TEMPLATE_LENGTH),
        buffer.readUtf(EconomyNetworkLimits.MAX_COMMISSION_REQUESTER_LENGTH),
        buffer.readUtf(EconomyNetworkLimits.MAX_COMMISSION_TARGET_LENGTH),
        buffer.readInt(), buffer.readInt(),
        new CommissionRewardSnapshot(buffer.readUtf(64), buffer.readInt(),
            buffer.readUtf(EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH)),
        buffer.readLong(), buffer.readLong(),
        enumValue(buffer.readInt(), CommissionStatus.values(), "commission status"),
        buffer.readUtf(EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH));
  }

  private static <T> T enumValue(int ordinal, T[] values, String field) {
    if (ordinal < 0 || ordinal >= values.length) throw new IllegalArgumentException("invalid " + field);
    return values[ordinal];
  }
}
