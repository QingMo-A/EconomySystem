package com.mo.economy_system.core.economy_system;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Loader-neutral economy state and business rules.
 *
 * <p>Targets supply a dirty callback (for example {@code SavedData#setDirty}) and translate {@link
 * Snapshot snapshots} to their native persistence format. Restoring a snapshot intentionally does
 * not invoke the callback.
 */
public final class EconomyLedger {
  public static final int MAX_BALANCE = Integer.MAX_VALUE;
  public static final int MAX_LOGS_PER_PLAYER = 1000;

  private static final String DEFAULT_CATEGORY = "系统";
  private static final String ALL_CATEGORIES = "全部";

  private final Runnable dirtyCallback;
  private final Map<UUID, Integer> accounts = new HashMap<>();
  private final Map<UUID, List<String>> offlineMessages = new HashMap<>();
  private final Map<UUID, Deque<BalanceLogEntry>> balanceLogs = new HashMap<>();

  public EconomyLedger(Runnable dirtyCallback) {
    this.dirtyCallback = Objects.requireNonNull(dirtyCallback, "dirtyCallback");
  }

  public int getBalance(UUID playerUUID) {
    return accounts.getOrDefault(playerUUID, 0);
  }

  public void setBalance(UUID playerUUID, int amount) {
    setBalance(playerUUID, amount, DEFAULT_CATEGORY, "余额设置");
  }

  public void setBalance(UUID playerUUID, int amount, String category, String reason) {
    int before = getBalance(playerUUID);
    int after = Math.max(0, amount);
    accounts.put(playerUUID, after);
    recordBalanceLog(playerUUID, category, reason, after - before, before, after);
    dirtyCallback.run();
  }

  public boolean addBalance(UUID playerUUID, int amount) {
    return addBalance(playerUUID, amount, DEFAULT_CATEGORY, "余额增加");
  }

  public boolean addBalance(UUID playerUUID, int amount, String category, String reason) {
    if (amount <= 0) {
      return false;
    }
    int balance = getBalance(playerUUID);
    if (balance > MAX_BALANCE - amount) {
      setBalance(playerUUID, MAX_BALANCE, category, reason);
      return true;
    }
    setBalance(playerUUID, balance + amount, category, reason);
    return true;
  }

  public boolean minBalance(UUID playerUUID, int amount) {
    return minBalance(playerUUID, amount, DEFAULT_CATEGORY, "余额减少");
  }

  public boolean minBalance(UUID playerUUID, int amount, String category, String reason) {
    if (amount <= 0) {
      return false;
    }
    int balance = getBalance(playerUUID);
    if (balance < amount) {
      return false;
    }
    setBalance(playerUUID, balance - amount, category, reason);
    return true;
  }

  public boolean hasEnoughBalance(UUID playerUUID, int amount) {
    return amount > 0 && getBalance(playerUUID) >= amount;
  }

  public boolean canCreditExact(UUID playerUUID, int amount) {
    return previewCreditExact(playerUUID, amount) == BalanceMutationResult.SUCCESS;
  }

  public synchronized BalanceMutationResult previewCreditExact(UUID playerUUID, int amount) {
    Objects.requireNonNull(playerUUID, "playerUUID");
    if (amount <= 0) return BalanceMutationResult.INVALID_AMOUNT;
    return (long) getBalance(playerUUID) + amount > MAX_BALANCE
        ? BalanceMutationResult.BALANCE_LIMIT
        : BalanceMutationResult.SUCCESS;
  }

  public BalanceMutationResult creditExact(
      UUID playerUUID, int amount, String category, String reason) {
    return mutateExact(playerUUID, amount, category, reason, true);
  }

  public BalanceMutationResult debitExact(
      UUID playerUUID, int amount, String category, String reason) {
    return mutateExact(playerUUID, amount, category, reason, false);
  }

  private synchronized BalanceMutationResult mutateExact(
      UUID playerUUID, int amount, String category, String reason, boolean credit) {
    Objects.requireNonNull(playerUUID, "playerUUID");
    if (amount <= 0) return BalanceMutationResult.INVALID_AMOUNT;
    boolean accountExisted = accounts.containsKey(playerUUID);
    int before = getBalance(playerUUID);
    if (credit && (long) before + amount > MAX_BALANCE) return BalanceMutationResult.BALANCE_LIMIT;
    if (!credit && before < amount) return BalanceMutationResult.INSUFFICIENT_FUNDS;
    Deque<BalanceLogEntry> previousLogs =
        balanceLogs.containsKey(playerUUID) ? new ArrayDeque<>(balanceLogs.get(playerUUID)) : null;
    int after = credit ? before + amount : before - amount;
    accounts.put(playerUUID, after);
    recordBalanceLog(playerUUID, category, reason, credit ? amount : -amount, before, after);
    try {
      dirtyCallback.run();
    } catch (RuntimeException exception) {
      if (accountExisted) accounts.put(playerUUID, before);
      else accounts.remove(playerUUID);
      if (previousLogs == null) balanceLogs.remove(playerUUID);
      else balanceLogs.put(playerUUID, previousLogs);
      return BalanceMutationResult.PERSIST_FAILED;
    }
    return BalanceMutationResult.SUCCESS;
  }

  /**
   * Moves the complete amount between two accounts or changes neither one. Recipient overflow is
   * rejected so currency can never disappear through the general {@link #addBalance(UUID, int)}
   * saturation behavior.
   */
  public synchronized BalanceTransferResult previewTransferExact(
      UUID senderUUID, UUID recipientUUID, int amount) {
    Objects.requireNonNull(senderUUID, "senderUUID");
    Objects.requireNonNull(recipientUUID, "recipientUUID");
    if (amount <= 0) return BalanceTransferResult.INVALID_AMOUNT;
    if (senderUUID.equals(recipientUUID)) return BalanceTransferResult.SAME_ACCOUNT;
    int senderBefore = getBalance(senderUUID);
    if (senderBefore < amount) return BalanceTransferResult.INSUFFICIENT_FUNDS;
    if ((long) getBalance(recipientUUID) + amount > MAX_BALANCE) {
      return BalanceTransferResult.RECIPIENT_BALANCE_LIMIT;
    }
    return BalanceTransferResult.SUCCESS;
  }

  public synchronized BalanceTransferResult transferExact(
      UUID senderUUID,
      UUID recipientUUID,
      int amount,
      String category,
      String senderReason,
      String recipientReason) {
    BalanceTransferResult preview = previewTransferExact(senderUUID, recipientUUID, amount);
    if (preview != BalanceTransferResult.SUCCESS) return preview;
    boolean senderExisted = accounts.containsKey(senderUUID);
    boolean recipientExisted = accounts.containsKey(recipientUUID);
    int senderBefore = getBalance(senderUUID);
    int recipientBefore = getBalance(recipientUUID);
    Deque<BalanceLogEntry> senderLogs =
        balanceLogs.containsKey(senderUUID) ? new ArrayDeque<>(balanceLogs.get(senderUUID)) : null;
    Deque<BalanceLogEntry> recipientLogs =
        balanceLogs.containsKey(recipientUUID)
            ? new ArrayDeque<>(balanceLogs.get(recipientUUID))
            : null;

    int senderAfter = senderBefore - amount;
    int recipientAfter = recipientBefore + amount;
    accounts.put(senderUUID, senderAfter);
    accounts.put(recipientUUID, recipientAfter);
    recordBalanceLog(senderUUID, category, senderReason, -amount, senderBefore, senderAfter);
    recordBalanceLog(
        recipientUUID, category, recipientReason, amount, recipientBefore, recipientAfter);
    try {
      dirtyCallback.run();
    } catch (RuntimeException exception) {
      restoreAccount(senderUUID, senderExisted, senderBefore, senderLogs);
      restoreAccount(recipientUUID, recipientExisted, recipientBefore, recipientLogs);
      return BalanceTransferResult.PERSIST_FAILED;
    }
    return BalanceTransferResult.SUCCESS;
  }

  /** Compatibility entry point for protocol 4; delegates to the atomic exact transfer. */
  public BalanceTransferResult transferBalance(
      UUID senderUUID,
      UUID recipientUUID,
      int amount,
      String category,
      String senderReason,
      String recipientReason) {
    return transferExact(
        senderUUID, recipientUUID, amount, category, senderReason, recipientReason);
  }

  private void restoreAccount(UUID id, boolean existed, int balance, Deque<BalanceLogEntry> logs) {
    if (existed) accounts.put(id, balance);
    else accounts.remove(id);
    if (logs == null) balanceLogs.remove(id);
    else balanceLogs.put(id, logs);
  }

  public void storeOfflineMessage(UUID playerUUID, String message) {
    offlineMessages.computeIfAbsent(playerUUID, key -> new ArrayList<>()).add(message);
    dirtyCallback.run();
  }

  public List<String> getOfflineMessages(UUID playerUUID) {
    List<String> messages = offlineMessages.remove(playerUUID);
    if (messages == null) {
      return new ArrayList<>();
    }
    dirtyCallback.run();
    return messages;
  }

  /** Returns newest-first balance history for the requested player. */
  public List<BalanceLogEntry> getBalanceLogs(UUID playerUUID) {
    Deque<BalanceLogEntry> logs = balanceLogs.get(playerUUID);
    return logs == null ? new ArrayList<>() : new ArrayList<>(logs);
  }

  public BalanceLogPage getBalanceLogs(UUID playerUUID, String category, int offset, int limit) {
    String normalizedCategory = category == null ? ALL_CATEGORIES : category;
    int safeOffset = Math.max(0, offset);
    int safeLimit = Math.max(1, Math.min(100, limit));
    List<BalanceLogEntry> filtered =
        getBalanceLogs(playerUUID).stream()
            .filter(
                entry ->
                    ALL_CATEGORIES.equals(normalizedCategory)
                        || normalizedCategory.equals(entry.category()))
            .toList();
    int total = filtered.size();
    int fromIndex = Math.min(safeOffset, total);
    int toIndex = Math.min(fromIndex + safeLimit, total);
    return new BalanceLogPage(
        filtered.subList(fromIndex, toIndex), normalizedCategory, safeOffset, safeLimit, total);
  }

  /** Returns balance-sorted immutable account entries. */
  public List<Map.Entry<UUID, Integer>> getAllAccounts() {
    return accounts.entrySet().stream()
        .sorted((left, right) -> right.getValue().compareTo(left.getValue()))
        .<Map.Entry<UUID, Integer>>map(
            entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()))
        .toList();
  }

  /** Returns immutable account entries without imposing an order. */
  public List<Map.Entry<UUID, Integer>> getAllPlayers() {
    return accounts.entrySet().stream()
        .<Map.Entry<UUID, Integer>>map(
            entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()))
        .toList();
  }

  /** Creates a deeply immutable state copy suitable for a target codec. */
  public Snapshot snapshot() {
    Map<UUID, Integer> accountSnapshot = Collections.unmodifiableMap(new LinkedHashMap<>(accounts));

    Map<UUID, List<String>> messageSnapshot = new LinkedHashMap<>();
    offlineMessages.forEach((uuid, messages) -> messageSnapshot.put(uuid, List.copyOf(messages)));

    Map<UUID, List<BalanceLogEntry>> logSnapshot = new LinkedHashMap<>();
    balanceLogs.forEach((uuid, logs) -> logSnapshot.put(uuid, List.copyOf(logs)));

    return new Snapshot(accountSnapshot, messageSnapshot, logSnapshot);
  }

  /** Replaces all state from an immutable target-decoded snapshot without marking it dirty. */
  public void restore(Snapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot");
    accounts.clear();
    offlineMessages.clear();
    balanceLogs.clear();

    accounts.putAll(snapshot.accounts());
    snapshot
        .offlineMessages()
        .forEach((uuid, messages) -> offlineMessages.put(uuid, new ArrayList<>(messages)));
    snapshot
        .balanceLogs()
        .forEach(
            (uuid, logs) -> {
              Deque<BalanceLogEntry> restoredLogs = new ArrayDeque<>();
              int count = 0;
              for (BalanceLogEntry entry : logs) {
                if (count++ >= MAX_LOGS_PER_PLAYER) {
                  break;
                }
                restoredLogs.addLast(entry);
              }
              balanceLogs.put(uuid, restoredLogs);
            });
  }

  private void recordBalanceLog(
      UUID playerUUID, String category, String reason, int delta, int before, int after) {
    if (delta == 0) {
      return;
    }
    Deque<BalanceLogEntry> logs =
        balanceLogs.computeIfAbsent(playerUUID, key -> new ArrayDeque<>());
    logs.addFirst(
        new BalanceLogEntry(System.currentTimeMillis(), category, reason, delta, before, after));
    while (logs.size() > MAX_LOGS_PER_PLAYER) {
      logs.removeLast();
    }
  }

  /** Deeply immutable representation of all ledger state. */
  public record Snapshot(
      Map<UUID, Integer> accounts,
      Map<UUID, List<String>> offlineMessages,
      Map<UUID, List<BalanceLogEntry>> balanceLogs) {
    public Snapshot {
      accounts = immutableMap(accounts);
      offlineMessages = immutableListMap(offlineMessages);
      balanceLogs = immutableListMap(balanceLogs);
    }

    private static <V> Map<UUID, V> immutableMap(Map<UUID, V> source) {
      Objects.requireNonNull(source, "source");
      return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static <V> Map<UUID, List<V>> immutableListMap(Map<UUID, ? extends List<V>> source) {
      Objects.requireNonNull(source, "source");
      Map<UUID, List<V>> copy = new LinkedHashMap<>();
      source.forEach((uuid, values) -> copy.put(uuid, List.copyOf(values)));
      return Collections.unmodifiableMap(copy);
    }
  }
}
