package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.MarketDataPayloadBudget;
import com.mo.economy_system.common.network.MarketDataRequestMessage;
import com.mo.economy_system.common.network.MarketDataRequestPurpose;
import com.mo.economy_system.common.network.MarketDataResponseKind;
import com.mo.economy_system.common.network.MarketDataResponseMessage;
import com.mo.economy_system.common.network.MarketOrderFilter;
import com.mo.economy_system.common.network.MarketOrderSnapshot;
import com.mo.economy_system.common.network.MarketOrderSort;
import com.mo.economy_system.platform.item.ItemStackSnapshotCodec;
import com.mo.economy_system.target.forge1201.item.Forge1201NbtAdapter;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

final class Forge1201MarketDataCodec {
  static void encodeRequest(MarketDataRequestMessage message, FriendlyByteBuf buffer) {
    buffer.writeLong(message.requestId());
    buffer.writeEnum(message.purpose());
    buffer.writeInt(message.offset());
    buffer.writeInt(message.limit());
    buffer.writeEnum(message.filter());
    buffer.writeEnum(message.sort());
    buffer.writeUtf(message.query(), EconomyNetworkLimits.MAX_MARKET_QUERY_LENGTH);
    buffer.writeBoolean(message.focusTradeId() != null);
    if (message.focusTradeId() != null) buffer.writeUUID(message.focusTradeId());
  }

  static MarketDataRequestMessage decodeRequest(FriendlyByteBuf buffer) {
    try {
      long requestId = buffer.readLong();
      MarketDataRequestPurpose purpose = readEnum(buffer, MarketDataRequestPurpose.values());
      int offset = buffer.readInt();
      int limit = buffer.readInt();
      MarketOrderFilter filter = readEnum(buffer, MarketOrderFilter.values());
      MarketOrderSort sort = readEnum(buffer, MarketOrderSort.values());
      String query = buffer.readUtf(EconomyNetworkLimits.MAX_MARKET_QUERY_LENGTH);
      UUID focusTradeId = buffer.readBoolean() ? buffer.readUUID() : null;
      return new MarketDataRequestMessage(
          requestId, purpose, offset, limit, filter, sort, query, focusTradeId);
    } catch (RuntimeException error) {
      throw new DecoderException("invalid market request", error);
    }
  }

  static void encodeResponse(MarketDataResponseMessage message, FriendlyByteBuf buffer) {
    MarketDataPayloadBudget.requireWithinLimit(message);
    buffer.writeEnum(message.kind());
    buffer.writeLong(message.requestId());
    buffer.writeLong(message.marketRevision());
    buffer.writeInt(message.offset());
    buffer.writeInt(message.limit());
    buffer.writeInt(message.totalMatched());
    buffer.writeInt(message.totalSales());
    buffer.writeInt(message.totalDemand());
    buffer.writeInt(message.orders().size());
    for (MarketOrderSnapshot order : message.orders()) writeOrder(order, buffer);
  }

  static MarketDataResponseMessage decodeResponse(FriendlyByteBuf buffer) {
    try {
      if (buffer.readableBytes() > EconomyNetworkLimits.MAX_MARKET_RESPONSE_WIRE_BYTES) {
        throw new DecoderException("market response exceeds wire limit");
      }
      MarketDataResponseKind kind = readEnum(buffer, MarketDataResponseKind.values());
      long id = buffer.readLong();
      long revision = buffer.readLong();
      int offset = buffer.readInt();
      int limit = buffer.readInt();
      int matched = buffer.readInt();
      int sales = buffer.readInt();
      int demand = buffer.readInt();
      int size = buffer.readInt();
      if (size < 0 || size > EconomyNetworkLimits.MAX_MARKET_PAGE_SIZE) {
        throw new DecoderException("invalid order count");
      }
      List<MarketOrderSnapshot> orders = new ArrayList<>(size);
      for (int i = 0; i < size; i++) orders.add(readOrder(buffer));
      return new MarketDataResponseMessage(
          kind, id, revision, offset, limit, matched, sales, demand, orders);
    } catch (RuntimeException error) {
      if (error instanceof DecoderException decoderException) throw decoderException;
      throw new DecoderException("invalid market response", error);
    }
  }

  private static void writeOrder(MarketOrderSnapshot order, FriendlyByteBuf buffer) {
    buffer.writeEnum(order.type());
    buffer.writeUUID(order.tradeId());
    buffer.writeNbt(Forge1201NbtAdapter.toNative(
        ItemStackSnapshotCodec.encode(order.item()).orElseThrow()));
    buffer.writeInt(order.quantity());
    buffer.writeInt(order.totalPrice());
    buffer.writeUtf(order.ownerName(), EconomyNetworkLimits.MAX_MARKET_OWNER_NAME_LENGTH);
    buffer.writeUUID(order.ownerId());
    buffer.writeLong(order.listingTime());
    buffer.writeLong(order.expirationTime());
    buffer.writeBoolean(order.delivered());
  }

  private static MarketOrderSnapshot readOrder(FriendlyByteBuf buffer) {
    MarketOrderType type = readEnum(buffer, MarketOrderType.values());
    UUID id = buffer.readUUID();
    CompoundTag tag = buffer.readNbt();
    if (tag == null) throw new DecoderException("missing snapshot");
    return new MarketOrderSnapshot(
        type,
        id,
        ItemStackSnapshotCodec.decode(Forge1201NbtAdapter.fromNative(tag)).orElseThrow(),
        buffer.readInt(),
        buffer.readInt(),
        buffer.readUtf(EconomyNetworkLimits.MAX_MARKET_OWNER_NAME_LENGTH),
        buffer.readUUID(),
        buffer.readLong(),
        buffer.readLong(),
        buffer.readBoolean());
  }

  private static <E> E readEnum(FriendlyByteBuf buffer, E[] values) {
    int ordinal = buffer.readVarInt();
    if (ordinal < 0 || ordinal >= values.length) throw new DecoderException("invalid enum");
    return values[ordinal];
  }
}
