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

  public synchronized List<MarketOrder> orders() {
    return List.copyOf(orders);
  }

  public synchronized long revision() {
    return revision;
  }

  public synchronized MarketLedgerView view() {
    return new MarketLedgerView(revision, orders);
  }

  public synchronized boolean isFull() {
    return orders.size() >= MAX_ORDERS;
  }

  public synchronized MarketOrder find(UUID tradeId) {
    return orders.stream()
        .filter(order -> order.tradeId().equals(tradeId))
        .findFirst()
        .orElse(null);
  }

  public synchronized boolean add(MarketOrder order) {
    Objects.requireNonNull(order, "order");
    if (isFull()
        || orders.stream().anyMatch(existing -> existing.tradeId().equals(order.tradeId())))
      return false;
    requireRevisionCapacity();
    orders.add(0, order);
    try {
      dirtyCallback.run();
    } catch (RuntimeException exception) {
      orders.remove(0);
      throw exception;
    }
    revision++;
    return true;
  }

  public synchronized boolean remove(UUID tradeId) {
    for (int index = 0; index < orders.size(); index++)
      if (orders.get(index).tradeId().equals(tradeId)) {
        requireRevisionCapacity();
        MarketOrder removed = orders.remove(index);
        try {
          dirtyCallback.run();
        } catch (RuntimeException exception) {
          orders.add(index, removed);
          throw exception;
        }
        revision++;
        return true;
      }
    return false;
  }

  public synchronized DemandOrderRemovalResult removeUndeliveredDemand(UUID tradeId) {
    Objects.requireNonNull(tradeId, "tradeId");
    for (int index = 0; index < orders.size(); index++) {
      MarketOrder order = orders.get(index);
      if (!order.tradeId().equals(tradeId)) continue;
      if (order.type() != MarketOrderType.DEMAND)
        return DemandOrderRemovalResult.failure(DemandOrderRemovalStatus.WRONG_ORDER_TYPE);
      if (order.delivered())
        return DemandOrderRemovalResult.failure(DemandOrderRemovalStatus.ALREADY_DELIVERED);
      requireRevisionCapacity();
      orders.remove(index);
      try {
        dirtyCallback.run();
      } catch (RuntimeException exception) {
        orders.add(index, order);
        return DemandOrderRemovalResult.failure(DemandOrderRemovalStatus.PERSIST_FAILED);
      }
      int originalIndex = index;
      revision++;
      return DemandOrderRemovalResult.removed(
          new MarketOrderRemoval(order, () -> restoreRemoval(order, originalIndex)));
    }
    return DemandOrderRemovalResult.failure(DemandOrderRemovalStatus.NOT_FOUND);
  }

  public synchronized DemandOrderRemovalResult removeUndeliveredDemandIfUnchanged(
      UUID tradeId, MarketOrder expectedOrder) {
    Objects.requireNonNull(tradeId, "tradeId");
    Objects.requireNonNull(expectedOrder, "expectedOrder");
    for (int index = 0; index < orders.size(); index++) {
      MarketOrder order = orders.get(index);
      if (!order.tradeId().equals(tradeId)) continue;
      if (order.type() != MarketOrderType.DEMAND)
        return DemandOrderRemovalResult.failure(DemandOrderRemovalStatus.WRONG_ORDER_TYPE);
      if (order.delivered())
        return DemandOrderRemovalResult.failure(DemandOrderRemovalStatus.ALREADY_DELIVERED);
      if (!order.equals(expectedOrder))
        return DemandOrderRemovalResult.failure(DemandOrderRemovalStatus.ORDER_CHANGED);
      if (revision == Long.MAX_VALUE)
        return DemandOrderRemovalResult.failure(DemandOrderRemovalStatus.PERSIST_FAILED);
      orders.remove(index);
      try { dirtyCallback.run(); }
      catch (RuntimeException error) {
        orders.add(index, order);
        return DemandOrderRemovalResult.failure(DemandOrderRemovalStatus.PERSIST_FAILED);
      }
      int originalIndex = index;
      revision++;
      return DemandOrderRemovalResult.removed(
          new MarketOrderRemoval(order, () -> restoreRemoval(order, originalIndex)));
    }
    return DemandOrderRemovalResult.failure(DemandOrderRemovalStatus.NOT_FOUND);
  }

  public synchronized SalesOrderRemovalResult removeSalesTransactional(UUID tradeId) {
    Objects.requireNonNull(tradeId, "tradeId");
    RemovalAttempt attempt = removeMatching(tradeId, MarketOrderType.SALES);
    return switch (attempt.status()) {
      case REMOVED -> SalesOrderRemovalResult.removed(attempt.removal());
      case NOT_FOUND -> SalesOrderRemovalResult.failure(SalesOrderRemovalStatus.NOT_FOUND);
      case WRONG_ORDER_TYPE ->
          SalesOrderRemovalResult.failure(SalesOrderRemovalStatus.WRONG_ORDER_TYPE);
      case PERSIST_FAILED ->
          SalesOrderRemovalResult.failure(SalesOrderRemovalStatus.PERSIST_FAILED);
    };
  }

  public synchronized DeliveredDemandRemovalResult removeDeliveredDemandTransactional(
      UUID tradeId) {
    Objects.requireNonNull(tradeId, "tradeId");
    for (int index = 0; index < orders.size(); index++) {
      MarketOrder order = orders.get(index);
      if (!order.tradeId().equals(tradeId)) continue;
      if (order.type() != MarketOrderType.DEMAND)
        return DeliveredDemandRemovalResult.failure(DeliveredDemandRemovalStatus.WRONG_ORDER_TYPE);
      if (!order.delivered())
        return DeliveredDemandRemovalResult.failure(DeliveredDemandRemovalStatus.NOT_DELIVERED);
      requireRevisionCapacity();
      orders.remove(index);
      try {
        dirtyCallback.run();
      } catch (RuntimeException exception) {
        orders.add(index, order);
        return DeliveredDemandRemovalResult.failure(DeliveredDemandRemovalStatus.PERSIST_FAILED);
      }
      int originalIndex = index;
      revision++;
      return DeliveredDemandRemovalResult.removed(
          new MarketOrderRemoval(order, () -> restoreRemoval(order, originalIndex)));
    }
    return DeliveredDemandRemovalResult.failure(DeliveredDemandRemovalStatus.NOT_FOUND);
  }

  private RemovalAttempt removeMatching(UUID tradeId, MarketOrderType expectedType) {
    for (int index = 0; index < orders.size(); index++) {
      MarketOrder order = orders.get(index);
      if (!order.tradeId().equals(tradeId)) continue;
      if (order.type() != expectedType)
        return RemovalAttempt.failure(RemovalStatus.WRONG_ORDER_TYPE);
      requireRevisionCapacity();
      orders.remove(index);
      try {
        dirtyCallback.run();
      } catch (RuntimeException exception) {
        orders.add(index, order);
        return RemovalAttempt.failure(RemovalStatus.PERSIST_FAILED);
      }
      int originalIndex = index;
      revision++;
      return RemovalAttempt.removed(
          new MarketOrderRemoval(order, () -> restoreRemoval(order, originalIndex)));
    }
    return RemovalAttempt.failure(RemovalStatus.NOT_FOUND);
  }

  private synchronized MarketOrderRestoreResult restoreRemoval(MarketOrder order, int index) {
    if (orders.stream().anyMatch(existing -> existing.tradeId().equals(order.tradeId())))
      return MarketOrderRestoreResult.DUPLICATE_ID;
    requireRevisionCapacity();
    int restoredIndex = Math.min(index, orders.size());
    orders.add(restoredIndex, order);
    try {
      dirtyCallback.run();
    } catch (RuntimeException exception) {
      orders.remove(restoredIndex);
      return MarketOrderRestoreResult.PERSIST_FAILED;
    }
    revision++;
    return MarketOrderRestoreResult.RESTORED;
  }

  public synchronized void replaceAll(List<MarketOrder> restored) {
    validateRestored(restored);
    requireRevisionCapacity();
    List<MarketOrder> before = List.copyOf(orders);
    orders.clear();
    orders.addAll(restored);
    try {
      dirtyCallback.run();
    } catch (RuntimeException exception) {
      orders.clear();
      orders.addAll(before);
      throw exception;
    }
    revision++;
  }

  public synchronized void loadFromPersistence(List<MarketOrder> restored, long loadedRevision) {
    if (loadedRevision < 0) throw new IllegalArgumentException("negative market revision");
    validateRestored(restored);
    orders.clear();
    orders.addAll(restored);
    revision = loadedRevision;
  }

  private enum RemovalStatus {
    REMOVED,
    NOT_FOUND,
    WRONG_ORDER_TYPE,
    PERSIST_FAILED
  }

  private record RemovalAttempt(RemovalStatus status, MarketOrderRemoval removal) {
    static RemovalAttempt failure(RemovalStatus status) {
      return new RemovalAttempt(status, null);
    }

    static RemovalAttempt removed(MarketOrderRemoval removal) {
      return new RemovalAttempt(RemovalStatus.REMOVED, removal);
    }
  }

  private static void validateRestored(List<MarketOrder> restored) {
    Objects.requireNonNull(restored, "restored");
    if (restored.size() > MAX_ORDERS) throw new IllegalArgumentException("too many market orders");
    Set<UUID> ids = new HashSet<>();
    for (MarketOrder order : restored) {
      if (!ids.add(order.tradeId()))
        throw new IllegalArgumentException("duplicate trade id: " + order.tradeId());
    }
  }

  public synchronized DemandDeliveryTransition markDemandDeliveredIfUnchanged(
      UUID tradeId, MarketOrder expectedOrder) {
    Objects.requireNonNull(tradeId, "tradeId");
    Objects.requireNonNull(expectedOrder, "expectedOrder");
    for (int index = 0; index < orders.size(); index++) {
      MarketOrder current = orders.get(index);
      if (!current.tradeId().equals(tradeId)) continue;
      if (current.type() != MarketOrderType.DEMAND)
        return DemandDeliveryTransition.failure(DemandDeliveryTransitionStatus.WRONG_ORDER_TYPE);
      if (current.delivered())
        return DemandDeliveryTransition.failure(DemandDeliveryTransitionStatus.ALREADY_DELIVERED);
      if (!current.equals(expectedOrder))
        return DemandDeliveryTransition.failure(DemandDeliveryTransitionStatus.ORDER_CHANGED);
      if (revision == Long.MAX_VALUE)
        return DemandDeliveryTransition.failure(DemandDeliveryTransitionStatus.PERSIST_FAILED);
      MarketOrder updated =
          new MarketOrder(
              current.type(),
              current.tradeId(),
              current.item(),
              current.quantity(),
              current.totalPrice(),
              current.sellerName(),
              current.sellerId(),
              current.listingTime(),
              current.expirationTime(),
              true);
      orders.set(index, updated);
      try {
        dirtyCallback.run();
      } catch (RuntimeException exception) {
        orders.set(index, current);
        return DemandDeliveryTransition.failure(DemandDeliveryTransitionStatus.PERSIST_FAILED);
      }
      revision++;
      return DemandDeliveryTransition.updated(current, updated);
    }
    return DemandDeliveryTransition.failure(DemandDeliveryTransitionStatus.NOT_FOUND);
  }

  private void requireRevisionCapacity() {
    if (revision == Long.MAX_VALUE) throw new IllegalStateException("market revision exhausted");
  }
}
