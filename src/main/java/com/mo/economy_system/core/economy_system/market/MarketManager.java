package com.mo.economy_system.core.economy_system.market;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MarketManager {
    private static final List<MarketItem> marketItems = new ArrayList<>();

    public static List<MarketItem> getMarketItems() {
        return new ArrayList<>(marketItems);
    }

    public static void setMarketItems(List<MarketItem> items) {
        marketItems.clear();
        marketItems.addAll(items);
    }

    public static void addMarketItem(MarketItem item) {
        marketItems.add(0, item);
    }

    public static void removeMarketItem(MarketItem item) {
        marketItems.remove(item);
    }

    public static void clearMarketItems() {
        marketItems.clear();
    }

    public static MarketItem getMarketItemById(UUID itemId) {
        for (MarketItem item : marketItems) {
            if (item.getTradeID().equals(itemId)) {
                return item;
            }
        }
        return null;
    }
}