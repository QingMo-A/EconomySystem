package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.Objects;
import java.util.UUID;

/** Common transaction for creating a territory from two selected X/Z points. */
public final class TerritoryClaimService {
  public enum Result {
    SUCCESS,
    INVALID_INPUT,
    OVERLAP,
    PRICE_OVERFLOW,
    INSUFFICIENT_FUNDS,
    PAYMENT_FAILED,
    PERSIST_FAILED,
    STATE_UNKNOWN,
    REFUND_FAILED
  }

  public enum RepositoryResult {
    CREATED,
    OVERLAP,
    /** Persistence failed and the repository proved that no territory was created. */
    PERSIST_FAILED,
    /** The repository cannot prove whether the territory was created. */
    STATE_UNKNOWN
  }

  public record Request(
      UUID ownerId,
      String ownerName,
      String territoryName,
      String dimensionId,
      TerritorySnapshots.Position first,
      TerritorySnapshots.Position second) {
    public Request {
      Objects.requireNonNull(ownerId, "ownerId");
      ownerName = bounded(ownerName, EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH, "ownerName");
      territoryName = bounded(
          territoryName, EconomyNetworkLimits.MAX_TERRITORY_NAME_LENGTH, "territoryName");
      dimensionId = bounded(
          dimensionId, EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH, "dimensionId");
      Objects.requireNonNull(first, "first");
      Objects.requireNonNull(second, "second");
    }

    private static String bounded(String value, int max, String name) {
      Objects.requireNonNull(value, name);
      String normalized = value.trim();
      if (normalized.isEmpty() || normalized.length() > max) {
        throw new IllegalArgumentException(name);
      }
      return normalized;
    }
  }

  public record Outcome(Result result, long area, int price, Throwable failure) {
    public Outcome {
      Objects.requireNonNull(result, "result");
      if (area < 0 || price < 0) throw new IllegalArgumentException("negative claim values");
      if (failure instanceof Error error) throw error;
    }

    static Outcome of(Result result) {
      return new Outcome(result, 0, 0, null);
    }
  }

  public interface Balance {
    BalanceMutationResult debitExact(UUID ownerId, int amount);

    BalanceMutationResult creditExact(UUID ownerId, int amount);
  }

  public interface Repository {
    boolean overlaps(Request request);

    RepositoryResult create(Request request, long area, int price);
  }

  @FunctionalInterface
  public interface Diagnostics {
    void warning(String stage, UUID ownerId, Throwable failure);

    static Diagnostics noop() {
      return (stage, ownerId, failure) -> {};
    }
  }

  private TerritoryClaimService() {}

  public static Outcome execute(
      Request request, Balance balance, Repository repository, Diagnostics diagnostics) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(balance, "balance");
    Objects.requireNonNull(repository, "repository");
    Objects.requireNonNull(diagnostics, "diagnostics");
    if (request.first().y() != request.second().y()
        || !TerritoryGeometry.validCoordinate(request.first())
        || !TerritoryGeometry.validCoordinate(request.second())) {
      return Outcome.of(Result.INVALID_INPUT);
    }

    final long area;
    final int price;
    try {
      area = TerritoryGeometry.area(request.first(), request.second());
      long calculated = TerritoryPricing.priceForArea(area, TerritoryPricing.DEFAULT_PRICE_PER_CELL);
      if (calculated > Integer.MAX_VALUE) return new Outcome(Result.PRICE_OVERFLOW, area, 0, null);
      price = (int) calculated;
    } catch (ArithmeticException overflow) {
      warn(diagnostics, "price", request.ownerId(), overflow);
      return new Outcome(Result.PRICE_OVERFLOW, 0, 0, overflow);
    }

    final boolean overlaps;
    try {
      overlaps = repository.overlaps(request);
    } catch (RuntimeException failure) {
      warn(diagnostics, "overlap", request.ownerId(), failure);
      return new Outcome(Result.STATE_UNKNOWN, area, price, failure);
    }
    if (overlaps) return new Outcome(Result.OVERLAP, area, price, null);

    BalanceMutationResult debit;
    try {
      debit = balance.debitExact(request.ownerId(), price);
    } catch (RuntimeException failure) {
      warn(diagnostics, "debit", request.ownerId(), failure);
      return new Outcome(Result.STATE_UNKNOWN, area, price, failure);
    }
    if (debit == null) {
      IllegalStateException failure = new IllegalStateException("null claim debit result");
      warn(diagnostics, "debit", request.ownerId(), failure);
      return new Outcome(Result.STATE_UNKNOWN, area, price, failure);
    }
    if (debit != BalanceMutationResult.SUCCESS) {
      return new Outcome(
          debit == BalanceMutationResult.INSUFFICIENT_FUNDS
              ? Result.INSUFFICIENT_FUNDS : Result.PAYMENT_FAILED,
          area, price, null);
    }

    RepositoryResult created;
    try {
      created = repository.create(request, area, price);
    } catch (RuntimeException failure) {
      warn(diagnostics, "create", request.ownerId(), failure);
      created = RepositoryResult.STATE_UNKNOWN;
    }
    if (created == RepositoryResult.CREATED) return new Outcome(Result.SUCCESS, area, price, null);
    if (created == null || created == RepositoryResult.STATE_UNKNOWN) {
      // Creation may have succeeded before the repository reported an uncertain outcome. A
      // refund here could leave a live territory with no charge.
      return new Outcome(Result.STATE_UNKNOWN, area, price, null);
    }

    BalanceMutationResult refund;
    try {
      refund = balance.creditExact(request.ownerId(), price);
    } catch (RuntimeException failure) {
      warn(diagnostics, "refund", request.ownerId(), failure);
      return new Outcome(Result.REFUND_FAILED, area, price, failure);
    }
    if (refund != BalanceMutationResult.SUCCESS) {
      IllegalStateException failure = new IllegalStateException("claim refund result: " + refund);
      warn(diagnostics, "refund", request.ownerId(), failure);
      return new Outcome(Result.REFUND_FAILED, area, price, failure);
    }
    return new Outcome(
        created == RepositoryResult.OVERLAP ? Result.OVERLAP
            : created == RepositoryResult.PERSIST_FAILED ? Result.PERSIST_FAILED
            : Result.STATE_UNKNOWN,
        area, price, null);
  }

  private static void warn(Diagnostics diagnostics, String stage, UUID ownerId, Throwable failure) {
    try {
      diagnostics.warning(stage, ownerId, failure);
    } catch (RuntimeException ignored) {
      // Diagnostics never change the authoritative result.
    }
  }
}
