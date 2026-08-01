package com.mo.economy_system.common.market;

import com.mo.economy_system.platform.item.ItemStackSnapshotCodec;
import com.mo.economy_system.platform.item.ItemStackSnapshotResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

/** Stable NBT codec for newly written market orders. */
public final class MarketOrderCodec {
    private MarketOrderCodec() {}

    public static CompoundTag encode(MarketOrder order) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", order.type().id());
        tag.putUUID("tradeID", order.tradeId());
        tag.put("itemStack", ItemStackSnapshotCodec.encode(order.item()).orElseThrow());
        tag.putInt("listedCount", order.quantity());
        tag.putInt("basePrice", order.totalPrice());
        tag.putString("sellerName", order.sellerName());
        tag.putUUID("sellerID", order.sellerId());
        tag.putLong("listingTime", order.listingTime());
        tag.putLong("expirationTime", order.expirationTime());
        if (order.type() == MarketOrderType.DEMAND) tag.putBoolean("delivered", order.delivered());
        return tag;
    }

    public static ItemStackSnapshotResult<MarketOrder> decodeCurrent(CompoundTag tag) {
        try {
            Set<String> allowed = new HashSet<>(Set.of("type", "tradeID", "itemStack", "listedCount", "basePrice",
                    "sellerName", "sellerID", "listingTime", "expirationTime", "delivered"));
            Set<String> unknown = new HashSet<>(tag.getAllKeys()); unknown.removeAll(allowed);
            if (!unknown.isEmpty()) throw new IllegalArgumentException("unknown fields: " + unknown);
            require(tag, "type", Tag.TAG_STRING); require(tag, "tradeID", Tag.TAG_INT_ARRAY);
            require(tag, "itemStack", Tag.TAG_COMPOUND); require(tag, "listedCount", Tag.TAG_INT);
            require(tag, "basePrice", Tag.TAG_INT); require(tag, "sellerName", Tag.TAG_STRING);
            require(tag, "sellerID", Tag.TAG_INT_ARRAY); require(tag, "listingTime", Tag.TAG_LONG);
            require(tag, "expirationTime", Tag.TAG_LONG);
            MarketOrderType type = MarketOrderType.fromPersistentId(tag.getString("type"));
            if (type == MarketOrderType.SALES && tag.contains("delivered")) throw new IllegalArgumentException("sales order has delivered field");
            if (type == MarketOrderType.DEMAND && !tag.contains("delivered", Tag.TAG_BYTE)) throw new IllegalArgumentException("demand order lacks delivered");
            ItemStackSnapshotResult<com.mo.economy_system.platform.item.ItemStackSnapshot> item =
                    ItemStackSnapshotCodec.decode(tag.getCompound("itemStack"));
            if (!item.isSuccess()) return ItemStackSnapshotResult.failure(item.error().orElseThrow(), item.detail());
            return ItemStackSnapshotResult.success(new MarketOrder(type, tag.getUUID("tradeID"), item.orElseThrow(),
                    tag.getInt("listedCount"), tag.getInt("basePrice"), tag.getString("sellerName"),
                    tag.getUUID("sellerID"), tag.getLong("listingTime"), tag.getLong("expirationTime"),
                    type == MarketOrderType.DEMAND && tag.getBoolean("delivered")));
        } catch (RuntimeException exception) {
            return ItemStackSnapshotResult.failure(com.mo.economy_system.platform.item.ItemStackSnapshotError.INVALID_SCHEMA,
                    "invalid market order: " + exception.getMessage());
        }
    }

    private static void require(CompoundTag tag, String key, int type) {
        if (!tag.contains(key, type)) throw new IllegalArgumentException(key + " has wrong or missing type");
    }
}
