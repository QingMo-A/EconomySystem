package com.mo.economy_system.network;

import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.network.DeliveryBoxClaimMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataRequestMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataResponseMessage;
import com.mo.economy_system.common.network.DeliveryBoxResponseKind;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.platform.item.ItemStackSnapshotCodec;
import com.mo.economy_system.platform.nbt.NbtData;
import com.mo.economy_system.platform.network.WireBuffer;
import com.mo.economy_system.platform.network.WireDecodeException;
import java.util.ArrayList;
import java.util.List;

public final class DeliveryBoxWireCodec {
  private DeliveryBoxWireCodec() {}

  public static void encodeRequest(DeliveryBoxDataRequestMessage message, WireBuffer buffer) {
    buffer.writeLong(message.requestId());
  }

  public static DeliveryBoxDataRequestMessage decodeRequest(WireBuffer buffer) {
    requireBytes(buffer, Long.BYTES);
    DeliveryBoxDataRequestMessage result = new DeliveryBoxDataRequestMessage(buffer.readLong());
    requireConsumed(buffer);
    return result;
  }

  public static void encodeClaim(DeliveryBoxClaimMessage message, WireBuffer buffer) {
    buffer.writeUuid(message.entryId());
    buffer.writeLong(message.requestId());
  }

  public static DeliveryBoxClaimMessage decodeClaim(WireBuffer buffer) {
    requireBytes(buffer, 24);
    DeliveryBoxClaimMessage result = new DeliveryBoxClaimMessage(buffer.readUuid(), buffer.readLong());
    requireConsumed(buffer);
    return result;
  }

  public static void encodeResponse(DeliveryBoxDataResponseMessage message, WireBuffer buffer) {
    try (WireBuffer temporary = buffer.temporary()) {
      temporary.writeUtf(message.kind().id(), 16);
      temporary.writeLong(message.requestId());
      temporary.writeInt(message.entries().size());
      for (DeliveryBoxEntrySnapshot entry : message.entries()) {
        temporary.writeUuid(entry.entryId());
        temporary.writeUtf(entry.source(), EconomyNetworkLimits.MAX_DELIVERY_SOURCE_LENGTH);
        temporary.writeNbt(ItemStackSnapshotCodec.encode(entry.item()).orElseThrow());
      }
      int size = temporary.readableBytes();
      if (size > EconomyNetworkLimits.MAX_DELIVERY_RESPONSE_WIRE_BYTES) {
        throw new IllegalArgumentException("delivery response exceeds wire budget");
      }
      buffer.writeRemaining(temporary);
    }
  }

  public static DeliveryBoxDataResponseMessage decodeResponse(WireBuffer buffer) {
    if (buffer.readableBytes() > EconomyNetworkLimits.MAX_DELIVERY_RESPONSE_WIRE_BYTES) {
      throw new WireDecodeException("delivery response exceeds wire budget");
    }
    DeliveryBoxResponseKind kind;
    try {
      kind = DeliveryBoxResponseKind.fromId(buffer.readUtf(16));
    } catch (RuntimeException failure) {
      throw new WireDecodeException("invalid delivery response kind", failure);
    }
    long requestId = buffer.readLong();
    int count = buffer.readInt();
    if (count < 0 || count > EconomyNetworkLimits.MAX_DELIVERY_BOX_ENTRIES) {
      throw new WireDecodeException("invalid delivery entry count: " + count);
    }
    List<DeliveryBoxEntrySnapshot> entries = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      java.util.UUID entryId = buffer.readUuid();
      String source = buffer.readUtf(EconomyNetworkLimits.MAX_DELIVERY_SOURCE_LENGTH);
      NbtData.Compound item = buffer.readNbt();
      if (item == null) throw new WireDecodeException("missing delivery item snapshot");
      try {
        entries.add(new DeliveryBoxEntrySnapshot(
            entryId, ItemStackSnapshotCodec.decode(item).orElseThrow(), source));
      } catch (RuntimeException failure) {
        throw new WireDecodeException("invalid delivery entry", failure);
      }
    }
    requireConsumed(buffer);
    try {
      return kind == DeliveryBoxResponseKind.DATA
          ? DeliveryBoxDataResponseMessage.data(requestId, entries)
          : new DeliveryBoxDataResponseMessage(kind, requestId, entries);
    } catch (RuntimeException failure) {
      throw new WireDecodeException("invalid delivery response", failure);
    }
  }

  private static void requireBytes(WireBuffer buffer, int count) {
    if (buffer.readableBytes() < count) throw new WireDecodeException("truncated delivery payload");
  }

  private static void requireConsumed(WireBuffer buffer) {
    if (buffer.isReadable()) throw new WireDecodeException("trailing delivery payload data");
  }
}
