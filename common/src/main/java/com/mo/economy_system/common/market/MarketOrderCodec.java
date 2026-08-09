package com.mo.economy_system.common.market;

import com.mo.economy_system.platform.item.ItemStackSnapshotCodec;
import com.mo.economy_system.platform.item.ItemStackSnapshotResult;
import com.mo.economy_system.platform.nbt.NbtData;

import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

/** Stable NBT codec for newly written market orders. */
public final class MarketOrderCodec {
    private MarketOrderCodec() {}

    public static NbtData.Compound encode(MarketOrder order) {
        NbtData.CompoundBuilder tag = NbtData.compoundBuilder()
                .putString("type", order.type().id())
                .putUuid("tradeID", order.tradeId())
                .put("itemStack", ItemStackSnapshotCodec.encode(order.item()).orElseThrow())
                .putInt("listedCount", order.quantity())
                .putInt("basePrice", order.totalPrice())
                .putString("sellerName", order.sellerName())
                .putUuid("sellerID", order.sellerId())
                .putLong("listingTime", order.listingTime())
                .putLong("expirationTime", order.expirationTime());
        if (order.type() == MarketOrderType.DEMAND) tag.putBoolean("delivered", order.delivered());
        return tag.build();
    }

    public static ItemStackSnapshotResult<MarketOrder> decodeCurrent(NbtData.Compound tag) {
        try {
            Set<String> allowed = new HashSet<>(Set.of("type", "tradeID", "itemStack", "listedCount", "basePrice",
                    "sellerName", "sellerID", "listingTime", "expirationTime", "delivered"));
            Set<String> unknown = new HashSet<>(tag.keys()); unknown.removeAll(allowed);
            if (!unknown.isEmpty()) throw new IllegalArgumentException("unknown fields: " + unknown);
            require(tag, "type", NbtData.StringValue.class); require(tag, "tradeID", NbtData.IntArrayValue.class);
            require(tag, "itemStack", NbtData.Compound.class); require(tag, "listedCount", NbtData.IntValue.class);
            require(tag, "basePrice", NbtData.IntValue.class); require(tag, "sellerName", NbtData.StringValue.class);
            require(tag, "sellerID", NbtData.IntArrayValue.class); require(tag, "listingTime", NbtData.LongValue.class);
            require(tag, "expirationTime", NbtData.LongValue.class);
            MarketOrderType type = MarketOrderType.fromPersistentId(((NbtData.StringValue) tag.get("type")).value());
            if (type == MarketOrderType.SALES && tag.contains("delivered")) throw new IllegalArgumentException("sales order has delivered field");
            if (type == MarketOrderType.DEMAND && !(tag.get("delivered") instanceof NbtData.ByteValue)) throw new IllegalArgumentException("demand order lacks delivered");
            ItemStackSnapshotResult<com.mo.economy_system.platform.item.ItemStackSnapshot> item =
                    ItemStackSnapshotCodec.decode((NbtData.Compound) tag.get("itemStack"));
            if (!item.isSuccess()) return ItemStackSnapshotResult.failure(item.error().orElseThrow(), item.detail());
            return ItemStackSnapshotResult.success(new MarketOrder(type, NbtData.readUuid(tag.get("tradeID")), item.orElseThrow(),
                    ((NbtData.IntValue) tag.get("listedCount")).value(), ((NbtData.IntValue) tag.get("basePrice")).value(), ((NbtData.StringValue) tag.get("sellerName")).value(),
                    NbtData.readUuid(tag.get("sellerID")), ((NbtData.LongValue) tag.get("listingTime")).value(), ((NbtData.LongValue) tag.get("expirationTime")).value(),
                    type == MarketOrderType.DEMAND && ((NbtData.ByteValue) tag.get("delivered")).value() != 0));
        } catch (RuntimeException exception) {
            return ItemStackSnapshotResult.failure(com.mo.economy_system.platform.item.ItemStackSnapshotError.INVALID_SCHEMA,
                    "invalid market order: " + exception.getMessage());
        }
    }

    private static void require(NbtData.Compound tag, String key, Class<? extends NbtData> type) {
        if (!type.isInstance(tag.get(key))) throw new IllegalArgumentException(key + " has wrong or missing type");
    }
}
