package com.mo.economy_system.core.territory_system;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.territory.TerritoryMemberRemovalService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class TerritoryMemberRemovalMutation {
  record MemberSnapshot(UUID playerId, String playerName) {
    MemberSnapshot {
      Objects.requireNonNull(playerId, "playerId");
      Objects.requireNonNull(playerName, "playerName");
    }
  }

  interface Membership {
    UUID ownerId();

    Map<UUID, MemberSnapshot> snapshotAll();

    boolean removeExact(UUID playerId, String expectedName);

    boolean restoreExact(UUID playerId, String expectedName);

    long count(UUID playerId);

    boolean contains(UUID playerId);
  }

  interface DirtyMarker {
    void markDirty();
  }

  private TerritoryMemberRemovalMutation() {}

  static TerritoryMemberRemovalService.RepositoryOutcome remove(
      Territory territory, UUID expectedOwner, UUID target, DirtyMarker dirtyMarker) {
    Objects.requireNonNull(territory, "territory");
    return remove(
        new Membership() {
          public UUID ownerId() {
            return territory.getOwnerUUID();
          }

          public Map<UUID, MemberSnapshot> snapshotAll() {
            Map<UUID, MemberSnapshot> result = new LinkedHashMap<>();
            for (PlayerInfo member : territory.getAuthorizedPlayers()) {
              if (member == null || member.getUuid() == null || member.getName() == null)
                throw new IllegalStateException("null member");
              MemberSnapshot previous =
                  result.putIfAbsent(
                      member.getUuid(), new MemberSnapshot(member.getUuid(), member.getName()));
              if (previous != null) throw new IllegalStateException("duplicate member UUID");
            }
            return result;
          }

          public boolean removeExact(UUID id, String expectedName) {
            Map<UUID, MemberSnapshot> current = snapshotAll();
            MemberSnapshot member = current.get(id);
            return member != null
                && member.playerName().equals(expectedName)
                && territory.getAuthorizedPlayers().remove(new PlayerInfo(id, expectedName));
          }

          public boolean restoreExact(UUID id, String expectedName) {
            if (contains(id)) return false;
            return territory.getAuthorizedPlayers().add(new PlayerInfo(id, expectedName));
          }

          public long count(UUID id) {
            return territory.authorizedPlayerCount(id);
          }

          public boolean contains(UUID id) {
            return territory.hasPermission(id);
          }
        },
        territory.getTerritoryID(),
        territory.getName(),
        expectedOwner,
        target,
        dirtyMarker);
  }

  static TerritoryMemberRemovalService.RepositoryOutcome remove(
      Membership membership,
      UUID territoryId,
      String territoryName,
      UUID expectedOwner,
      UUID target,
      DirtyMarker dirtyMarker) {
    Objects.requireNonNull(membership, "membership");
    Objects.requireNonNull(territoryId, "territoryId");
    Objects.requireNonNull(territoryName, "territoryName");
    Objects.requireNonNull(expectedOwner, "expectedOwner");
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(dirtyMarker, "dirtyMarker");
    if (!expectedOwner.equals(membership.ownerId()))
      return out(TerritoryMemberRemovalService.RepositoryResult.OWNER_MISMATCH);
    if (expectedOwner.equals(target))
      return out(TerritoryMemberRemovalService.RepositoryResult.OWNER_TARGET);

    Map<UUID, MemberSnapshot> before;
    try {
      before = validatedSnapshot(membership, expectedOwner);
    } catch (RuntimeException failure) {
      return unknown(TerritoryMemberRemovalService.RepositoryFailureKind.INTEGRITY, failure);
    }
    MemberSnapshot removed = before.get(target);
    if (removed == null)
      return out(TerritoryMemberRemovalService.RepositoryResult.TARGET_NOT_MEMBER);
    Map<UUID, MemberSnapshot> expected = new LinkedHashMap<>(before);
    expected.remove(target);

    RuntimeException primary;
    try {
      if (!membership.removeExact(target, removed.playerName())
          || membership.count(target) != 0
          || membership.contains(target)) throw new IllegalStateException("target removal failed");
      if (!validatedSnapshot(membership, expectedOwner).equals(expected))
        throw new IllegalStateException("unrelated members changed");
      dirtyMarker.markDirty();
      if (!validatedSnapshot(membership, expectedOwner).equals(expected))
        throw new IllegalStateException("post-dirty members changed");
      return new TerritoryMemberRemovalService.RepositoryOutcome(
          TerritoryMemberRemovalService.RepositoryResult.REMOVED,
          new TerritoryMemberRemovalService.RemovedMember(
              territoryId, expectedOwner, territoryName, target, removed.playerName()));
    } catch (RuntimeException failure) {
      primary = failure;
    }

    try {
      Map<UUID, MemberSnapshot> current = validatedSnapshot(membership, expectedOwner);
      MemberSnapshot currentTarget = current.get(target);
      if (currentTarget == null) {
        if (!membership.restoreExact(target, removed.playerName()))
          throw new IllegalStateException("target restoration failed");
      } else if (!currentTarget.equals(removed)) {
        throw new IllegalStateException("target restoration conflict");
      }
      dirtyMarker.markDirty();
      if (!validatedSnapshot(membership, expectedOwner).equals(before))
        throw new IllegalStateException("member rollback mismatch");
      return new TerritoryMemberRemovalService.RepositoryOutcome(
          TerritoryMemberRemovalService.RepositoryResult.PERSIST_FAILED,
          null,
          TerritoryMemberRemovalService.RepositoryFailureKind.PERSISTENCE,
          primary);
    } catch (RuntimeException rollback) {
      primary.addSuppressed(rollback);
      return unknown(TerritoryMemberRemovalService.RepositoryFailureKind.UNKNOWN, primary);
    }
  }

  static Map<UUID, String> snapshot(Territory territory) {
    Map<UUID, MemberSnapshot> raw =
        validatedSnapshot(
            new Membership() {
              public UUID ownerId() {
                return territory.getOwnerUUID();
              }

              public Map<UUID, MemberSnapshot> snapshotAll() {
                Map<UUID, MemberSnapshot> result = new LinkedHashMap<>();
                for (PlayerInfo member : territory.getAuthorizedPlayers()) {
                  if (member == null || member.getUuid() == null || member.getName() == null)
                    throw new IllegalStateException("null member");
                  if (result.putIfAbsent(
                          member.getUuid(), new MemberSnapshot(member.getUuid(), member.getName()))
                      != null) throw new IllegalStateException("duplicate member UUID");
                }
                return result;
              }

              public boolean removeExact(UUID id, String name) {
                throw new UnsupportedOperationException();
              }

              public boolean restoreExact(UUID id, String name) {
                throw new UnsupportedOperationException();
              }

              public long count(UUID id) {
                return territory.authorizedPlayerCount(id);
              }

              public boolean contains(UUID id) {
                return territory.hasPermission(id);
              }
            },
            territory.getOwnerUUID());
    Map<UUID, String> result = new LinkedHashMap<>();
    raw.forEach((id, member) -> result.put(id, member.playerName()));
    return result;
  }

  private static Map<UUID, MemberSnapshot> validatedSnapshot(
      Membership membership, UUID expectedOwner) {
    Map<UUID, MemberSnapshot> supplied =
        new LinkedHashMap<>(Objects.requireNonNull(membership.snapshotAll(), "member snapshot"));
    for (Map.Entry<UUID, MemberSnapshot> entry : supplied.entrySet()) {
      UUID id = entry.getKey();
      MemberSnapshot member = entry.getValue();
      if (id == null
          || member == null
          || !id.equals(member.playerId())
          || expectedOwner.equals(id)
          || membership.count(id) != 1
          || !membership.contains(id)) throw new IllegalStateException("invalid member set");
      String rawName = member.playerName();
      if (rawName.isEmpty()
          || !rawName.equals(rawName.trim())
          || rawName.length() > EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH)
        throw new IllegalStateException("non-canonical member name");
    }
    return supplied;
  }

  private static TerritoryMemberRemovalService.RepositoryOutcome out(
      TerritoryMemberRemovalService.RepositoryResult result) {
    return new TerritoryMemberRemovalService.RepositoryOutcome(result, null);
  }

  private static TerritoryMemberRemovalService.RepositoryOutcome unknown(
      TerritoryMemberRemovalService.RepositoryFailureKind kind, RuntimeException failure) {
    return new TerritoryMemberRemovalService.RepositoryOutcome(
        TerritoryMemberRemovalService.RepositoryResult.STATE_UNKNOWN, null, kind, failure);
  }
}
