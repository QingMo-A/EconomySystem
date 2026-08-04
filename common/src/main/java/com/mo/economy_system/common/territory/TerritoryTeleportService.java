package com.mo.economy_system.common.territory;

import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

/** Loader-neutral protocol-19 validation and recall-potion transaction. */
public final class TerritoryTeleportService<D> {
  public interface Repository { Optional<TerritoryTeleportTarget> find(UUID territoryId); }
  public interface DestinationAdapter<D> {
    Optional<D> resolve(String dimensionId);
    boolean prepareAndValidate(D dimension, TerritorySnapshots.Position backpoint) throws Exception;
    void teleport(D dimension, TerritorySnapshots.Position backpoint) throws Exception;
    TerritoryTeleportArrival arrival(D dimension, TerritorySnapshots.Position backpoint);
    void particles(D dimension, TerritorySnapshots.Position backpoint) throws Exception;
    void sound(D dimension, TerritorySnapshots.Position backpoint) throws Exception;
  }
  public interface Inventory { Optional<Reservation> reserveRecallPotion() throws Exception; }
  public interface Reservation { int slot(); void commit(); void rollback() throws Exception; }
  public interface Diagnostics { void warning(String stage, UUID playerId, UUID territoryId, int slot, Throwable primary, Throwable secondary); }

