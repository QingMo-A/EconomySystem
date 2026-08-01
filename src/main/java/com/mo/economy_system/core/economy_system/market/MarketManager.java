package com.mo.economy_system.core.economy_system.market;

import net.minecraft.server.level.ServerLevel;
import java.util.List;
import java.util.UUID;
import com.mo.economy_system.common.market.DemandDeliveryTransitionResult;

/** Compatibility facade; MarketSavedData/MarketLedger remain the only authority. */
public final class MarketManager {
    private static MarketSavedData data;
    private MarketManager() {}
    static synchronized void bind(MarketSavedData value) { data = value; }
    static synchronized MarketSavedData boundDataForTest() { return data; }
    private static MarketSavedData data() { if (data == null) throw new IllegalStateException("market is not initialized"); return data; }
    /** Returned items are detached read-only compatibility views; mutating them never updates the ledger. */
    public static synchronized List<MarketItem> getMarketItems() { return data().getMarketItems(); }
    public static synchronized void setMarketItems(List<MarketItem> items) { data().replaceMarketItems(items); }
    public static synchronized void addMarketItem(MarketItem item) { data().addMarketItem(item); }
    public static synchronized void removeMarketItem(MarketItem item) { data().removeMarketItem(item); }
    public static synchronized boolean removeMarketItemById(UUID id) { return data().removeOrder(id); }
    public static synchronized void clearMarketItems() { data().clearMarketItems(); }
    /** Returns a detached read-only compatibility view. */
    public static synchronized MarketItem getMarketItemById(UUID id) {
        return data().getMarketItems().stream().filter(item -> item.getTradeID().equals(id)).findFirst().orElse(null);
    }
    public static synchronized DemandDeliveryTransitionResult markDemandOrderDelivered(UUID id) {
        return data().markDemandDelivered(id);
    }
    public static synchronized void saveTo(ServerLevel level) { MarketSavedData.getInstance(level).setDirty(); }
}
