package com.mo.economy_system.network.commission_public;

import com.mo.economy_system.common.commission.PublicCommission;
import com.mo.economy_system.common.commission.PublicCommissionStatus;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.commission_public.PublicCommissionActionResponseMessage;
import com.mo.economy_system.common.network.commission_public.PublicCommissionDataResponseKind;
import com.mo.economy_system.common.network.commission_public.PublicCommissionDataResponseMessage;
import com.mo.economy_system.common.network.commission_public.PublicCommissionDataRequestMessage;
import com.mo.economy_system.common.network.commission_public.PublicCommissionSubmitMessage;
import com.mo.economy_system.common.network.commission_public.PublicCommissionSubmitStatus;
import com.mo.economy_system.platform.network.WireBuffer;
import java.util.ArrayList;
import java.util.List;

/** Stable field order and defensive bounds for the public commission protocol. */
public final class PublicCommissionWireCodec {
  private static final int MAX_PUBLIC_NAME_LENGTH = 128;

  private PublicCommissionWireCodec() {}

  public static void encodeDataRequest(PublicCommissionDataRequestMessage message, WireBuffer buffer) {
    buffer.writeLong(message.requestId());
  }

  public static PublicCommissionDataRequestMessage decodeDataRequest(WireBuffer buffer) {
    return new PublicCommissionDataRequestMessage(buffer.readLong());
  }

  public static void encodeSubmit(PublicCommissionSubmitMessage message, WireBuffer buffer) {
    buffer.writeLong(message.requestId());
    buffer.writeUuid(message.commissionId());
    buffer.writeUuid(message.submissionId());
    buffer.writeInt(message.amount());
  }

  public static PublicCommissionSubmitMessage decodeSubmit(WireBuffer buffer) {
    return new PublicCommissionSubmitMessage(buffer.readLong(), buffer.readUuid(),
        buffer.readUuid(), buffer.readInt());
  }

  public static void encodeDataResponse(PublicCommissionDataResponseMessage message,
                                        WireBuffer buffer) {
    buffer.writeInt(message.kind().ordinal());
    buffer.writeLong(message.requestId());
    buffer.writeLong(message.serverNowMillis());
    buffer.writeInt(message.commissions().size());
    for (PublicCommission commission : message.commissions()) encodeCommission(commission, buffer);
    buffer.writeUtf(message.errorKey(), EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH);
  }

  public static PublicCommissionDataResponseMessage decodeDataResponse(WireBuffer buffer) {
    PublicCommissionDataResponseKind kind = enumValue(buffer.readInt(),
        PublicCommissionDataResponseKind.values(), "public commission response kind");
    long requestId = buffer.readLong();
    long now = buffer.readLong();
    int size = buffer.readInt();
    if (size < 0 || size > EconomyNetworkLimits.MAX_COMMISSION_ENTRIES) {
      throw new IllegalArgumentException("invalid public commission response size: " + size);
    }
    List<PublicCommission> commissions = new ArrayList<>(size);
    for (int index = 0; index < size; index++) commissions.add(decodeCommission(buffer));
    String error = buffer.readUtf(EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH);
    return new PublicCommissionDataResponseMessage(kind, requestId, now, commissions, error);
  }

  public static void encodeActionResponse(PublicCommissionActionResponseMessage message,
                                          WireBuffer buffer) {
    buffer.writeLong(message.requestId());
    buffer.writeInt(message.status().ordinal());
    buffer.writeInt(message.acceptedAmount());
    buffer.writeInt(message.payout());
    buffer.writeUtf(message.message(), EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH);
  }

  public static PublicCommissionActionResponseMessage decodeActionResponse(WireBuffer buffer) {
    long requestId = buffer.readLong();
    PublicCommissionSubmitStatus status = enumValue(buffer.readInt(),
        PublicCommissionSubmitStatus.values(), "public commission action status");
    return new PublicCommissionActionResponseMessage(requestId, status, buffer.readInt(),
        buffer.readInt(), buffer.readUtf(EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH));
  }

  private static void encodeCommission(PublicCommission value, WireBuffer buffer) {
    buffer.writeUuid(value.commissionId());
    buffer.writeUtf(value.name(), MAX_PUBLIC_NAME_LENGTH);
    buffer.writeUtf(value.requesterId(), EconomyNetworkLimits.MAX_COMMISSION_TEMPLATE_LENGTH);
    buffer.writeUtf(value.requesterName(), EconomyNetworkLimits.MAX_COMMISSION_REQUESTER_LENGTH);
    buffer.writeUtf(value.targetSnapshot(), EconomyNetworkLimits.MAX_COMMISSION_TARGET_LENGTH);
    buffer.writeInt(value.targetAmount());
    buffer.writeInt(value.unitReward());
    buffer.writeLong(value.generatedAt());
    buffer.writeLong(value.expiresAt());
    buffer.writeUtf(value.description(), EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH);
    buffer.writeInt(value.status().ordinal());
    buffer.writeInt(value.remainingAmount());
    buffer.writeInt(value.remainingBudget());
  }

  private static PublicCommission decodeCommission(WireBuffer buffer) {
    return new PublicCommission(
        buffer.readUuid(),
        buffer.readUtf(MAX_PUBLIC_NAME_LENGTH),
        buffer.readUtf(EconomyNetworkLimits.MAX_COMMISSION_TEMPLATE_LENGTH),
        buffer.readUtf(EconomyNetworkLimits.MAX_COMMISSION_REQUESTER_LENGTH),
        buffer.readUtf(EconomyNetworkLimits.MAX_COMMISSION_TARGET_LENGTH),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readLong(),
        buffer.readLong(),
        buffer.readUtf(EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH),
        enumValue(buffer.readInt(), PublicCommissionStatus.values(), "public commission status"),
        buffer.readInt(),
        buffer.readInt());
  }

  private static <T> T enumValue(int ordinal, T[] values, String field) {
    if (ordinal < 0 || ordinal >= values.length) throw new IllegalArgumentException("invalid " + field);
    return values[ordinal];
  }
}
