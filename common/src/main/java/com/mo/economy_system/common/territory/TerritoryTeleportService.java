package com.mo.economy_system.common.territory;

import java.util.Optional;
import java.util.UUID;

/** Loader-neutral protocol-19 validation and recall-potion transaction. */
public final class TerritoryTeleportService<D> {
  public interface Repository { Optional<TerritoryTeleportTarget> find(UUID territoryId); }
  public interface DestinationAdapter<D> {
    Optional<D> resolve(String dimensionId);
    boolean prepareAndValidate(D dimension, TerritorySnapshots.Position backpoint) throws Exception;
    void teleport(D dimension, TerritorySnapshots.Position backpoint) throws Exception;
    boolean arrived(D dimension, TerritorySnapshots.Position backpoint) throws Exception;
    void particles(D dimension, TerritorySnapshots.Position backpoint) throws Exception;
    void sound(D dimension, TerritorySnapshots.Position backpoint) throws Exception;
  }
  public interface Inventory { Optional<Reservation> reserveRecallPotion() throws Exception; }
  public interface Reservation { int slot(); void rollback() throws Exception; }
  public interface Diagnostics { void warning(String stage, UUID playerId, UUID territoryId, int slot, Throwable primary, Throwable secondary); }

  private final Repository repository; private final DestinationAdapter<D> destination;
  private final Inventory inventory; private final TerritoryTeleportRateLimiter limiter; private final Diagnostics diagnostics;
  public TerritoryTeleportService(Repository repository, DestinationAdapter<D> destination, Inventory inventory,
      TerritoryTeleportRateLimiter limiter, Diagnostics diagnostics) {
    this.repository = repository; this.destination = destination; this.inventory = inventory;
    this.limiter = limiter; this.diagnostics = diagnostics;
  }

  public TerritoryTeleportOutcome execute(UUID requesterId, UUID territoryId, long serverTick) {
    TerritoryTeleportTarget target = repository.find(territoryId).orElse(null);
    if (target == null) return TerritoryTeleportOutcome.of(TerritoryTeleportResult.TERRITORY_NOT_FOUND);
    if (!target.permits(requesterId)) return TerritoryTeleportOutcome.of(TerritoryTeleportResult.NO_PERMISSION);
    if (target.backpoint().isEmpty()) return TerritoryTeleportOutcome.of(TerritoryTeleportResult.NO_BACKPOINT);
    D dimension = destination.resolve(target.dimensionId()).orElse(null);
    if (dimension == null) return TerritoryTeleportOutcome.of(TerritoryTeleportResult.DIMENSION_NOT_FOUND);
    if (!limiter.tryAcquire(requesterId, serverTick)) return TerritoryTeleportOutcome.of(TerritoryTeleportResult.COOLDOWN);
    try {
      if (!destination.prepareAndValidate(dimension, target.backpoint().get()))
        return TerritoryTeleportOutcome.of(TerritoryTeleportResult.UNSAFE_DESTINATION);
    } catch (Exception error) {
      diagnostics.warning("prepare", requesterId, territoryId, -1, error, null);
      return TerritoryTeleportOutcome.of(TerritoryTeleportResult.UNSAFE_DESTINATION);
    }
    Reservation reservation;
    try { reservation = inventory.reserveRecallPotion().orElse(null); }
    catch (Exception error) { diagnostics.warning("reserve", requesterId, territoryId, -1, error, null); return TerritoryTeleportOutcome.of(TerritoryTeleportResult.NO_RECALL_POTION); }
    if (reservation == null) return TerritoryTeleportOutcome.of(TerritoryTeleportResult.NO_RECALL_POTION);
    Throwable teleportError = null; boolean arrived = false;
    try { destination.teleport(dimension, target.backpoint().get()); }
    catch (Throwable error) { teleportError = error; }
    try { arrived = destination.arrived(dimension, target.backpoint().get()); }
    catch (Throwable error) { if (teleportError == null) teleportError = error; else teleportError.addSuppressed(error); }
    if (!arrived) {
      if (teleportError == null) teleportError = new IllegalStateException("player did not arrive at destination");
      try { reservation.rollback(); }
      catch (Throwable rollbackError) {
        diagnostics.warning("rollback", requesterId, territoryId, reservation.slot(), teleportError, rollbackError);
        return TerritoryTeleportOutcome.of(TerritoryTeleportResult.ROLLBACK_FAILED);
      }
      diagnostics.warning("teleport", requesterId, territoryId, reservation.slot(), teleportError, null);
      return TerritoryTeleportOutcome.of(TerritoryTeleportResult.TELEPORT_FAILED);
    }
    if (teleportError != null) diagnostics.warning("teleport-arrived", requesterId, territoryId, reservation.slot(), teleportError, null);
    try { destination.particles(dimension, target.backpoint().get()); } catch (Throwable error) { diagnostics.warning("particles", requesterId, territoryId, reservation.slot(), error, null); }
    try { destination.sound(dimension, target.backpoint().get()); } catch (Throwable error) { diagnostics.warning("sound", requesterId, territoryId, reservation.slot(), error, null); }
    return TerritoryTeleportOutcome.success(target.territoryName());
  }
}
