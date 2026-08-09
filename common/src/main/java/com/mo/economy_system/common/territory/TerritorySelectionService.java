package com.mo.economy_system.common.territory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-thread state machine for the claim-wand point selection flow.
 *
 * <p>The loader owns only the item and notification shell. Dimension checks,
 * point ordering, cancellation, expiry and the overlap decision are shared so
 * a version cannot accidentally accept a selection made in another world.</p>
 */
public final class TerritorySelectionService {
  public static final long DEFAULT_TIMEOUT_TICKS = 20L * 60L;

  public enum Mode {
    CLAIM,
    RESIZE
  }

  public enum Result {
    FIRST_SELECTED,
    SECOND_SELECTED,
    CANCELLED,
    DIMENSION_MISMATCH,
    Y_MISMATCH,
    OVERLAP,
    NO_SESSION,
    EXPIRED,
    STATE_UNKNOWN
  }

  public record Point(int x, int y, int z) {}

  public record Session(
      UUID playerId,
      Mode mode,
      UUID territoryId,
      String dimensionId,
      Optional<Point> first,
      Optional<Point> second,
      long expiresAt) {
    public Session {
      Objects.requireNonNull(playerId, "playerId");
      Objects.requireNonNull(mode, "mode");
      if (mode == Mode.RESIZE && territoryId == null) {
        throw new IllegalArgumentException("resize session needs territoryId");
      }
      if (mode == Mode.CLAIM && territoryId != null) {
        throw new IllegalArgumentException("claim session cannot have territoryId");
      }
      dimensionId = requireDimension(dimensionId);
      first = Objects.requireNonNull(first, "first");
      second = Objects.requireNonNull(second, "second");
      if (expiresAt < 0) throw new IllegalArgumentException("expiresAt");
      if (second.isPresent() && first.isEmpty()) {
        throw new IllegalArgumentException("second point without first point");
      }
    }
  }

  public record SelectionOutcome(Result result, Session session) {
    public SelectionOutcome {
      Objects.requireNonNull(result, "result");
      if ((result == Result.FIRST_SELECTED || result == Result.SECOND_SELECTED)
          && session == null) {
        throw new IllegalArgumentException("selection result needs session");
      }
    }
  }

  @FunctionalInterface
  public interface OverlapChecker {
    boolean overlaps(Point first, Point second, UUID excludedTerritoryId);
  }

  private static final class MutableSession {
    private final UUID playerId;
    private final Mode mode;
    private final UUID territoryId;
    private final String dimensionId;
    private final long expiresAt;
    private Point first;
    private Point second;

    private MutableSession(
        UUID playerId, Mode mode, UUID territoryId, String dimensionId, long expiresAt) {
      this.playerId = playerId;
      this.mode = mode;
      this.territoryId = territoryId;
      this.dimensionId = requireDimension(dimensionId);
      this.expiresAt = expiresAt;
    }

    private Session snapshot() {
      return new Session(
          playerId,
          mode,
          territoryId,
          dimensionId,
          Optional.ofNullable(first),
          Optional.ofNullable(second),
          expiresAt);
    }
  }

  private final long timeoutTicks;
  private final Map<UUID, MutableSession> sessions = new HashMap<>();

  public TerritorySelectionService() {
    this(DEFAULT_TIMEOUT_TICKS);
  }

  public TerritorySelectionService(long timeoutTicks) {
    if (timeoutTicks <= 0) throw new IllegalArgumentException("timeoutTicks");
    this.timeoutTicks = timeoutTicks;
  }

  public synchronized boolean has(UUID playerId, Mode mode, long tick) {
    Objects.requireNonNull(playerId, "playerId");
    if (tick < 0) throw new IllegalArgumentException("tick");
    MutableSession session = sessions.get(playerId);
    if (session == null || session.mode != mode) return false;
    if (session.expiresAt < tick) {
      sessions.remove(playerId);
      return false;
    }
    return true;
  }

  public synchronized SelectionOutcome selectClaim(
      UUID playerId, String dimensionId, Point point, long tick, OverlapChecker overlap) {
    return select(playerId, Mode.CLAIM, null, dimensionId, point, tick, overlap);
  }

