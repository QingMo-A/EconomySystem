package com.mo.economy_system.network;

import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.network.DeliveryBoxClaimMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataRequestMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataResponseMessage;
import com.mo.economy_system.common.network.DeliveryBoxResponseKind;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.platform.item.ItemStackSnapshotCodec;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public final class DeliveryBoxWireCodec {
  private DeliveryBoxWireCodec() {}

  public static void encodeRequest(DeliveryBoxDataRequestMessage message, FriendlyByteBuf buffer) {
    buffer.writeLong(message.requestId());
  }

  public static DeliveryBoxDataRequestMessage decodeRequest(FriendlyByteBuf buffer) {
    requireBytes(buffer, Long.BYTES);
    DeliveryBoxDataRequestMessage result = new DeliveryBoxDataRequestMessage(buffer.readLong());
    requireConsumed(buffer);
    return result;
  }

  public static void encodeClaim(DeliveryBoxClaimMessage message, FriendlyByteBuf buffer) {
    buffer.writeUUID(message.entryId());
    buffer.writeLong(message.requestId());
  }

  public static DeliveryBoxClaimMessage decodeClaim(FriendlyByteBuf buffer) {
    requireBytes(buffer, 24);
    DeliveryBoxClaimMessage result = new DeliveryBoxClaimMessage(buffer.readUUID(), buffer.readLong());
    requireConsumed(buffer);
    return result;
  }

  public static void encodeResponse(DeliveryBoxDataResponseMessage message, FriendlyByteBuf buffer) {
    FriendlyByteBuf temporary = new FriendlyByteBuf(Unpooled.buffer());
    try {
      temporary.writeUtf(message.kind().id(), 16);
      temporary.writeLong(message.requestId());
      temporary.writeInt(message.entries().size());
      for (DeliveryBoxEntrySnapshot entry : message.entries()) {
        temporary.writeUUID(entry.entryId());
        temporary.writeUtf(entry.source(), EconomyNetworkLimits.MAX_DELIVERY_SOURCE_LENGTH);
        temporary.writeNbt(ItemStackSnapshotCodec.encode(entry.item()).orElseThrow());
      }
      int size = temporary.readableBytes();
      if (size > EconomyNetworkLimits.MAX_DELIVERY_RESPONSE_WIRE_BYTES) {
        throw new IllegalArgumentException("delivery response exceeds wire budget");
      }
      buffer.writeBytes(temporary, temporary.readerIndex(), size);
    } finally {
      temporary.release();
    }
  }

  public static DeliveryBoxDataResponseMessage decodeResponse(FriendlyByteBuf buffer) {
    if (buffer.readableBytes() > EconomyNetworkLimits.MAX_DELIVERY_RESPONSE_WIRE_BYTES) {
      throw new DecoderException("delivery response exceeds wire budget");
    }
    DeliveryBoxResponseKind kind;
    try {
      kind = DeliveryBoxResponseKind.fromId(buffer.readUtf(16));
    } catch (RuntimeException failure) {
      throw new DecoderException("invalid delivery response kind", failure);
    }
    long requestId = buffer.readLong();
    int count = buffer.readInt();
    if (count < 0 || count > EconomyNetworkLimits.MAX_DELIVERY_BOX_ENTRIES) {
      throw new DecoderException("invalid delivery entry count: " + count);
    }
    List<DeliveryBoxEntrySnapshot> entries = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      java.util.UUID entryId = buffer.readUUID();
      String source = buffer.readUtf(EconomyNetworkLimits.MAX_DELIVERY_SOURCE_LENGTH);
      CompoundTag item = buffer.readNbt();
      if (item == null) throw new DecoderException("missing delivery item snapshot");
      try {
        entries.add(new DeliveryBoxEntrySnapshot(
            entryId, ItemStackSnapshotCodec.decode(item).orElseThrow(), source));
      } catch (RuntimeException failure) {
        throw new DecoderException("invalid delivery entry", failure);
      }
    }
    requireConsumed(buffer);
    try {
      return kind == DeliveryBoxResponseKind.DATA
          ? DeliveryBoxDataResponseMessage.data(requestId, entries)
          : new DeliveryBoxDataResponseMessage(kind, requestId, entries);
    } catch (RuntimeException failure) {
      throw new DecoderException("invalid delivery response", failure);
    }
  }

  private static void requireBytes(FriendlyByteBuf buffer, int count) {
    if (buffer.readableBytes() < count) throw new DecoderException("truncated delivery payload");
  }

  private static void requireConsumed(FriendlyByteBuf buffer) {
    if (buffer.isReadable()) throw new DecoderException("trailing delivery payload data");
  }
}
