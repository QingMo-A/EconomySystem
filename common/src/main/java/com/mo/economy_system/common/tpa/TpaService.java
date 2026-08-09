package com.mo.economy_system.common.tpa;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Loader-neutral TPA request and acceptance transaction. */
public final class TpaService {
  public enum SendResult {
    SUCCESS,
    SELF,
    NO_POTION,
    TARGET_BUSY,
    SENDER_BUSY,
    CAPACITY,
    STATE_UNKNOWN
  }

  public enum AcceptResult {
    SUCCESS,
    NO_REQUEST,
    SENDER_OFFLINE,
    SENDER_NO_POTION,
    INVENTORY_FAILED,
    TELEPORT_FAILED,
    TELEPORT_STATE_UNKNOWN,
    ROLLBACK_FAILED
  }

  public enum DenyResult {
    SUCCESS,
    NO_REQUEST
  }

  public record AcceptOutcome(AcceptResult result, TpaRequest request) {
    public AcceptOutcome {
      Objects.requireNonNull(result, "result");
      if ((result != AcceptResult.NO_REQUEST) != (request != null)) {
        throw new IllegalArgumentException("result/request");
      }
    }
  }

  public record DenyOutcome(DenyResult result, TpaRequest request) {
    public DenyOutcome {
      Objects.requireNonNull(result, "result");
      if ((result == DenyResult.SUCCESS) != (request != null)) {
        throw new IllegalArgumentException("result/request");
      }
    }
  }

  @FunctionalInterface
  public interface Diagnostics {
    void warning(String stage, TpaRequest request, int slot, Throwable primary, Throwable secondary);
  }

  private final TpaRequestStore requests;
  private final TpaPort port;
  private final Diagnostics diagnostics;

  public TpaService(TpaRequestStore requests, TpaPort port, Diagnostics diagnostics) {
    this.requests = Objects.requireNonNull(requests, "requests");
    this.port = Objects.requireNonNull(port, "port");
    this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
  }

  public TpaService(TpaRequestStore requests, TpaPort port) {
    this(requests, port, (stage, request, slot, primary, secondary) -> {});
  }

  public synchronized SendResult send(UUID senderId, UUID targetId, long serverTick) {
    Objects.requireNonNull(senderId, "senderId");
    Objects.requireNonNull(targetId, "targetId");
    if (senderId.equals(targetId)) return SendResult.SELF;
    boolean hasPotion;
    try {
      hasPotion = port.hasWormholePotion(senderId);
    } catch (RuntimeException error) {
      warn("send-inventory", null, -1, error, null);
      return SendResult.STATE_UNKNOWN;
    }
    if (!hasPotion) return SendResult.NO_POTION;
    return switch (requests.create(senderId, targetId, serverTick)) {
      case CREATED -> SendResult.SUCCESS;
      case SELF -> SendResult.SELF;
      case TARGET_BUSY -> SendResult.TARGET_BUSY;
      case SENDER_BUSY -> SendResult.SENDER_BUSY;
      case CAPACITY -> SendResult.CAPACITY;
      case INVALID_TICK -> SendResult.STATE_UNKNOWN;
    };
  }

