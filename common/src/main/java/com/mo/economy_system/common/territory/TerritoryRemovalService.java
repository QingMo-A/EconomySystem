package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.util.*;

/** Loader-neutral authenticated-owner territory deletion orchestration. */
public final class TerritoryRemovalService {
  public enum Result {
    SUCCESS,
    TERRITORY_NOT_FOUND,
    NO_PERMISSION,
    RATE_LIMITED,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  public enum RepositoryResult {
    REMOVED,
    NOT_FOUND,
    OWNER_MISMATCH,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  public record RemovedTerritory(UUID territoryId, UUID ownerId, String territoryName) {
    public RemovedTerritory {
      Objects.requireNonNull(territoryId);
      Objects.requireNonNull(ownerId);
      Objects.requireNonNull(territoryName);
      territoryName = territoryName.trim();
      if (territoryName.isEmpty()
          || territoryName.length() > EconomyNetworkLimits.MAX_TERRITORY_NAME_LENGTH)
        throw new IllegalArgumentException("territoryName");
    }
  }

  public record RepositoryOutcome(
      RepositoryResult result, RemovedTerritory removedTerritory, Throwable failure) {
    public RepositoryOutcome {
      Objects.requireNonNull(result);
      if ((result == RepositoryResult.REMOVED) != (removedTerritory != null))
        throw new IllegalArgumentException("result/snapshot");
      if ((result == RepositoryResult.REMOVED
              || result == RepositoryResult.NOT_FOUND
              || result == RepositoryResult.OWNER_MISMATCH)
          && failure != null) throw new IllegalArgumentException("unexpected failure");
      if (failure instanceof Error error) throw error;
    }

    public RepositoryOutcome(RepositoryResult result, RemovedTerritory removedTerritory) {
      this(result, removedTerritory, null);
    }
  }

  public record Outcome(Result result, RemovedTerritory removedTerritory) {
    public Outcome {
      Objects.requireNonNull(result);
      if ((result == Result.SUCCESS) != (removedTerritory != null))
        throw new IllegalArgumentException("result/snapshot");
    }
  }

  public interface Repository {
    RepositoryOutcome remove(UUID territoryId, UUID expectedOwnerId);
  }

  public interface Cleanup {
    void cleanup(RemovedTerritory removedTerritory, long tick);
  }

  public interface Diagnostics {
    void warning(String stage, UUID playerId, UUID territoryId, Throwable error);
  }

  private final Repository repository;
  private final TerritoryRemovalRateLimiter limiter;
  private final Cleanup invites;
  private final Cleanup resize;
  private final Diagnostics diagnostics;

  public TerritoryRemovalService(
      Repository repository,
      TerritoryRemovalRateLimiter limiter,
      Cleanup invites,
      Cleanup resize,
      Diagnostics diagnostics) {
    this.repository = Objects.requireNonNull(repository);
    this.limiter = Objects.requireNonNull(limiter);
    this.invites = Objects.requireNonNull(invites);
    this.resize = Objects.requireNonNull(resize);
    this.diagnostics = Objects.requireNonNull(diagnostics);
  }

  public Outcome remove(UUID playerId, UUID territoryId, long tick) {
    Objects.requireNonNull(playerId);
    Objects.requireNonNull(territoryId);
    if (tick < 0) throw new IllegalArgumentException("tick");
    if (!limiter.acquire(playerId, tick)) return out(Result.RATE_LIMITED);
    RepositoryOutcome repositoryOutcome;
    try {
      repositoryOutcome = repository.remove(territoryId, playerId);
    } catch (RuntimeException error) {
      warn("repository", playerId, territoryId, error);
      return out(Result.STATE_UNKNOWN);
    }
    if (repositoryOutcome == null) {
      warn(
          "repository",
          playerId,
          territoryId,
          new IllegalStateException("null repository outcome"));
      return out(Result.STATE_UNKNOWN);
    }
    if (repositoryOutcome.failure() != null)
      warn(repositoryStage(repositoryOutcome), playerId, territoryId, repositoryOutcome.failure());
    if (repositoryOutcome.result() == RepositoryResult.REMOVED) {
      RemovedTerritory removed = repositoryOutcome.removedTerritory();
      if (!territoryId.equals(removed.territoryId()) || !playerId.equals(removed.ownerId())) {
        warn(
            "repository-contract",
            playerId,
            territoryId,
            new IllegalStateException("removed snapshot mismatch"));
        return out(Result.STATE_UNKNOWN);
      }
      cleanup(invites, "cleanup-invites", playerId, removed, tick);
      cleanup(resize, "cleanup-resize", playerId, removed, tick);
      return new Outcome(Result.SUCCESS, removed);
    }
    return out(
        switch (repositoryOutcome.result()) {
          case NOT_FOUND -> Result.TERRITORY_NOT_FOUND;
          case OWNER_MISMATCH -> Result.NO_PERMISSION;
          case PERSIST_FAILED -> Result.PERSIST_FAILED;
          case STATE_UNKNOWN -> Result.STATE_UNKNOWN;
          case REMOVED -> throw new AssertionError();
        });
  }

  private void cleanup(
      Cleanup cleanup, String stage, UUID player, RemovedTerritory removed, long tick) {
    try {
      cleanup.cleanup(removed, tick);
    } catch (RuntimeException error) {
      warn(stage, player, removed.territoryId(), error);
    }
  }

  private void warn(String stage, UUID player, UUID territory, Throwable error) {
    try {
      diagnostics.warning(stage, player, territory, error);
    } catch (RuntimeException ignored) {
    }
  }

  private static String repositoryStage(RepositoryOutcome outcome) {
    if (outcome.result() == RepositoryResult.PERSIST_FAILED) return "repository-persist-failed";
    String message = outcome.failure().getMessage();
    return message != null
            && (message.contains("mismatch")
                || message.contains("malformed")
                || message.contains("duplicate")
                || message.contains("invariant"))
        ? "repository-integrity"
        : "repository-state-unknown";
  }

  private static Outcome out(Result result) {
    return new Outcome(result, null);
  }
}
