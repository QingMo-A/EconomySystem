package com.mo.economy_system.common.redpacket;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Loader-neutral red-packet state machine.
 *
 * <p>The account port must provide exact, server-authoritative mutations. State is committed only
 * after the corresponding account mutation succeeds; persistence failures are compensated where
 * the adapter can prove the reverse mutation succeeded.
 */
public final class RedPacketService {
  private static final String CATEGORY = "红包";
  private static final String CREATE_REASON = "创建红包";
  private static final String CLAIM_REASON = "领取红包";
  private static final String CANCEL_REASON = "取消红包退款";
  private static final String EXPIRE_REASON = "红包过期退款";

  public enum CreateResult {
    SUCCESS,
    INVALID_AMOUNT,
    INVALID_DURATION,
    INVALID_PLAYER_COUNT,
    ALREADY_ACTIVE,
    INSUFFICIENT_FUNDS,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  public enum ClaimResult {
    SUCCESS,
    NO_AVAILABLE,
    ALREADY_CLAIMED,
    BALANCE_LIMIT,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  public enum CancelResult {
    SUCCESS,
    NO_ACTIVE,
    BALANCE_LIMIT,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  public enum ExpireResult {
    REFUNDED,
    NO_REFUND,
    BALANCE_LIMIT,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  public record CreateOutcome(CreateResult result, RedPacket packet) {
    public CreateOutcome {
      Objects.requireNonNull(result, "result");
      if ((result == CreateResult.SUCCESS) != (packet != null)) {
        throw new IllegalArgumentException("result/packet");
      }
    }
  }

  public record ClaimOutcome(ClaimResult result, RedPacket packet, int amount, boolean completed) {
    public ClaimOutcome {
      Objects.requireNonNull(result, "result");
      if (amount < 0) throw new IllegalArgumentException("amount");
      if (result == ClaimResult.SUCCESS && (packet == null || amount <= 0)) {
        throw new IllegalArgumentException("success outcome");
      }
      if (result != ClaimResult.SUCCESS && (packet != null || amount != 0 || completed)) {
        throw new IllegalArgumentException("failure outcome");
      }
    }
  }

  public record CancelOutcome(CancelResult result, int refundedAmount) {
    public CancelOutcome {
      Objects.requireNonNull(result, "result");
      if (refundedAmount < 0) throw new IllegalArgumentException("refundedAmount");
      if (result != CancelResult.SUCCESS && refundedAmount != 0) {
        throw new IllegalArgumentException("failure refund");
      }
    }
  }

  public record ExpireOutcome(ExpireResult result, RedPacket packet, int refundedAmount) {
    public ExpireOutcome {
      Objects.requireNonNull(result, "result");
      Objects.requireNonNull(packet, "packet");
      if (refundedAmount < 0) throw new IllegalArgumentException("refundedAmount");
      if ((result == ExpireResult.REFUNDED || result == ExpireResult.NO_REFUND)
          && refundedAmount != packet.remainingAmount()) {
        throw new IllegalArgumentException("refund/packet");
      }
    }
  }

  @FunctionalInterface
  public interface IntRandom {
    int nextInt(int bound);
  }

  public interface Diagnostics {
    void warning(String operation, UUID senderId, UUID playerId, Throwable error);
  }

  private final RedPacketAccountPort accounts;
  private final RedPacketRepository repository;
  private final LongSupplier clock;
  private final IntRandom random;
  private final Diagnostics diagnostics;
  private final Map<UUID, RedPacket> packets = new LinkedHashMap<>();

  public RedPacketService(
      RedPacketAccountPort accounts,
      RedPacketRepository repository,
      LongSupplier clock,
      IntRandom random,
      Diagnostics diagnostics) {
    this.accounts = Objects.requireNonNull(accounts, "accounts");
    this.repository = Objects.requireNonNull(repository, "repository");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.random = Objects.requireNonNull(random, "random");
    this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    load();
  }

  public RedPacketService(
      RedPacketAccountPort accounts, RedPacketRepository repository, LongSupplier clock, IntRandom random) {
    this(accounts, repository, clock, random, (operation, sender, player, error) -> {});
  }

  public RedPacketService(RedPacketAccountPort accounts) {
    this(accounts, RedPacketRepository.empty(), System::currentTimeMillis, bound -> new java.util.Random().nextInt(bound));
  }

  public synchronized CreateOutcome create(
      UUID senderId,
      String senderName,
      int amount,
      long durationMinutes,
      RedPacket.Mode mode,
      int participantCount) {
    Objects.requireNonNull(senderId, "senderId");
    Objects.requireNonNull(mode, "mode");
    if (amount <= 0) return new CreateOutcome(CreateResult.INVALID_AMOUNT, null);
    if (durationMinutes <= 0) return new CreateOutcome(CreateResult.INVALID_DURATION, null);
    if (participantCount <= 0) return new CreateOutcome(CreateResult.INVALID_PLAYER_COUNT, null);
    if (packets.containsKey(senderId)) return new CreateOutcome(CreateResult.ALREADY_ACTIVE, null);

    long now = now();
    long durationMillis;
    long expiration;
    try {
      durationMillis = Math.multiplyExact(durationMinutes, 60_000L);
      expiration = Math.addExact(now, durationMillis);
    } catch (ArithmeticException error) {
      warn("create-time", senderId, null, error);
      return new CreateOutcome(CreateResult.INVALID_DURATION, null);
    }

    MutationAttempt debit = mutateDebit(senderId, amount, CREATE_REASON);
    CreateResult debitResult = mapCreateDebit(debit);
    if (debitResult != CreateResult.SUCCESS) return new CreateOutcome(debitResult, null);

    RedPacket packet;
    try {
      packet = new RedPacket(
          senderId,
          senderName,
          amount,
          participantCount,
          0,
          mode,
          now,
          expiration,
          Set.of());
      packets.put(senderId, packet);
      persist();
    } catch (RuntimeException error) {
      packets.remove(senderId);
      return compensateCreate(senderId, amount, error);
    }
    return new CreateOutcome(CreateResult.SUCCESS, packet);
  }

  public synchronized Optional<RedPacket> find(UUID senderId) {
    Objects.requireNonNull(senderId, "senderId");
    return Optional.ofNullable(packets.get(senderId));
  }

  public synchronized Optional<RedPacket> recentClaimable() {
    long now = now();
    return packets.values().stream()
        .filter(packet -> packet.isClaimable(now))
        .max(Comparator.comparingLong(RedPacket::createdAtMillis));
  }

  public synchronized ClaimOutcome claim(UUID playerId, UUID requestedSenderId) {
    Objects.requireNonNull(playerId, "playerId");
    RedPacket packet = requestedSenderId == null
        ? recentClaimable().orElse(null)
        : packets.get(requestedSenderId);
    long now = now();
    if (packet == null || !packet.isClaimable(now)) return claimFailure(ClaimResult.NO_AVAILABLE);
    if (packet.hasClaimed(playerId)) return claimFailure(ClaimResult.ALREADY_CLAIMED);

    int remainingPlayers = packet.remainingPlayers();
    int remainingAmount = packet.remainingAmount();
    if (remainingPlayers <= 0 || remainingAmount <= 0) {
      return claimFailure(ClaimResult.NO_AVAILABLE);
    }
    int amount;
    try {
      amount = allocate(packet, remainingAmount, remainingPlayers);
    } catch (RuntimeException error) {
      warn("claim-allocation", packet.senderId(), playerId, error);
      return claimFailure(ClaimResult.STATE_UNKNOWN);
    }
    MutationAttempt credit = mutateCredit(playerId, amount, CLAIM_REASON);
    ClaimResult creditResult = mapClaimCredit(credit);
    if (creditResult != ClaimResult.SUCCESS) return claimFailure(creditResult);

    RedPacket updated;
    try {
      updated = packet.claimedBy(playerId, amount);
      boolean completed = updated.remainingAmount() == 0;
      if (completed) packets.remove(packet.senderId());
      else packets.put(packet.senderId(), updated);
      persist();
      return new ClaimOutcome(ClaimResult.SUCCESS, updated, amount, completed);
    } catch (RuntimeException error) {
      packets.put(packet.senderId(), packet);
      return compensateClaim(playerId, amount, packet, error);
    }
  }

  public synchronized CancelOutcome cancel(UUID senderId) {
    Objects.requireNonNull(senderId, "senderId");
    RedPacket packet = packets.get(senderId);
    if (packet == null) return new CancelOutcome(CancelResult.NO_ACTIVE, 0);
    int remaining = packet.remainingAmount();
    if (remaining == 0) {
      packets.remove(senderId);
      try {
        persist();
      } catch (RuntimeException error) {
        packets.put(senderId, packet);
        return new CancelOutcome(CancelResult.STATE_UNKNOWN, 0);
      }
      return new CancelOutcome(CancelResult.SUCCESS, 0);
    }
    MutationAttempt credit = mutateCredit(senderId, remaining, CANCEL_REASON);
    CancelResult creditResult = mapCancelCredit(credit);
    if (creditResult != CancelResult.SUCCESS) return new CancelOutcome(creditResult, 0);
    packets.remove(senderId);
    try {
      persist();
      return new CancelOutcome(CancelResult.SUCCESS, remaining);
    } catch (RuntimeException error) {
      packets.put(senderId, packet);
      MutationAttempt rollback = mutateDebit(senderId, remaining, "红包取消回滚");
      if (!rollback.succeeded()) {
        warn("cancel-rollback", senderId, null, error);
        return new CancelOutcome(CancelResult.STATE_UNKNOWN, 0);
      }
      return new CancelOutcome(CancelResult.PERSIST_FAILED, 0);
    }
  }

  public synchronized List<ExpireOutcome> expire() {
    long now = now();
    List<ExpireOutcome> outcomes = new ArrayList<>();
    for (RedPacket packet : new ArrayList<>(packets.values())) {
      if (!packet.isExpired(now)) continue;
      outcomes.add(expireOne(packet));
    }
    return List.copyOf(outcomes);
  }

  /** Refunds all remaining escrow during an orderly server shutdown. */
  public synchronized List<ExpireOutcome> refundAll() {
    List<ExpireOutcome> outcomes = new ArrayList<>();
    for (RedPacket packet : new ArrayList<>(packets.values())) {
      outcomes.add(expireOne(packet));
    }
    return List.copyOf(outcomes);
  }

  public synchronized List<RedPacket> snapshot() {
    return List.copyOf(packets.values());
  }

  private void load() {
    List<RedPacket> loaded;
    try {
      loaded = Objects.requireNonNull(repository.load(), "repository.load");
    } catch (RuntimeException error) {
      warn("load", null, null, error);
      return;
    }
    for (RedPacket packet : loaded) {
      if (packet == null || packets.putIfAbsent(packet.senderId(), packet) != null) {
        warn("load-validation", packet == null ? null : packet.senderId(), null,
            new IllegalArgumentException("duplicate or null packet"));
      }
    }
  }

  private void persist() {
    repository.save(List.copyOf(packets.values()));
  }

  private ExpireOutcome expireOne(RedPacket packet) {
    int remaining = packet.remainingAmount();
    if (remaining == 0) {
      packets.remove(packet.senderId());
      try {
        persist();
        return new ExpireOutcome(ExpireResult.NO_REFUND, packet, 0);
      } catch (RuntimeException error) {
        packets.put(packet.senderId(), packet);
        warn("expire-persist", packet.senderId(), null, error);
        return new ExpireOutcome(ExpireResult.STATE_UNKNOWN, packet, 0);
      }
    }
    MutationAttempt credit = mutateCredit(packet.senderId(), remaining, EXPIRE_REASON);
    ExpireResult creditResult = mapExpireCredit(credit);
    if (creditResult != ExpireResult.REFUNDED) {
      return new ExpireOutcome(creditResult, packet, 0);
    }
    packets.remove(packet.senderId());
    try {
      persist();
      return new ExpireOutcome(ExpireResult.REFUNDED, packet, remaining);
    } catch (RuntimeException error) {
      packets.put(packet.senderId(), packet);
      MutationAttempt rollback = mutateDebit(packet.senderId(), remaining, "红包过期退款回滚");
      if (!rollback.succeeded()) {
        warn("expire-rollback", packet.senderId(), null, error);
        return new ExpireOutcome(ExpireResult.STATE_UNKNOWN, packet, 0);
      }
      return new ExpireOutcome(ExpireResult.PERSIST_FAILED, packet, 0);
    }
  }

  private int allocate(RedPacket packet, int remainingAmount, int remainingPlayers) {
    if (remainingPlayers == 1) return remainingAmount;
    if (!packet.isLucky()) return Math.max(1, remainingAmount / remainingPlayers);
    int maximum = Math.max(1, remainingAmount - (remainingPlayers - 1));
    int randomValue = random.nextInt(maximum);
    if (randomValue < 0 || randomValue >= maximum) {
      throw new IllegalStateException("random source returned an invalid value");
    }
    return randomValue + 1;
  }

  private CreateOutcome compensateCreate(UUID senderId, int amount, RuntimeException error) {
    warn("create-persist", senderId, null, error);
    MutationAttempt rollback = mutateCredit(senderId, amount, "红包创建回滚");
    return new CreateOutcome(
        rollback.succeeded() ? CreateResult.PERSIST_FAILED : CreateResult.STATE_UNKNOWN,
        null);
  }

  private ClaimOutcome compensateClaim(UUID playerId, int amount, RedPacket packet, RuntimeException error) {
    warn("claim-persist", packet.senderId(), playerId, error);
    MutationAttempt rollback = mutateDebit(playerId, amount, "红包领取回滚");
    return new ClaimOutcome(
        rollback.succeeded() ? ClaimResult.PERSIST_FAILED : ClaimResult.STATE_UNKNOWN,
        null,
        0,
        false);
  }

  private MutationAttempt mutateDebit(UUID playerId, int amount, String reason) {
    try {
      return new MutationAttempt(
          Objects.requireNonNull(accounts.debit(playerId, amount, CATEGORY, reason), "debit result"),
          null);
    } catch (RuntimeException error) {
      warn("debit", null, playerId, error);
      return new MutationAttempt(null, error);
    }
  }

  private MutationAttempt mutateCredit(UUID playerId, int amount, String reason) {
    try {
      return new MutationAttempt(
          Objects.requireNonNull(accounts.credit(playerId, amount, CATEGORY, reason), "credit result"),
          null);
    } catch (RuntimeException error) {
      warn("credit", null, playerId, error);
      return new MutationAttempt(null, error);
    }
  }

  private CreateResult mapCreateDebit(MutationAttempt attempt) {
    if (attempt.unknown()) return CreateResult.STATE_UNKNOWN;
    return switch (attempt.result()) {
      case SUCCESS -> CreateResult.SUCCESS;
      case INSUFFICIENT_FUNDS -> CreateResult.INSUFFICIENT_FUNDS;
      case INVALID_AMOUNT -> CreateResult.INVALID_AMOUNT;
      case PERSIST_FAILED -> CreateResult.PERSIST_FAILED;
      case BALANCE_LIMIT -> CreateResult.STATE_UNKNOWN;
    };
  }

  private ClaimResult mapClaimCredit(MutationAttempt attempt) {
    if (attempt.unknown()) return ClaimResult.STATE_UNKNOWN;
    return switch (attempt.result()) {
      case SUCCESS -> ClaimResult.SUCCESS;
      case BALANCE_LIMIT -> ClaimResult.BALANCE_LIMIT;
      case PERSIST_FAILED -> ClaimResult.PERSIST_FAILED;
      case INVALID_AMOUNT, INSUFFICIENT_FUNDS -> ClaimResult.STATE_UNKNOWN;
    };
  }

  private CancelResult mapCancelCredit(MutationAttempt attempt) {
    if (attempt.unknown()) return CancelResult.STATE_UNKNOWN;
    return switch (attempt.result()) {
      case SUCCESS -> CancelResult.SUCCESS;
      case BALANCE_LIMIT -> CancelResult.BALANCE_LIMIT;
      case PERSIST_FAILED -> CancelResult.PERSIST_FAILED;
      case INVALID_AMOUNT, INSUFFICIENT_FUNDS -> CancelResult.STATE_UNKNOWN;
    };
  }

  private ExpireResult mapExpireCredit(MutationAttempt attempt) {
    if (attempt.unknown()) return ExpireResult.STATE_UNKNOWN;
    return switch (attempt.result()) {
      case SUCCESS -> ExpireResult.REFUNDED;
      case BALANCE_LIMIT -> ExpireResult.BALANCE_LIMIT;
      case PERSIST_FAILED -> ExpireResult.PERSIST_FAILED;
      case INVALID_AMOUNT, INSUFFICIENT_FUNDS -> ExpireResult.STATE_UNKNOWN;
    };
  }

  private ClaimOutcome claimFailure(ClaimResult result) {
    return new ClaimOutcome(result, null, 0, false);
  }

  private record MutationAttempt(BalanceMutationResult result, RuntimeException error) {
    boolean unknown() {
      return result == null;
    }

    boolean succeeded() {
      return result == BalanceMutationResult.SUCCESS;
    }
  }

  private long now() {
    try {
      return clock.getAsLong();
    } catch (RuntimeException error) {
      warn("clock", null, null, error);
      return System.currentTimeMillis();
    }
  }

  private void warn(String operation, UUID senderId, UUID playerId, Throwable error) {
    try {
      diagnostics.warning(operation, senderId, playerId, error);
    } catch (RuntimeException ignored) {
      // Diagnostics must never change the authoritative result.
    }
  }
}