  private final Repository repository; private final DestinationAdapter<D> destination;
  private final Inventory inventory; private final TerritoryTeleportRateLimiter limiter; private final Diagnostics diagnostics;
  public TerritoryTeleportService(Repository repository, DestinationAdapter<D> destination, Inventory inventory,
      TerritoryTeleportRateLimiter limiter, Diagnostics diagnostics) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.destination = Objects.requireNonNull(destination, "destination");
    this.inventory = Objects.requireNonNull(inventory, "inventory");
    this.limiter = Objects.requireNonNull(limiter, "limiter");
    this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
  }

  public TerritoryTeleportOutcome execute(UUID requesterId, UUID territoryId, long serverTick) {
    Objects.requireNonNull(requesterId, "requesterId"); Objects.requireNonNull(territoryId, "territoryId");
    if (serverTick < 0) throw new IllegalArgumentException("serverTick must be non-negative");
    TerritoryTeleportTarget target;
    try {
      Optional<TerritoryTeleportTarget> found = repository.find(territoryId);
      if (found == null) throw new IllegalStateException("repository returned null Optional");
      target = found.orElse(null);
    } catch (Exception error) {
      warn("repository", requesterId, territoryId, -1, error, null);
      return TerritoryTeleportOutcome.of(TerritoryTeleportResult.TELEPORT_FAILED);
    }
    if (target == null) return TerritoryTeleportOutcome.of(TerritoryTeleportResult.TERRITORY_NOT_FOUND);
    if (!territoryId.equals(target.territoryId())) {
      warn("target", requesterId, territoryId, -1,
          new IllegalStateException("repository returned territory " + target.territoryId()), null);
      return TerritoryTeleportOutcome.of(TerritoryTeleportResult.TELEPORT_FAILED);
    }
    if (!target.permits(requesterId)) return TerritoryTeleportOutcome.of(TerritoryTeleportResult.NO_PERMISSION);
    if (target.backpoint().isEmpty()) return TerritoryTeleportOutcome.of(TerritoryTeleportResult.NO_BACKPOINT);
    D dimension;
    try {
      Optional<D> resolved = destination.resolve(target.dimensionId());
      if (resolved == null) throw new IllegalStateException("resolver returned null Optional");
      dimension = resolved.orElse(null);
    } catch (Exception error) {
      warn("dimension", requesterId, territoryId, -1, error, null);
      return TerritoryTeleportOutcome.of(TerritoryTeleportResult.TELEPORT_FAILED);
    }
    if (dimension == null) return TerritoryTeleportOutcome.of(TerritoryTeleportResult.DIMENSION_NOT_FOUND);
    if (!limiter.tryAcquire(requesterId, serverTick)) return TerritoryTeleportOutcome.of(TerritoryTeleportResult.COOLDOWN);
    try {
      if (!destination.prepareAndValidate(dimension, target.backpoint().get()))
        return TerritoryTeleportOutcome.of(TerritoryTeleportResult.UNSAFE_DESTINATION);
    } catch (Exception error) {
      warn("prepare", requesterId, territoryId, -1, error, null);
      return TerritoryTeleportOutcome.of(TerritoryTeleportResult.TELEPORT_FAILED);
    }
    Reservation reservation;
    try { reservation = inventory.reserveRecallPotion().orElse(null); }
    catch (RecallPotionReserveException error) {
      warn("reserve",requesterId,territoryId,error.slot(),error,error.getCause() instanceof Exception cause?cause:null);
      return TerritoryTeleportOutcome.of(error.rollbackFailed()?TerritoryTeleportResult.ROLLBACK_FAILED:TerritoryTeleportResult.TELEPORT_FAILED);
    }
    catch (Exception error) { warn("reserve", requesterId, territoryId, -1, error, null); return TerritoryTeleportOutcome.of(TerritoryTeleportResult.TELEPORT_FAILED); }
    if (reservation == null) return TerritoryTeleportOutcome.of(TerritoryTeleportResult.NO_RECALL_POTION);
    Exception teleportError = null; TerritoryTeleportArrival arrival;
    try { destination.teleport(dimension, target.backpoint().get()); }
    catch (Exception error) { teleportError = error; }
    try{arrival=Objects.requireNonNull(destination.arrival(dimension,target.backpoint().get()),"arrival");}
    catch(RuntimeException error){
      try{reservation.commit();}catch(RuntimeException commitError){if(commitError!=error)error.addSuppressed(commitError);}
      warn("arrival-unknown",requesterId,territoryId,reservation.slot(),error,null);
      return TerritoryTeleportOutcome.of(TerritoryTeleportResult.TELEPORT_STATE_UNKNOWN);
    }
    if(arrival==TerritoryTeleportArrival.UNKNOWN){reservation.commit();warn("arrival-unknown",requesterId,territoryId,reservation.slot(),new IllegalStateException("adapter reported UNKNOWN"),null);return TerritoryTeleportOutcome.of(TerritoryTeleportResult.TELEPORT_STATE_UNKNOWN);}
    if (arrival == TerritoryTeleportArrival.NOT_ARRIVED) {
      if (teleportError == null) teleportError = new IllegalStateException("player did not arrive at destination");
      try { reservation.rollback(); }
      catch (Exception rollbackError) {
        if (rollbackError != teleportError && !containsSuppressed(teleportError, rollbackError)) teleportError.addSuppressed(rollbackError);
        warn("rollback", requesterId, territoryId, reservation.slot(), teleportError, rollbackError);
        return TerritoryTeleportOutcome.of(TerritoryTeleportResult.ROLLBACK_FAILED);
      }
      warn("teleport", requesterId, territoryId, reservation.slot(), teleportError, null);
      return TerritoryTeleportOutcome.of(TerritoryTeleportResult.TELEPORT_FAILED);
    }
    if (teleportError != null) warn("teleport-arrived", requesterId, territoryId, reservation.slot(), teleportError, null);
    try { reservation.commit(); } catch (RuntimeException error) { warn("commit-after-arrival", requesterId, territoryId, reservation.slot(), error, null); }
    try { destination.particles(dimension, target.backpoint().get()); } catch (Exception error) { warn("particles", requesterId, territoryId, reservation.slot(), error, null); }
    try { destination.sound(dimension, target.backpoint().get()); } catch (Exception error) { warn("sound", requesterId, territoryId, reservation.slot(), error, null); }
    return TerritoryTeleportOutcome.success(target.territoryName());
  }

  private void warn(String stage, UUID playerId, UUID territoryId, int slot, Exception primary, Exception secondary) {
    try { diagnostics.warning(stage, playerId, territoryId, slot, primary, secondary); }
    catch (Exception ignored) { /* Diagnostics cannot change protocol semantics. */ }
  }
  private static boolean containsSuppressed(Throwable primary, Throwable candidate) {
    for (Throwable value : primary.getSuppressed()) if (value == candidate) return true;
    return false;
  }
}
