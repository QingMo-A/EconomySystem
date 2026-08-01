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
    private long revision;

    public MarketLedger(Runnable dirtyCallback) {
        this.dirtyCallback = Objects.requireNonNull(dirtyCallback, "dirtyCallback");
    }

    public synchronized List<MarketOrder> orders() { return List.copyOf(orders); }
    public synchronized long revision() { return revision; }
    public synchronized MarketLedgerView view() { return new MarketLedgerView(revision, orders); }
    public synchronized boolean isFull() { return orders.size() >= MAX_ORDERS; }
    public synchronized MarketOrder find(UUID tradeId) {
        return orders.stream().filter(order -> order.tradeId().equals(tradeId)).findFirst().orElse(null);
    }

    public synchronized boolean add(MarketOrder order) {
        Objects.requireNonNull(order, "order");
        if (isFull() || orders.stream().anyMatch(existing -> existing.tradeId().equals(order.tradeId()))) return false;
        requireRevisionCapacity(); orders.add(0, order);
        try {
            dirtyCallback.run();
        } catch (RuntimeException exception) {
            orders.remove(0);
            throw exception;
        }
        revision++; return true;
    }

    public synchronized boolean remove(UUID tradeId) {
        for (int index=0;index<orders.size();index++) if (orders.get(index).tradeId().equals(tradeId)) {
            requireRevisionCapacity(); MarketOrder removed=orders.remove(index);
            try { dirtyCallback.run(); }
            catch (RuntimeException exception) { orders.add(index,removed);throw exception; }
            revision++; return true;
        }
        return false;
    }

    public synchronized DemandOrderRemovalResult removeUndeliveredDemand(UUID tradeId) {
        Objects.requireNonNull(tradeId, "tradeId");
        for (int index=0;index<orders.size();index++) {
            MarketOrder order=orders.get(index);
            if (!order.tradeId().equals(tradeId)) continue;
            if (order.type()!=MarketOrderType.DEMAND) return DemandOrderRemovalResult.failure(DemandOrderRemovalStatus.WRONG_ORDER_TYPE);
            if (order.delivered()) return DemandOrderRemovalResult.failure(DemandOrderRemovalStatus.ALREADY_DELIVERED);
            requireRevisionCapacity(); orders.remove(index);
            try { dirtyCallback.run(); }
            catch (RuntimeException exception) { orders.add(index,order);return DemandOrderRemovalResult.failure(DemandOrderRemovalStatus.PERSIST_FAILED); }
            int originalIndex=index;
            revision++; return DemandOrderRemovalResult.removed(new MarketOrderRemoval(order,()->restoreRemoval(order,originalIndex)));
        }
        return DemandOrderRemovalResult.failure(DemandOrderRemovalStatus.NOT_FOUND);
    }

    private synchronized MarketOrderRestoreResult restoreRemoval(MarketOrder order,int index) {
        if (orders.stream().anyMatch(existing->existing.tradeId().equals(order.tradeId()))) return MarketOrderRestoreResult.DUPLICATE_ID;
        requireRevisionCapacity(); int restoredIndex=Math.min(index,orders.size());orders.add(restoredIndex,order);
        try { dirtyCallback.run(); }
        catch (RuntimeException exception) { orders.remove(restoredIndex);return MarketOrderRestoreResult.PERSIST_FAILED; }
        revision++; return MarketOrderRestoreResult.RESTORED;
    }

    public synchronized void restore(List<MarketOrder> restored) {
        validateRestored(restored); orders.clear();orders.addAll(restored);
    }

    public synchronized void replace(List<MarketOrder> restored) {
        validateRestored(restored); requireRevisionCapacity(); List<MarketOrder> before=List.copyOf(orders);
        orders.clear(); orders.addAll(restored);
        try { dirtyCallback.run(); } catch (RuntimeException exception) { orders.clear();orders.addAll(before);throw exception; }
        revision++;
    }

    public synchronized void load(List<MarketOrder> restored,long loadedRevision) {
        if(loadedRevision<0)throw new IllegalArgumentException("negative market revision"); validateRestored(restored);
        orders.clear();orders.addAll(restored);revision=loadedRevision;
    }

    private static void validateRestored(List<MarketOrder> restored) {
        Objects.requireNonNull(restored, "restored");
        if (restored.size() > MAX_ORDERS) throw new IllegalArgumentException("too many market orders");
        Set<UUID> ids = new HashSet<>();
        for (MarketOrder order : restored) {
            if (!ids.add(order.tradeId())) throw new IllegalArgumentException("duplicate trade id: " + order.tradeId());
        }
    }

    public synchronized DemandDeliveryTransitionResult markDemandDelivered(UUID tradeId) {
        Objects.requireNonNull(tradeId, "tradeId");
        for (int index = 0; index < orders.size(); index++) {
            MarketOrder current = orders.get(index);
            if (!current.tradeId().equals(tradeId)) continue;
            if (current.type() != MarketOrderType.DEMAND) return DemandDeliveryTransitionResult.WRONG_ORDER_TYPE;
            if (current.delivered()) return DemandDeliveryTransitionResult.ALREADY_DELIVERED;
            requireRevisionCapacity(); MarketOrder updated = new MarketOrder(current.type(), current.tradeId(), current.item(), current.quantity(),
                    current.totalPrice(), current.sellerName(), current.sellerId(), current.listingTime(),
                    current.expirationTime(), true);
            orders.set(index, updated);
            try {
                dirtyCallback.run();
            } catch (RuntimeException exception) {
                orders.set(index, current);
                return DemandDeliveryTransitionResult.PERSIST_FAILED;
            }
            revision++; return DemandDeliveryTransitionResult.UPDATED;
        }
        return DemandDeliveryTransitionResult.NOT_FOUND;
    }
    private void requireRevisionCapacity(){if(revision==Long.MAX_VALUE)throw new IllegalStateException("market revision exhausted");}
}