  public synchronized boolean startResize(
      UUID playerId, UUID territoryId, String dimensionId, long tick) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(territoryId, "territoryId");
    requireDimension(dimensionId);
    if (tick < 0) throw new IllegalArgumentException("tick");
    sessions.put(
        playerId,
        new MutableSession(
            playerId, Mode.RESIZE, territoryId, dimensionId, expiration(tick)));
    return true;
  }

  public synchronized SelectionOutcome selectResize(
      UUID playerId,
      UUID territoryId,
      String dimensionId,
      Point point,
      long tick,
      OverlapChecker overlap) {
    return select(playerId, Mode.RESIZE, territoryId, dimensionId, point, tick, overlap);
  }

  public synchronized Optional<Session> session(UUID playerId, Mode mode, long tick) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(mode, "mode");
    if (tick < 0) throw new IllegalArgumentException("tick");
    MutableSession value = sessions.get(playerId);
    if (value == null || value.mode != mode) return Optional.empty();
    if (value.expiresAt < tick) {
      sessions.remove(playerId);
      return Optional.empty();
    }
    return Optional.of(value.snapshot());
  }

  public synchronized boolean clear(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return sessions.remove(playerId) != null;
  }

  public synchronized int clearForTerritory(UUID territoryId) {
    return clearForTerritorySessions(territoryId).size();
  }

  /** Clears resize sessions for a territory and returns their pre-clear snapshots. */
  public synchronized List<Session> clearForTerritorySessions(UUID territoryId) {
    Objects.requireNonNull(territoryId, "territoryId");
    List<Session> removed = new ArrayList<>();
    for (var iterator = sessions.entrySet().iterator(); iterator.hasNext(); ) {
      MutableSession value = iterator.next().getValue();
      if (territoryId.equals(value.territoryId)) {
        removed.add(value.snapshot());
        iterator.remove();
      }
    }
    return List.copyOf(removed);
  }

  /** Removes expired sessions and returns immutable snapshots for notifications. */
  public synchronized List<Session> expire(long tick) {
    if (tick < 0) throw new IllegalArgumentException("tick");
    List<Session> expired = new ArrayList<>();
    for (var iterator = sessions.entrySet().iterator(); iterator.hasNext(); ) {
      MutableSession value = iterator.next().getValue();
      if (value.expiresAt < tick) {
        expired.add(value.snapshot());
        iterator.remove();
      }
    }
    return List.copyOf(expired);
  }

  public synchronized void clearAll() {
    sessions.clear();
  }

  private SelectionOutcome select(
      UUID playerId,
      Mode mode,
      UUID territoryId,
      String dimensionId,
      Point point,
      long tick,
      OverlapChecker overlap) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(mode, "mode");
    Objects.requireNonNull(point, "point");
    requireDimension(dimensionId);
    if (tick < 0) throw new IllegalArgumentException("tick");
    Objects.requireNonNull(overlap, "overlap");

    MutableSession session = sessions.get(playerId);
    if (session == null && mode == Mode.CLAIM) {
      session = new MutableSession(
          playerId, Mode.CLAIM, null, dimensionId, expiration(tick));
      sessions.put(playerId, session);
    }
    if (session == null || session.mode != mode
        || (mode == Mode.RESIZE && !Objects.equals(session.territoryId, territoryId))) {
      return new SelectionOutcome(Result.NO_SESSION, null);
    }
    if (session.expiresAt < tick) {
      sessions.remove(playerId);
      return new SelectionOutcome(Result.EXPIRED, null);
    }
    if (!session.dimensionId.equals(dimensionId)) {
      sessions.remove(playerId);
      return new SelectionOutcome(Result.DIMENSION_MISMATCH, null);
    }
    if (session.first == null) {
      session.first = point;
      return new SelectionOutcome(Result.FIRST_SELECTED, session.snapshot());
    }
    if (session.second != null) {
      sessions.remove(playerId);
      return new SelectionOutcome(Result.CANCELLED, null);
    }
    if (session.first.y() != point.y()) {
      session.first = null;
      return new SelectionOutcome(Result.Y_MISMATCH, session.snapshot());
    }
    final boolean overlaps;
    try {
      overlaps = overlap.overlaps(session.first, point, session.territoryId);
    } catch (RuntimeException failure) {
      session.first = null;
      return new SelectionOutcome(Result.STATE_UNKNOWN, session.snapshot());
    }
    if (overlaps) {
      session.first = null;
      return new SelectionOutcome(Result.OVERLAP, session.snapshot());
    }
    session.second = point;
    return new SelectionOutcome(Result.SECOND_SELECTED, session.snapshot());
  }

  private long expiration(long tick) {
    if (tick > Long.MAX_VALUE - timeoutTicks) return Long.MAX_VALUE;
    return tick + timeoutTicks;
  }

  private static String requireDimension(String value) {
    Objects.requireNonNull(value, "dimensionId");
    if (value.isBlank()) throw new IllegalArgumentException("dimensionId");
    return value;
  }
}
