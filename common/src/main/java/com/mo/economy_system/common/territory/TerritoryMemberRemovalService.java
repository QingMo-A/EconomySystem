package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.util.Objects;
import java.util.UUID;

/** Loader-neutral authenticated-owner member removal orchestration. */
public final class TerritoryMemberRemovalService {
  public enum Result {
    SUCCESS,
    TERRITORY_NOT_FOUND,
    NO_PERMISSION,
    CANNOT_REMOVE_OWNER,
    TARGET_NOT_MEMBER,
    RATE_LIMITED,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  public enum RepositoryResult {
    REMOVED,
    TERRITORY_NOT_FOUND,
    OWNER_MISMATCH,
    OWNER_TARGET,
    TARGET_NOT_MEMBER,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  public enum RepositoryFailureKind {
    NONE,
    INTEGRITY,
    PERSISTENCE,
    UNKNOWN
  }

  public record RemovedMember(
      UUID territoryId,
      UUID ownerId,
      String territoryName,
      UUID targetPlayerId,
      String targetPlayerName) {
    public RemovedMember {
      Objects.requireNonNull(territoryId, "territoryId");
      Objects.requireNonNull(ownerId, "ownerId");
      Objects.requireNonNull(targetPlayerId, "targetPlayerId");
      territoryName = validName(territoryName, EconomyNetworkLimits.MAX_TERRITORY_NAME_LENGTH);
      targetPlayerName = validName(targetPlayerName, EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH);
    }
  }

  public record RepositoryOutcome(
      RepositoryResult result,
      RemovedMember removedMember,
      RepositoryFailureKind failureKind,
      Throwable failure) {
    public RepositoryOutcome {
      Objects.requireNonNull(result, "result");
      Objects.requireNonNull(failureKind, "failureKind");
      if ((result == RepositoryResult.REMOVED) != (removedMember != null))
        throw new IllegalArgumentException("result/snapshot");
      if ((failureKind == RepositoryFailureKind.NONE) != (failure == null))
        throw new IllegalArgumentException("failure/kind");
      if (result == RepositoryResult.PERSIST_FAILED
          && failureKind != RepositoryFailureKind.PERSISTENCE)
        throw new IllegalArgumentException("persistence kind");
      if (result == RepositoryResult.STATE_UNKNOWN
          && failureKind != RepositoryFailureKind.INTEGRITY
          && failureKind != RepositoryFailureKind.UNKNOWN)
        throw new IllegalArgumentException("unknown kind");
      if (result != RepositoryResult.PERSIST_FAILED
          && result != RepositoryResult.STATE_UNKNOWN
          && failureKind != RepositoryFailureKind.NONE)
        throw new IllegalArgumentException("unexpected failure");
      if (failure instanceof Error error) throw error;
    }

    public RepositoryOutcome(RepositoryResult result, RemovedMember removedMember) {
      this(result, removedMember, RepositoryFailureKind.NONE, null);
    }
  }

  public record Outcome(Result result, RemovedMember removedMember) {
    public Outcome {
      Objects.requireNonNull(result, "result");
      if ((result == Result.SUCCESS) != (removedMember != null))
        throw new IllegalArgumentException("result/snapshot");
    }
  }

  public interface Repository {
    RepositoryOutcome remove(UUID territoryId, UUID expectedOwnerId, UUID targetPlayerId);
  }

  public interface Cleanup {
    TerritoryInviteStore.DiscardResult cleanup(UUID targetPlayerId, UUID territoryId, long tick);
  }

  public interface Diagnostics {
    void warning(String stage, UUID playerId, UUID territoryId, Throwable error);
  }

  private final Repository repository;
  private final TerritoryMemberRemovalRateLimiter limiter;
  private final Cleanup cleanup;
  private final Diagnostics diagnostics;

  public TerritoryMemberRemovalService(
      Repository repository,
      TerritoryMemberRemovalRateLimiter limiter,
      Cleanup cleanup,
      Diagnostics diagnostics) {
    this.repository = Objects.requireNonNull(repository);
    this.limiter = Objects.requireNonNull(limiter);
    this.cleanup = Objects.requireNonNull(cleanup);
    this.diagnostics = Objects.requireNonNull(diagnostics);
  }

  public Outcome remove(UUID playerId, UUID territoryId, UUID targetPlayerId, long tick) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(territoryId, "territoryId");
    Objects.requireNonNull(targetPlayerId, "targetPlayerId");
    if (tick < 0) throw new IllegalArgumentException("tick");
    if (!limiter.acquire(playerId, tick)) return out(Result.RATE_LIMITED);
    RepositoryOutcome repositoryOutcome;
    try {
      repositoryOutcome = repository.remove(territoryId, playerId, targetPlayerId);
    } catch (RuntimeException failure) {
      warn("repository", playerId, territoryId, failure);
      return out(Result.STATE_UNKNOWN);
    }
    if (repositoryOutcome == null) {
      warn("repository", playerId, territoryId, new IllegalStateException("null outcome"));
      return out(Result.STATE_UNKNOWN);
    }
    if (repositoryOutcome.failure() != null)
      warn(repositoryStage(repositoryOutcome), playerId, territoryId, repositoryOutcome.failure());
    if (repositoryOutcome.result() == RepositoryResult.REMOVED) {
      RemovedMember removed = repositoryOutcome.removedMember();
      if (!territoryId.equals(removed.territoryId())
          || !playerId.equals(removed.ownerId())
          || !targetPlayerId.equals(removed.targetPlayerId())) {
        warn(
            "repository-contract",
            playerId,
            territoryId,
            new IllegalStateException("snapshot mismatch"));
        return out(Result.STATE_UNKNOWN);
      }
      try {
        TerritoryInviteStore.DiscardResult result =
            cleanup.cleanup(targetPlayerId, territoryId, tick);
        if (result.processingSkipped() > 0)
          warn(
              "cleanup-pending-invite-processing",
              playerId,
              territoryId,
              new IllegalStateException("processing invite skipped"));
      } catch (RuntimeException failure) {
        warn("cleanup-pending-invite", playerId, territoryId, failure);
      }
      return new Outcome(Result.SUCCESS, removed);
    }
    return out(
        switch (repositoryOutcome.result()) {
          case TERRITORY_NOT_FOUND -> Result.TERRITORY_NOT_FOUND;
          case OWNER_MISMATCH -> Result.NO_PERMISSION;
          case OWNER_TARGET -> Result.CANNOT_REMOVE_OWNER;
          case TARGET_NOT_MEMBER -> Result.TARGET_NOT_MEMBER;
          case PERSIST_FAILED -> Result.PERSIST_FAILED;
          case STATE_UNKNOWN -> Result.STATE_UNKNOWN;
          case REMOVED -> throw new AssertionError();
        });
  }

  private void warn(String stage, UUID player, UUID territory, Throwable failure) {
    try {
      diagnostics.warning(stage, player, territory, failure);
    } catch (RuntimeException ignored) {
    }
  }

  private static String repositoryStage(RepositoryOutcome outcome) {
    return switch (outcome.failureKind()) {
      case INTEGRITY -> "repository-integrity";
      case PERSISTENCE -> "repository-persist-failed";
      case UNKNOWN -> "repository-state-unknown";
      case NONE -> throw new IllegalArgumentException("no failure");
    };
  }

  private static String validName(String value, int max) {
    Objects.requireNonNull(value, "name");
    value = value.trim();
    if (value.isEmpty() || value.length() > max) throw new IllegalArgumentException("name");
    return value;
  }

  private static Outcome out(Result result) {
    return new Outcome(result, null);
  }
}
