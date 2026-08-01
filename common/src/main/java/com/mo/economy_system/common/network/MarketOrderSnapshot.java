package com.mo.economy_system.common.network;

import com.mo.economy_system.common.market.MarketOrder;
import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshotValidator;
import java.util.Objects;
import java.util.UUID;

public record MarketOrderSnapshot(MarketOrderType type, UUID tradeId, ItemStackSnapshot item, int quantity,
                                  int totalPrice, String ownerName, UUID ownerId, long listingTime,
                                  long expirationTime, boolean delivered) {
    public MarketOrderSnapshot {
        Objects.requireNonNull(type); Objects.requireNonNull(tradeId); Objects.requireNonNull(item);
        Objects.requireNonNull(ownerName); Objects.requireNonNull(ownerId);
        if (ownerName.isBlank() || ownerName.length() > EconomyNetworkLimits.MAX_MARKET_OWNER_NAME_LENGTH || item.count()!=1
                || !ItemStackSnapshotValidator.validate(item).isSuccess()
                || quantity<=0 || totalPrice<=0 || expirationTime<listingTime
                || type==MarketOrderType.SALES && delivered) throw new IllegalArgumentException("invalid market order snapshot");
    }
    public static MarketOrderSnapshot from(MarketOrder order) {
        return new MarketOrderSnapshot(order.type(),order.tradeId(),order.item(),order.quantity(),order.totalPrice(),
                order.sellerName(),order.sellerId(),order.listingTime(),order.expirationTime(),order.delivered());
    }
}
