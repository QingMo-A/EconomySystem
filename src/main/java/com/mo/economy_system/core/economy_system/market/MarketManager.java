package com.mo.economy_system.core.economy_system.market;

import net.minecraft.server.level.ServerLevel;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

/** Compatibility facade for protocols not yet migrated; state lives only in MarketSavedData's common ledger. */
public final class MarketManager {
    private static MarketSavedData data;
    private static List<MarketItem> compatibilityView;
    private MarketManager() {}
    static synchronized void bind(MarketSavedData value) { data = value; }
    private static MarketSavedData data() { if (data == null) throw new IllegalStateException("market is not initialized"); return data; }
    public static synchronized List<MarketItem> getMarketItems() {
        compatibilityView = data().getMarketItems();
        return new ArrayList<>(compatibilityView);
    }
    public static synchronized void setMarketItems(List<MarketItem> items) { compatibilityView = new ArrayList<>(items); data().replaceMarketItems(items); }
    public static synchronized void addMarketItem(MarketItem item) { data().addMarketItem(item); compatibilityView = data().getMarketItems(); }
    public static synchronized void removeMarketItem(MarketItem item) { data().removeMarketItem(item); if (compatibilityView != null) compatibilityView.removeIf(value -> value.getTradeID().equals(item.getTradeID())); }
    public static synchronized boolean removeMarketItemById(UUID id) { boolean removed = data().removeOrder(id); if (compatibilityView != null) compatibilityView.removeIf(value -> value.getTradeID().equals(id)); return removed; }
    public static synchronized void clearMarketItems() { data().clearMarketItems(); compatibilityView = new ArrayList<>(); }
    public static synchronized MarketItem getMarketItemById(UUID id) { compatibilityView = data().getMarketItems(); return compatibilityView.stream().filter(i -> i.getTradeID().equals(id)).findFirst().orElse(null); }
    public static synchronized void saveTo(ServerLevel level) {
        MarketSavedData target = MarketSavedData.getInstance(level);
        if (compatibilityView != null) target.replaceMarketItems(compatibilityView); else target.setDirty();
    }
}
