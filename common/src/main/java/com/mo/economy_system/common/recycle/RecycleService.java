package com.mo.economy_system.common.recycle;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.platform.item.ItemStackSnapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Authoritative recycling transaction service. Targets provide the inventory and economy ports;
 * this class owns quote selection, cycle quotas and duplicate-submission protection.
 */
public final class RecycleService {
  /** Durable cycle/quota/idempotency state supplied by a target adapter. */
  public interface StateRepository {
    State load();

    void save(State state);
  }

  public record State(long cycleNumber, Map<String, Integer> highRemaining,
                      Set<UUID> completedSubmissions) {
    public State {
      highRemaining = Map.copyOf(Objects.requireNonNull(highRemaining, "highRemaining"));
      completedSubmissions = Set.copyOf(Objects.requireNonNull(completedSubmissions,
          "completedSubmissions"));
      for (Map.Entry<String, Integer> entry : highRemaining.entrySet()) {
        if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null
            || entry.getValue() < 0) throw new IllegalArgumentException("invalid recycle quota state");
      }
      for (UUID id : completedSubmissions) Objects.requireNonNull(id, "completed submission");
    }
  }

  public interface InventoryPort {
    int count(UUID playerId, ItemStackSnapshot item);

    /** Removes exactly {@code amount}, returning false without mutation when it cannot. */
    boolean remove(UUID playerId, ItemStackSnapshot item, int amount);

    /** Restores a previously removed amount during transaction rollback. */
    void restore(UUID playerId, ItemStackSnapshot item, int amount);
  }

  public interface EconomyPort {
    BalanceMutationResult creditExact(UUID playerId, int amount, String category, String reason);
  }

  private final RecycleConfig config;
  private final InventoryPort inventory;
  private final EconomyPort economy;
  private final StateRepository stateRepository;
  private final Map<String, Integer> highRemaining = new HashMap<>();
  private final Set<UUID> completedSubmissions = new HashSet<>();
  private long cycleNumber = Long.MIN_VALUE;

  public RecycleService(RecycleConfig config, InventoryPort inventory, EconomyPort economy) {
    this(config, inventory, economy, new StateRepository() {
      @Override public State load() { return null; }
      @Override public void save(State state) { }
    });
  }

  public RecycleService(RecycleConfig config, InventoryPort inventory, EconomyPort economy,
                        StateRepository stateRepository) {
    this.config = Objects.requireNonNull(config, "config");
    this.inventory = Objects.requireNonNull(inventory, "inventory");
    this.economy = Objects.requireNonNull(economy, "economy");
    this.stateRepository = Objects.requireNonNull(stateRepository, "stateRepository");
    State persisted = stateRepository.load();
    if (persisted == null || persisted.cycleNumber() == Long.MIN_VALUE) {
      resetCycle(0L);
    } else {
      cycleNumber = persisted.cycleNumber();
      completedSubmissions.addAll(persisted.completedSubmissions());
      for (RecycleOffer offer : config.offers()) {
        int saved = persisted.highRemaining().getOrDefault(offer.itemId(), offer.highQuota());
        highRemaining.put(offer.itemId(), Math.min(Math.max(0, saved), offer.highQuota()));
      }
    }
  }

  public synchronized RecycleResult recycle(
      UUID playerId, ItemStackSnapshot item, int requestedAmount, UUID submissionId, long nowMillis) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(item, "item");
    Objects.requireNonNull(submissionId, "submissionId");
    if (requestedAmount <= 0) return failure(RecycleResult.Status.INVALID_AMOUNT);
    if (completedSubmissions.contains(submissionId)) return failure(RecycleResult.Status.DUPLICATE_SUBMISSION);
    rollover(nowMillis);

    RecycleOffer offer = config.byItemId().get(item.itemId());
    if (offer == null) return failure(RecycleResult.Status.UNKNOWN_ITEM);
    int available = Math.max(0, inventory.count(playerId, item));
    if (available < requestedAmount) return failure(RecycleResult.Status.INSUFFICIENT_ITEMS);

    int high = highRemaining.getOrDefault(item.itemId(), 0);
    boolean useHigh = offer.hasHighPrice() && high > 0;
    if (!useHigh && offer.hasHighPrice() && !offer.fallbackToBaseWhenHighQuotaExhausted()) {
      return new RecycleResult(RecycleResult.Status.HIGH_QUOTA_EXHAUSTED, 0, 0, 0, 0);
    }
    int accepted = requestedAmount;
    int highAccepted = useHigh ? Math.min(requestedAmount, high) : 0;
    if (useHigh && high < requestedAmount && !offer.fallbackToBaseWhenHighQuotaExhausted()) {
      // A stop-policy quote accepts only the remaining high-demand quota; it never silently
      // downgrades the excess to the normal price.
      accepted = high;
    }
    int unitPrice = useHigh ? offer.highUnitPrice() : offer.baseUnitPrice();
    long payoutLong = (long) highAccepted * offer.highUnitPrice()
        + (long) (accepted - highAccepted) * offer.baseUnitPrice();
    if (payoutLong > Integer.MAX_VALUE) return failure(RecycleResult.Status.BALANCE_LIMIT);
    int payout = (int) payoutLong;
    if (accepted <= 0) return new RecycleResult(RecycleResult.Status.HIGH_QUOTA_EXHAUSTED, 0, 0, 0, high);
    if (!inventory.remove(playerId, item, accepted)) {
      return failure(RecycleResult.Status.INSUFFICIENT_ITEMS);
    }
    BalanceMutationResult credited = economy.creditExact(playerId, payout, "回收站", "回收 " + item.itemId());
    if (credited != BalanceMutationResult.SUCCESS) {
      inventory.restore(playerId, item, accepted);
      return new RecycleResult(mapBalanceStatus(credited), 0, unitPrice, 0,
          highRemaining.getOrDefault(item.itemId(), 0));
    }
    if (useHigh) highRemaining.put(item.itemId(), high - highAccepted);
    completedSubmissions.add(submissionId);
    persistState();
    return new RecycleResult(RecycleResult.Status.SUCCESS, accepted, unitPrice, payout,
        highRemaining.getOrDefault(item.itemId(), 0));
  }

  public synchronized Map<String, Integer> highQuotaRemaining(long nowMillis) {
    rollover(nowMillis);
    return Map.copyOf(highRemaining);
  }

  public RecycleConfig config() { return config; }

  private void rollover(long nowMillis) {
    long duration = config.cycle().toMillis();
    long current = Math.floorDiv(nowMillis, duration);
    if (current != cycleNumber) resetCycle(current);
  }

  private void resetCycle(long number) {
    cycleNumber = number;
    highRemaining.clear();
    config.offers().forEach(offer -> highRemaining.put(offer.itemId(), offer.highQuota()));
    completedSubmissions.clear();
    persistState();
  }

  private void persistState() {
    stateRepository.save(new State(cycleNumber, highRemaining, completedSubmissions));
  }

  private static RecycleResult failure(RecycleResult.Status status) {
    return new RecycleResult(status, 0, 0, 0, 0);
  }

  private static RecycleResult.Status mapBalanceStatus(BalanceMutationResult result) {
    return switch (result) {
      case BALANCE_LIMIT -> RecycleResult.Status.BALANCE_LIMIT;
      case PERSIST_FAILED -> RecycleResult.Status.PERSIST_FAILED;
      default -> RecycleResult.Status.PERSIST_FAILED;
    };
  }
}
