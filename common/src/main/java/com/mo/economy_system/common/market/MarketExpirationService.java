package com.mo.economy_system.common.market;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Common authority for expiring market orders and compensating their owner. */
public final class MarketExpirationService {
  public static final String ACCOUNT_CATEGORY = "市场";
  public static final String REFUND_REASON = "求购单过期退款";
  public static final String DELIVERY_SOURCE = "Market";

  private MarketExpirationService() {}

  /**
   * Expires every order whose persisted deadline has been reached. The repository compares the
   * complete order snapshot before removing it, so a concurrent mutation is never compensated
   * using stale data.
   */
  public static List<MarketExpirationOutcome> expire(long nowMillis, Context context) {
    if (nowMillis < 0) throw new IllegalArgumentException("nowMillis must be non-negative");
    Objects.requireNonNull(context, "context");
    List<MarketOrder> snapshot = List.copyOf(context.repository().orders());
    List<MarketExpirationOutcome> outcomes = new ArrayList<>();
    for (MarketOrder order : snapshot) {
      if (!MarketOrder.isExpiredAt(order.expirationTime(), nowMillis)) continue;
      outcomes.add(expireOne(order, context));
    }
    return List.copyOf(outcomes);
  }

  private static MarketExpirationOutcome expireOne(MarketOrder expected, Context context) {
    MarketOrderRemovalResult removal;
    try {
      removal = context.repository().removeIfUnchanged(expected);
    } catch (RuntimeException failure) {
      report(context, expected, "remove", MarketExpirationResult.STATE_UNKNOWN, failure);
      return new MarketExpirationOutcome(expected, MarketExpirationResult.STATE_UNKNOWN);
    }
    if (removal == null) {
      report(context, expected, "remove-null", MarketExpirationResult.STATE_UNKNOWN, null);
      return new MarketExpirationOutcome(expected, MarketExpirationResult.STATE_UNKNOWN);
    }
    switch (removal.status()) {
      case NOT_FOUND -> {
        return new MarketExpirationOutcome(expected, MarketExpirationResult.NOT_FOUND);
      }
      case ORDER_CHANGED -> {
        return new MarketExpirationOutcome(expected, MarketExpirationResult.ORDER_CHANGED);
      }
      case PERSIST_FAILED -> {
        return new MarketExpirationOutcome(expected, MarketExpirationResult.PERSIST_FAILED);
      }
      case REMOVED -> {
        // Continue below with the authoritative snapshot returned by the ledger.
      }
    }

    MarketOrder authoritative = removal.removal().order();
    if (authoritative.type() == MarketOrderType.DEMAND && !authoritative.delivered()) {
      BalanceMutationResult credit;
      try {
        credit = Objects.requireNonNull(
            context.accounts().credit(
                authoritative.sellerId(),
                authoritative.totalPrice(),
                ACCOUNT_CATEGORY,
                REFUND_REASON),
            "credit result");
      } catch (RuntimeException failure) {
        report(context, authoritative, "credit", MarketExpirationResult.STATE_UNKNOWN, failure);
        return new MarketExpirationOutcome(authoritative, MarketExpirationResult.STATE_UNKNOWN);
      }
      if (credit == BalanceMutationResult.SUCCESS) {
        return new MarketExpirationOutcome(authoritative, MarketExpirationResult.REFUNDED);
      }
      return rollbackAfterFailure(
          removal.removal(), authoritative, context, MarketExpirationResult.CREDIT_FAILED);
    }

    boolean delivered;
    try {
      delivered = context.delivery().enqueue(
          authoritative.sellerId(),
          authoritative.item(),
          authoritative.quantity(),
          DELIVERY_SOURCE);
    } catch (RuntimeException failure) {
      report(context, authoritative, "delivery", MarketExpirationResult.STATE_UNKNOWN, failure);
      return new MarketExpirationOutcome(authoritative, MarketExpirationResult.STATE_UNKNOWN);
    }
    if (delivered) {
      return new MarketExpirationOutcome(
          authoritative, MarketExpirationResult.RETURNED_TO_DELIVERY);
    }
    return rollbackAfterFailure(
        removal.removal(), authoritative, context, MarketExpirationResult.DELIVERY_FAILED);
  }

  private static MarketExpirationOutcome rollbackAfterFailure(
      MarketOrderRemoval removal,
      MarketOrder order,
      Context context,
      MarketExpirationResult failureResult) {
    try {
      if (removal.restore().restore() == MarketOrderRestoreResult.RESTORED) {
        return new MarketExpirationOutcome(order, failureResult);
      }
    } catch (RuntimeException failure) {
      report(context, order, "restore", MarketExpirationResult.STATE_UNKNOWN, failure);
      return new MarketExpirationOutcome(order, MarketExpirationResult.STATE_UNKNOWN);
    }
    report(context, order, "restore", MarketExpirationResult.STATE_UNKNOWN, null);
    return new MarketExpirationOutcome(order, MarketExpirationResult.STATE_UNKNOWN);
  }

  private static void report(
      Context context,
      MarketOrder order,
      String stage,
      MarketExpirationResult result,
      RuntimeException failure) {
    try {
      context.reporter().report(order.tradeId(), order.sellerId(), stage, result, failure);
    } catch (RuntimeException ignored) {
      // Diagnostics must not alter the persisted market state.
    }
  }

  public record Context(
      Repository repository, Accounts accounts, Delivery delivery, FailureReporter reporter) {
    public Context {
      Objects.requireNonNull(repository, "repository");
      Objects.requireNonNull(accounts, "accounts");
      Objects.requireNonNull(delivery, "delivery");
      Objects.requireNonNull(reporter, "reporter");
    }
  }

  public interface Repository {
    List<MarketOrder> orders();

    MarketOrderRemovalResult removeIfUnchanged(MarketOrder expected);
  }

  public interface Accounts {
    BalanceMutationResult credit(UUID ownerId, int amount, String category, String reason);
  }

  @FunctionalInterface
  public interface Delivery {
    boolean enqueue(UUID ownerId, com.mo.economy_system.platform.item.ItemStackSnapshot item,
        int quantity, String source);
  }

  @FunctionalInterface
  public interface FailureReporter {
    void report(UUID tradeId, UUID ownerId, String stage, MarketExpirationResult result,
        RuntimeException failure);

    static FailureReporter noop() {
      return (tradeId, ownerId, stage, result, failure) -> {};
    }
  }
}
