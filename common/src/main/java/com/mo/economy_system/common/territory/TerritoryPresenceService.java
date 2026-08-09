package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server-thread player/territory presence state machine shared by every target. */
public final class TerritoryPresenceService {
  private final Map<UUID, PlayerState> players = new HashMap<>();

  public synchronized TickOutcome tick(
      UUID playerId, long tick, Location location, Lookup lookup) {
    Objects.requireNonNull(playerId, "playerId");
    if (tick < 0) throw new IllegalArgumentException("tick");
    Objects.requireNonNull(location, "location");
    Objects.requireNonNull(lookup, "lookup");

    PlayerState state = players.computeIfAbsent(playerId, ignored -> new PlayerState());
    if (state.lastObservedTick >= 0 && tick < state.lastObservedTick) state.resetForClock();
    state.lastObservedTick = tick;

    Owned exited = null;
    Owned entered = null;
    boolean lookupFailed = false;
    Optional<Owned> resolved = null;

    if (elapsed(state.lastPositionCheckTick, tick,
        TerritoryRuntimePolicy.MOVEMENT_CHECK_INTERVAL_TICKS)) {
      state.lastPositionCheckTick = tick;
      if (!location.equals(state.location)) {
        LookupResult result = resolve(lookup, location);
        lookupFailed = result.failed();
        if (!result.failed()) {
          resolved = result.territory();
          Transition transition = updateCurrent(state, resolved);
          exited = transition.exited();
          entered = transition.entered();
          state.location = location;
        }
      }
    }

    boolean applyBuffs = false;
    if (state.current != null
        && elapsed(state.lastBuffApplyTick, tick,
            TerritoryRuntimePolicy.BUFF_REAPPLY_INTERVAL_TICKS)) {
      if (resolved == null) {
        LookupResult result = resolve(lookup, location);
        lookupFailed |= result.failed();
        if (!result.failed()) {
          resolved = result.territory();
          Transition transition = updateCurrent(state, resolved);
          if (exited == null) exited = transition.exited();
          if (entered == null) entered = transition.entered();
          state.location = location;
        }
      }
      if (!lookupFailed && state.current != null) {
        state.lastBuffApplyTick = tick;
        applyBuffs = true;
      }
    }

    return new TickOutcome(
        Optional.ofNullable(exited),
        Optional.ofNullable(entered),
        Optional.ofNullable(state.current),
        applyBuffs,
        lookupFailed);
  }

  public synchronized boolean clear(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return players.remove(playerId) != null;
  }

  public synchronized void clearAll() {
    players.clear();
  }

  public synchronized int trackedPlayers() {
    return players.size();
  }

  private static LookupResult resolve(Lookup lookup, Location location) {
    try {
      Optional<Owned> territory = lookup.find(location);
      return territory == null
          ? new LookupResult(Optional.empty(), true)
          : new LookupResult(territory, false);
    } catch (RuntimeException failure) {
      return new LookupResult(Optional.empty(), true);
    }
  }

  private static Transition updateCurrent(PlayerState state, Optional<Owned> resolved) {
    Owned previous = state.current;
    Owned current = resolved.orElse(null);
    UUID previousId = id(previous);
    UUID currentId = id(current);
    state.current = current;
    return Objects.equals(previousId, currentId)
        ? Transition.NONE
        : new Transition(previous, current);
  }

  private static UUID id(Owned territory) {
    return territory == null ? null : territory.summary().territoryId();
  }

  private static boolean elapsed(long previous, long current, long interval) {
    return previous < 0 || current < previous || current - previous >= interval;
  }

  public record Location(String dimensionId, int x, int z) {
    public Location {
      Objects.requireNonNull(dimensionId, "dimensionId");
      if (dimensionId.isBlank()) throw new IllegalArgumentException("dimensionId");
    }
  }

  public record TickOutcome(
      Optional<Owned> exited,
      Optional<Owned> entered,
      Optional<Owned> current,
      boolean applyBuffs,
      boolean lookupFailed) {
    public TickOutcome {
      exited = Objects.requireNonNull(exited, "exited");
      entered = Objects.requireNonNull(entered, "entered");
      current = Objects.requireNonNull(current, "current");
      if (applyBuffs && current.isEmpty()) {
        throw new IllegalArgumentException("buff application requires a current territory");
      }
    }
  }

  @FunctionalInterface
  public interface Lookup {
    Optional<Owned> find(Location location);
  }

  private static final class PlayerState {
    private long lastObservedTick = -1L;
    private long lastPositionCheckTick = -1L;
    private long lastBuffApplyTick;
    private Location location;
    private Owned current;

    private void resetForClock() {
      lastPositionCheckTick = -1L;
      lastBuffApplyTick = 0L;
      location = null;
      current = null;
    }
  }

  private record LookupResult(Optional<Owned> territory, boolean failed) {}

  private record Transition(Owned exited, Owned entered) {
    private static final Transition NONE = new Transition(null, null);
  }
}
