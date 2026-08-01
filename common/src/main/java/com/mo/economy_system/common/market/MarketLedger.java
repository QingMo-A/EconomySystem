package com.mo.economy_system.common.market;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Single shared authority for market order state. */
public final class MarketLedger {
    public static final int MAX_ORDERS = 10_000;
    private final Runnable dirtyCallback;
    private final List<MarketOrder> orders = new ArrayList<>();

    public MarketLedger(Runnable dirtyCallback) {
        this.dirtyCallback = Objects.requireNonNull(dirtyCallback, "dirtyCallback");
    }

    public synchronized List<MarketOrder> orders() { return List.copyOf(orders); }
    public synchronized boolean isFull() { return orders.size() >= MAX_ORDERS; }

    public synchronized boolean add(MarketOrder order) {
        Objects.requireNonNull(order, "order");
        if (isFull() || orders.stream().anyMatch(existing -> existing.tradeId().equals(order.tradeId()))) return false;
        orders.add(0, order);
        try {
            dirtyCallback.run();
        } catch (RuntimeException exception) {
            orders.remove(0);
            throw exception;
        }
        return true;
    }

    public synchronized boolean remove(UUID tradeId) {
        boolean changed = orders.removeIf(order -> order.tradeId().equals(tradeId));
        if (changed) dirtyCallback.run();
        return changed;
    }

    public synchronized void restore(List<MarketOrder> restored) {
        Objects.requireNonNull(restored, "restored");
        if (restored.size() > MAX_ORDERS) throw new IllegalArgumentException("too many market orders");
        Set<UUID> ids = new HashSet<>();
        for (MarketOrder order : restored) {
            if (!ids.add(order.tradeId())) throw new IllegalArgumentException("duplicate trade id: " + order.tradeId());
        }
        orders.clear();
        orders.addAll(restored);
    }
}
