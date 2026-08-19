package com.mo.economy_system.api.account;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Stable public API for EconomySystem's single 梦鱼币 account ledger. */
public interface EconomyAccountApi {
  int MAX_SOURCE_LENGTH = 64;
  int MAX_REASON_LENGTH = 160;
  Pattern SOURCE_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

  int maxBalance();

  int balance(UUID playerId);

  boolean hasAtLeast(UUID playerId, int amount);

  MutationStatus previewCredit(UUID playerId, int amount);

  MutationStatus credit(UUID playerId, int amount, TransactionNote note);

  MutationStatus debit(UUID playerId, int amount, TransactionNote note);

  TransferStatus previewTransfer(UUID senderId, UUID recipientId, int amount);

  /**
   * Atomically transfers currency between two accounts. Both notes must use the same namespaced
   * source so both sides of the ledger remain queryable as one integration transaction.
   */
  TransferStatus transfer(
      UUID senderId,
      UUID recipientId,
      int amount,
      TransactionNote senderNote,
      TransactionNote recipientNote);

  /** Returns newest-first history. Blank {@code sourceFilter} means all sources. */
  LogPage history(UUID playerId, String sourceFilter, int offset, int limit);

  enum MutationStatus {
    SUCCESS,
    INVALID_AMOUNT,
    INSUFFICIENT_FUNDS,
    BALANCE_LIMIT,
    PERSIST_FAILED
  }

  enum TransferStatus {
    SUCCESS,
    INVALID_AMOUNT,
    SAME_ACCOUNT,
    INSUFFICIENT_FUNDS,
    RECIPIENT_BALANCE_LIMIT,
    PERSIST_FAILED,
    TARGET_NOT_AVAILABLE
  }

  /** Namespaced source plus a bounded human-readable reason recorded in EconomySystem's ledger. */
  record TransactionNote(String source, String reason) {
    public TransactionNote {
      source = Objects.requireNonNull(source, "source").trim();
      reason = Objects.requireNonNull(reason, "reason").trim();
      if (source.isEmpty() || source.length() > MAX_SOURCE_LENGTH || !SOURCE_PATTERN.matcher(source).matches()) {
        throw new IllegalArgumentException("source must be a namespaced id such as mymod:quest_reward");
      }
      if (reason.isEmpty() || reason.length() > MAX_REASON_LENGTH) {
        throw new IllegalArgumentException("reason must be 1.." + MAX_REASON_LENGTH + " characters");
      }
    }

    public static TransactionNote of(String source, String reason) {
      return new TransactionNote(source, reason);
    }
  }

  record LogEntry(
      long timeMillis,
      String source,
      String reason,
      int delta,
      int beforeBalance,
      int afterBalance) {
    public LogEntry {
      source = Objects.requireNonNullElse(source, "");
      reason = Objects.requireNonNullElse(reason, "");
    }
  }

  record LogPage(List<LogEntry> entries, String sourceFilter, int offset, int limit, int total) {
    public LogPage {
      entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
      sourceFilter = Objects.requireNonNullElse(sourceFilter, "");
      if (offset < 0 || limit <= 0 || total < 0) throw new IllegalArgumentException("invalid log page bounds");
    }
  }
}
