package com.mo.economy_system.core.territory_system;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.territory.TerritoryMemberRemovalService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class TerritoryMemberRemovalMutation {
  interface DirtyMarker {
    void markDirty();
  }

  private TerritoryMemberRemovalMutation() {}

  static TerritoryMemberRemovalService.RepositoryOutcome remove(
      Territory territory, UUID expectedOwner, UUID target, DirtyMarker dirtyMarker) {
    Objects.requireNonNull(territory);
    Objects.requireNonNull(expectedOwner);
    Objects.requireNonNull(target);
    Objects.requireNonNull(dirtyMarker);
    if (!expectedOwner.equals(territory.getOwnerUUID()))
      return out(TerritoryMemberRemovalService.RepositoryResult.OWNER_MISMATCH);
    if (expectedOwner.equals(target))
      return out(TerritoryMemberRemovalService.RepositoryResult.OWNER_TARGET);
    Map<UUID, String> before;
    try {
      before = snapshot(territory);
    } catch (RuntimeException e) {
      return unknown(TerritoryMemberRemovalService.RepositoryFailureKind.INTEGRITY, e);
    }
    String targetName = before.get(target);
    if (targetName == null)
      return out(TerritoryMemberRemovalService.RepositoryResult.TARGET_NOT_MEMBER);
    RuntimeException primary;
    try {
      if (!territory.getAuthorizedPlayers().remove(new PlayerInfo(target, targetName))
          || territory.authorizedPlayerCount(target) != 0
          || territory.hasPermission(target))
        throw new IllegalStateException("target removal failed");
      Map<UUID, String> expected = new LinkedHashMap<>(before);
      expected.remove(target);
      if (!snapshot(territory).equals(expected))
        throw new IllegalStateException("unrelated members changed");
      dirtyMarker.markDirty();
      if (!snapshot(territory).equals(expected))
        throw new IllegalStateException("post-dirty members changed");
      return new TerritoryMemberRemovalService.RepositoryOutcome(
          TerritoryMemberRemovalService.RepositoryResult.REMOVED,
          new TerritoryMemberRemovalService.RemovedMember(
              territory.getTerritoryID(), expectedOwner, territory.getName(), target, targetName));
    } catch (RuntimeException e) {
      primary = e;
    }
    boolean restored = true;
    try {
      Map<UUID, String> current = snapshot(territory);
      if (!current.containsKey(target)) {
        if (!territory.addAuthorizedPlayerIfAbsent(target, targetName))
          throw new IllegalStateException("target restoration failed");
      } else if (!Objects.equals(current.get(target), targetName))
        throw new IllegalStateException("target restoration conflict");
      dirtyMarker.markDirty();
      if (!snapshot(territory).equals(before))
        throw new IllegalStateException("member rollback mismatch");
    } catch (RuntimeException rollback) {
      restored = false;
      primary.addSuppressed(rollback);
    }
    return restored
        ? new TerritoryMemberRemovalService.RepositoryOutcome(
            TerritoryMemberRemovalService.RepositoryResult.PERSIST_FAILED,
            null,
            TerritoryMemberRemovalService.RepositoryFailureKind.PERSISTENCE,
            primary)
        : unknown(TerritoryMemberRemovalService.RepositoryFailureKind.UNKNOWN, primary);
  }

  static Map<UUID, String> snapshot(Territory territory) {
    Map<UUID, String> result = new LinkedHashMap<>();
    for (PlayerInfo member : territory.getAuthorizedPlayers()) {
      if (member == null || member.getUuid() == null || member.getName() == null)
        throw new IllegalStateException("null member");
      String name = member.getName().trim();
      if (name.isEmpty()
          || name.length() > EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH
          || territory.getOwnerUUID().equals(member.getUuid())
          || result.putIfAbsent(member.getUuid(), name) != null)
        throw new IllegalStateException("invalid member set");
    }
    return result;
  }

  private static TerritoryMemberRemovalService.RepositoryOutcome out(
      TerritoryMemberRemovalService.RepositoryResult r) {
    return new TerritoryMemberRemovalService.RepositoryOutcome(r, null);
  }

  private static TerritoryMemberRemovalService.RepositoryOutcome unknown(
      TerritoryMemberRemovalService.RepositoryFailureKind k, RuntimeException e) {
    return new TerritoryMemberRemovalService.RepositoryOutcome(
        TerritoryMemberRemovalService.RepositoryResult.STATE_UNKNOWN, null, k, e);
  }
}
