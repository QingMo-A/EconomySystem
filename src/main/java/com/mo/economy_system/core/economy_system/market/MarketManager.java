package com.mo.economy_system.core.economy_system.market;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

public class MarketManager {
    private static final List<MarketItem> marketItems = new ArrayList<>();

    public static synchronized List<MarketItem> getMarketItems() {
        return new ArrayList<>(marketItems);
    }

    public static synchronized void setMarketItems(List<MarketItem> items) {
        marketItems.clear();
        marketItems.addAll(items);
    }

    public static synchronized void addMarketItem(MarketItem item) {
        marketItems.add(0, item);
    }

    public static synchronized void removeMarketItem(MarketItem item) {
        marketItems.remove(item);
    }

    public static synchronized boolean removeMarketItemById(UUID itemId) {
        return marketItems.removeIf(item -> item.getTradeID().equals(itemId));
    }

    public static synchronized void clearMarketItems() {
        marketItems.clear();
    }

    public static synchronized MarketItem getMarketItemById(UUID itemId) {
        for (MarketItem item : marketItems) {
            if (item.getTradeID().equals(itemId)) {
                return item;
            }
        }
        return null;
    }

    public static synchronized void saveTo(ServerLevel level) {
        MarketSavedData marketData = MarketSavedData.getInstance(level.getServer().overworld());
        marketData.clearMarketItems();
        for (MarketItem item : marketItems) {
            marketData.addMarketItem(item);
        }
    }
}
