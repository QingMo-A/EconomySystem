package com.mo.economy_system.common.transfer;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A bounded reservation ledger for transfer temporary files.
 *
 * <p>The ledger is intentionally loader neutral. A client session owns one instance and both
 * outgoing snapshots and incoming part files reserve from that same instance. Reservation and
 * release are serialized under one monitor, so the file count and byte count can never diverge.
 */
public final class CheckedFileTransferTempBudget implements AutoCloseable {
  public enum ResultCode {
    RESERVED,
    TEMP_STORAGE_LIMIT,
    INVALID_BYTES
  }

  /** The outcome of a reservation attempt. */
  public record ReservationResult(ResultCode code, Reservation reservation) {
    public ReservationResult {
      Objects.requireNonNull(code, "code");
      if ((code == ResultCode.RESERVED) != (reservation != null)) {
        throw new IllegalArgumentException("reservation result");
      }
    }

    public boolean success() {
      return code == ResultCode.RESERVED;
    }

    public boolean isTempStorageLimit() {
      return code == ResultCode.TEMP_STORAGE_LIMIT;
    }
  }

  /** A one-shot reservation owned by the budget that created it. */
  public static final class Reservation implements AutoCloseable {
    private final CheckedFileTransferTempBudget owner;
    private final long bytes;
    private final AtomicBoolean released = new AtomicBoolean();

    private Reservation(CheckedFileTransferTempBudget owner, long bytes) {
      this.owner = owner;
      this.bytes = bytes;
    }

    public long bytes() {
      return bytes;
    }

    public boolean isReleased() {
      return released.get();
    }

    /** Releases this reservation once. A second release is a harmless no-op. */
    public boolean release() {
      return owner.release(this);
    }

    @Override
    public void close() {
      release();
    }
  }

  private final int maxFiles;
  private final long maxBytes;
  private final Set<Reservation> reservations =
      Collections.newSetFromMap(new IdentityHashMap<>());
  private long reservedBytes;

  public CheckedFileTransferTempBudget() {
    this(EconomyNetworkLimits.MAX_TRANSFER_TEMP_FILES, EconomyNetworkLimits.MAX_TRANSFER_TEMP_BYTES);
  }

  public CheckedFileTransferTempBudget(int maxFiles, long maxBytes) {
    if (maxFiles < 1) {
      throw new IllegalArgumentException("maxFiles");
    }
    if (maxBytes < 0) {
      throw new IllegalArgumentException("maxBytes");
    }
    this.maxFiles = maxFiles;
    this.maxBytes = maxBytes;
  }

  public int maxFiles() {
    return maxFiles;
  }

  public long maxBytes() {
    return maxBytes;
  }

  public synchronized int reservedFiles() {
    return reservations.size();
  }

  public synchronized long reservedBytes() {
    return reservedBytes;
  }

  public synchronized int availableFiles() {
    return maxFiles - reservations.size();
  }

  public synchronized long availableBytes() {
    return maxBytes - reservedBytes;
  }

  /**
   * Attempts to reserve one temporary-file slot and {@code bytes} bytes.
   *
   * <p>Zero-byte transfers still consume one file slot. Negative sizes and sizes that cannot be
   * represented by the configured budget are rejected without changing either counter.
   */
  public synchronized ReservationResult tryReserve(long bytes) {
    if (bytes < 0) {
      return new ReservationResult(ResultCode.INVALID_BYTES, null);
    }
    if (reservations.size() >= maxFiles
        || bytes > maxBytes
        || bytes > maxBytes - reservedBytes) {
      return new ReservationResult(ResultCode.TEMP_STORAGE_LIMIT, null);
    }
    Reservation reservation = new Reservation(this, bytes);
    reservations.add(reservation);
    reservedBytes += bytes;
    return new ReservationResult(ResultCode.RESERVED, reservation);
  }

  /**
   * Convenience form of {@link #tryReserve(long)}. Returns {@code null} when the reservation is
   * rejected; callers that need the explicit {@code TEMP_STORAGE_LIMIT} code should use
   * {@link #tryReserve(long)}.
   */
  public Reservation reserve(long bytes) {
    return tryReserve(bytes).reservation();
  }

  /** Named alias for integrations that prefer an explicit result over a nullable reservation. */
  public ReservationResult reserveResult(long bytes) {
    return tryReserve(bytes);
  }

  /** Releases a reservation belonging to this budget. */
  public synchronized boolean release(Reservation reservation) {
    if (reservation == null || reservation.owner != this || !reservations.remove(reservation)) {
      return false;
    }
    if (!reservation.released.compareAndSet(false, true)) {
      // Identity-set removal above should make this impossible except if a caller races with
      // clear(). Keep counters safe even in that defensive branch.
      return false;
    }
    reservedBytes -= reservation.bytes;
    return true;
  }

  /**
   * Releases every live reservation. This is used on logout, reconnect, and client stop. It is
   * deliberately idempotent and does not retain references to released reservations.
   */
  public synchronized void clear() {
    for (Reservation reservation : reservations) {
      reservation.released.set(true);
    }
    reservations.clear();
    reservedBytes = 0;
  }

  @Override
  public void close() {
    clear();
  }
}
