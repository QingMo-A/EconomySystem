package com.mo.economy_system.common.market;

import com.mo.economy_system.platform.item.ItemStackSnapshot;

import java.util.Objects;
import java.util.UUID;

/** Immutable loader-neutral market order. Item template count is always one. */
public record MarketOrder(
        MarketOrderType type,
        UUID tradeId,
        ItemStackSnapshot item,
        int quantity,
        int totalPrice,
        String sellerName,
        UUID sellerId,
        long listingTime,
        long expirationTime,
        boolean delivered
) {
    public static final long EXPIRATION_DURATION_MILLIS = 3L * 24L * 60L * 60L * 1000L;

    /** Expiration is inclusive so an order is never visible after its persisted deadline. */
    public static boolean isExpiredAt(long expirationTime, long nowMillis) {
        return nowMillis >= expirationTime;
    }

    public MarketOrder {
        type = Objects.requireNonNull(type, "type");
        tradeId = Objects.requireNonNull(tradeId, "tradeId");
        item = Objects.requireNonNull(item, "item");
        sellerName = Objects.requireNonNull(sellerName, "sellerName");
        sellerId = Objects.requireNonNull(sellerId, "sellerId");
        if (item.count() != 1) throw new IllegalArgumentException("item template count must be one");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (totalPrice <= 0) throw new IllegalArgumentException("totalPrice must be positive");
        if (expirationTime < listingTime) throw new IllegalArgumentException("expirationTime precedes listingTime");
        if (type == MarketOrderType.SALES && delivered) throw new IllegalArgumentException("sales order cannot be delivered");
    }
}