  public synchronized AcceptOutcome accept(UUID targetId, long serverTick) {
    Objects.requireNonNull(targetId, "targetId");
    TpaRequestStore.Claim claim = requests.claim(targetId, serverTick).orElse(null);
    if (claim == null) return new AcceptOutcome(AcceptResult.NO_REQUEST, null);
    TpaRequest request = claim.request();
    try {
      if (!port.isOnline(request.senderId())) {
        return new AcceptOutcome(
            releaseOrUnknown(claim, serverTick, request, "release-offline")
                ? AcceptResult.SENDER_OFFLINE
                : AcceptResult.TELEPORT_STATE_UNKNOWN,
            request);
      }
    } catch (RuntimeException error) {
      warn("online", request, -1, error, null);
      releaseOrUnknown(claim, serverTick, request, "release-online-error");
      return new AcceptOutcome(AcceptResult.TELEPORT_STATE_UNKNOWN, request);
    }

    TpaPort.PotionReservation reservation;
    try {
      reservation = port.reserveWormholePotion(request.senderId()).orElse(null);
    } catch (TpaReservationException error) {
      warn("reserve", request, error.slot(), error, error.getCause());
      boolean released = error.rollbackFailed()
          || releaseOrUnknown(claim, serverTick, request, "release-reserve-error");
      return new AcceptOutcome(
          error.rollbackFailed()
              ? AcceptResult.ROLLBACK_FAILED
              : released ? AcceptResult.INVENTORY_FAILED : AcceptResult.TELEPORT_STATE_UNKNOWN,
          request);
    } catch (RuntimeException error) {
      warn("reserve", request, -1, error, null);
      return new AcceptOutcome(
          releaseOrUnknown(claim, serverTick, request, "release-reserve-runtime")
              ? AcceptResult.INVENTORY_FAILED
              : AcceptResult.TELEPORT_STATE_UNKNOWN,
          request);
    } catch (Exception error) {
      warn("reserve", request, -1, error, null);
      return new AcceptOutcome(
          releaseOrUnknown(claim, serverTick, request, "release-reserve-error")
              ? AcceptResult.INVENTORY_FAILED
              : AcceptResult.TELEPORT_STATE_UNKNOWN,
          request);
    }
    if (reservation == null) {
      return new AcceptOutcome(
          releaseOrUnknown(claim, serverTick, request, "release-no-potion")
              ? AcceptResult.SENDER_NO_POTION
              : AcceptResult.TELEPORT_STATE_UNKNOWN,
          request);
    }

    Throwable teleportError = null;
    try {
      port.teleport(request.senderId(), request.targetId());
    } catch (Throwable error) {
      if (error instanceof Error fatal) throw fatal;
      teleportError = error;
    }

    TpaPort.TeleportArrival arrival;
    try {
      arrival = Objects.requireNonNull(
          port.arrival(request.senderId(), request.targetId()), "arrival");
    } catch (RuntimeException error) {
      safeCommit(reservation, request, "commit-arrival-error");
      warn("arrival", request, reservation.slot(), error, teleportError);
      return new AcceptOutcome(AcceptResult.TELEPORT_STATE_UNKNOWN, request);
    }
    if (arrival == TpaPort.TeleportArrival.UNKNOWN) {
      safeCommit(reservation, request, "commit-arrival-unknown");
      warn(
          "arrival-unknown",
          request,
          reservation.slot(),
          new IllegalStateException("adapter reported UNKNOWN"),
          teleportError);
      return new AcceptOutcome(AcceptResult.TELEPORT_STATE_UNKNOWN, request);
    }
    if (arrival == TpaPort.TeleportArrival.NOT_ARRIVED) {
      Throwable primary = teleportError == null
          ? new IllegalStateException("sender did not arrive")
          : teleportError;
      try {
        reservation.rollback();
      } catch (Exception rollbackError) {
        if (rollbackError != primary) primary.addSuppressed(rollbackError);
        warn("rollback", request, reservation.slot(), primary, rollbackError);
        return new AcceptOutcome(AcceptResult.ROLLBACK_FAILED, request);
      }
      warn("teleport", request, reservation.slot(), primary, null);
      return new AcceptOutcome(AcceptResult.TELEPORT_FAILED, request);
    }

    if (teleportError != null) {
      warn("teleport-arrived", request, reservation.slot(), teleportError, null);
    }
    safeCommit(reservation, request, "commit");
    try {
      port.effects(request.senderId(), request.targetId());
    } catch (Exception error) {
      warn("effects", request, reservation.slot(), error, null);
    }
    return new AcceptOutcome(AcceptResult.SUCCESS, request);
  }

  public synchronized DenyOutcome deny(UUID targetId, long serverTick) {
    Objects.requireNonNull(targetId, "targetId");
    TpaRequestStore.Claim claim = requests.claim(targetId, serverTick).orElse(null);
    return claim == null
        ? new DenyOutcome(DenyResult.NO_REQUEST, null)
        : new DenyOutcome(DenyResult.SUCCESS, claim.request());
  }

  public synchronized List<TpaRequest> expire(long serverTick) {
    return requests.expire(serverTick);
  }

  /** Visible for server lifecycle checks and common contract tests. */
  public synchronized int pendingCount() {
    return requests.size();
  }

  public synchronized void clear() {
    requests.clear();
  }

  private void safeCommit(
      TpaPort.PotionReservation reservation, TpaRequest request, String stage) {
    try {
      reservation.commit();
    } catch (RuntimeException error) {
      warn(stage, request, reservation.slot(), error, null);
    }
  }

  /**
   * Restores a claimed request before returning a retryable acceptance result. A failed restore
   * means the request and the adapter state can no longer be reconciled, so callers must fail
   * closed instead of claiming that the sender can simply retry.
   */
  private boolean releaseOrUnknown(
      TpaRequestStore.Claim claim, long serverTick, TpaRequest request, String stage) {
    try {
      if (requests.release(claim, serverTick)) return true;
      warn(stage, request, -1, new IllegalStateException("request release rejected"), null);
    } catch (RuntimeException error) {
      warn(stage, request, -1, error, null);
    }
    return false;
  }

  private void warn(
      String stage, TpaRequest request, int slot, Throwable primary, Throwable secondary) {
    try {
      diagnostics.warning(stage, request, slot, primary, secondary);
    } catch (RuntimeException ignored) {
      // Diagnostics must never change the transaction outcome.
    }
  }
}
