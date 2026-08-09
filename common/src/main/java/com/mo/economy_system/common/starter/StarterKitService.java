package com.mo.economy_system.common.starter;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.Objects;
import java.util.UUID;

/** Loader-neutral, exactly-once starter-kit claim transaction. */
public final class StarterKitService {
  public static final int REWARD_AMOUNT = 10_000;
  private static final String CATEGORY = "系统";
  private static final String CREDIT_REASON = "新手礼包";
  private static final String ROLLBACK_REASON = "新手礼包回滚";

  public enum Result {
    SUCCESS,
    ALREADY_CLAIMED,
    BALANCE_LIMIT,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  public record Outcome(Result result, int amount) {
    public Outcome {
      Objects.requireNonNull(result, "result");
      if (amount < 0) throw new IllegalArgumentException("amount");
      if ((result == Result.SUCCESS) != (amount == REWARD_AMOUNT)) {
        throw new IllegalArgumentException("result/amount");
      }
    }
  }

  @FunctionalInterface
  public interface Diagnostics {
    void warning(String operation, UUID playerId, Throwable primary, Throwable secondary);
  }

  private final StarterKitPort marker;
  private final StarterKitAccountPort accounts;
  private final Diagnostics diagnostics;

  public StarterKitService(
      StarterKitPort marker, StarterKitAccountPort accounts, Diagnostics diagnostics) {
    this.marker = Objects.requireNonNull(marker, "marker");
    this.accounts = Objects.requireNonNull(accounts, "accounts");
    this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
  }

  public StarterKitService(StarterKitPort marker, StarterKitAccountPort accounts) {
    this(marker, accounts, (operation, playerId, primary, secondary) -> {});
  }

  public synchronized Outcome claim(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    boolean claimed;
    try {
      claimed = marker.claimed(playerId);
    } catch (Exception error) {
      warn("read-marker", playerId, error, null);
      return failure(Result.STATE_UNKNOWN);
    }
    if (claimed) return failure(Result.ALREADY_CLAIMED);

    BalanceMutationResult credit;
    try {
      credit = Objects.requireNonNull(
          accounts.credit(playerId, REWARD_AMOUNT, CATEGORY, CREDIT_REASON), "credit result");
    } catch (RuntimeException error) {
      warn("credit", playerId, error, null);
      return failure(Result.STATE_UNKNOWN);
    }
    switch (credit) {
      case BALANCE_LIMIT -> { return failure(Result.BALANCE_LIMIT); }
      case PERSIST_FAILED -> { return failure(Result.PERSIST_FAILED); }
      case INVALID_AMOUNT, INSUFFICIENT_FUNDS -> {
        warn("credit-result", playerId,
            new IllegalStateException("unexpected credit result: " + credit), null);
        return failure(Result.STATE_UNKNOWN);
      }
      case SUCCESS -> { }
    }

    try {
      marker.markClaimed(playerId);
      return new Outcome(Result.SUCCESS, REWARD_AMOUNT);
    } catch (Exception markerError) {
      boolean unmarked = false;
      try {
        marker.unmarkClaimed(playerId);
        unmarked = true;
      } catch (Exception unmarkError) {
        markerError.addSuppressed(unmarkError);
      }
      BalanceMutationResult rollback;
      try {
        rollback = accounts.debit(playerId, REWARD_AMOUNT, CATEGORY, ROLLBACK_REASON);
      } catch (RuntimeException rollbackError) {
        warn("marker-rollback", playerId, markerError, rollbackError);
        return failure(Result.STATE_UNKNOWN);
      }
      if (rollback != BalanceMutationResult.SUCCESS) {
        warn("marker-rollback", playerId, markerError,
            new IllegalStateException("credit rollback result: " + rollback));
        return failure(Result.STATE_UNKNOWN);
      }
      // A failed mark may have partially committed.  Do not report a retryable
      // failure unless the marker state was explicitly restored as well.
      if (!unmarked) {
        warn("marker-state-unknown", playerId, markerError, null);
        return failure(Result.STATE_UNKNOWN);
      }
      warn("mark-claimed", playerId, markerError, null);
      return failure(Result.PERSIST_FAILED);
    }
  }

  private static Outcome failure(Result result) {
    return new Outcome(result, 0);
  }

  private void warn(String operation, UUID playerId, Throwable primary, Throwable secondary) {
    try {
      diagnostics.warning(operation, playerId, primary, secondary);
    } catch (RuntimeException ignored) {
      // Diagnostics cannot change the account/marker result.
    }
  }
}
