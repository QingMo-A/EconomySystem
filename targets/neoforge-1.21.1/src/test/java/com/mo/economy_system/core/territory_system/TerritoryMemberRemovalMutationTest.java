package com.mo.economy_system.core.territory_system;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.territory.TerritoryMemberRemovalService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class TerritoryMemberRemovalMutationTest {
  private final UUID territoryId = UUID.randomUUID();
  private final UUID owner = UUID.randomUUID();
  private final UUID target = UUID.randomUUID();
  private final UUID other = UUID.randomUUID();

  @Test
  void removesExactCanonicalMemberAndPreservesServerNameAndOthers() {
    Territory territory = territory("target");
    var outcome = TerritoryMemberRemovalMutation.remove(territory, owner, target, () -> {});
    assertEquals(TerritoryMemberRemovalService.RepositoryResult.REMOVED, outcome.result());
    assertEquals("target", outcome.removedMember().targetPlayerName());
    assertFalse(territory.hasPermission(target));
    assertEquals(Map.of(other, "other"), TerritoryMemberRemovalMutation.snapshot(territory));
  }

  @Test
  void rejectsOwnerMismatchOwnerTargetAndMissingTarget() {
    FakeMembership membership = fake();
    assertEquals(
        TerritoryMemberRemovalService.RepositoryResult.OWNER_MISMATCH,
        remove(membership, UUID.randomUUID(), target, () -> {}).result());
    assertEquals(
        TerritoryMemberRemovalService.RepositoryResult.OWNER_TARGET,
        remove(membership, owner, owner, () -> {}).result());
    assertEquals(
        TerritoryMemberRemovalService.RepositoryResult.TARGET_NOT_MEMBER,
        remove(membership, owner, UUID.randomUUID(), () -> {}).result());
  }

  @Test
  void rejectsNonCanonicalAndMalformedPreStateAsIntegrityFailure() {
    for (String invalid :
        new String[] {
          "", " target ", "x".repeat(EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH + 1)
        }) {
      FakeMembership membership = fake();
      membership.members.put(target, member(target, invalid));
      assertIntegrity(remove(membership, owner, target, () -> {}));
    }
    FakeMembership ownerMixed = fake();
    ownerMixed.members.put(owner, member(owner, "owner"));
    assertIntegrity(remove(ownerMixed, owner, target, () -> {}));
    FakeMembership nullMember = fake();
    nullMember.members.put(other, null);
    assertIntegrity(remove(nullMember, owner, target, () -> {}));
  }

  @Test
  void removeFailuresCompensateOnlyWhenFullSnapshotIsRestored() {
    for (int mode = 0; mode < 3; mode++) {
      FakeMembership membership = fake();
      membership.removeMode = mode;
      var outcome = remove(membership, owner, target, () -> {});
      assertEquals(TerritoryMemberRemovalService.RepositoryResult.PERSIST_FAILED, outcome.result());
      assertEquals(original(), membership.members);
    }
  }

  @Test
  void firstDirtyFailureFullyRollsBackAndPreservesPrimary() {
    FakeMembership membership = fake();
    int[] calls = {0};
    var outcome =
        remove(
            membership,
            owner,
            target,
            () -> {
              if (calls[0]++ == 0) throw new IllegalStateException("primary-dirty");
            });
    assertEquals(TerritoryMemberRemovalService.RepositoryResult.PERSIST_FAILED, outcome.result());
    assertEquals("primary-dirty", outcome.failure().getMessage());
    assertEquals(original(), membership.members);
  }

  @Test
  void silentDirtyDriftIsDetectedAndRolledBack() {
    FakeMembership membership = fake();
    int[] calls = {0};
    var outcome =
        remove(
            membership,
            owner,
            target,
            () -> {
              if (calls[0]++ == 0) membership.members.put(other, member(other, "changed"));
              else membership.members.put(other, member(other, "other"));
            });
    assertEquals(TerritoryMemberRemovalService.RepositoryResult.PERSIST_FAILED, outcome.result());
    assertEquals(original(), membership.members);
  }

  @Test
  void restoreConflictNameDriftAndExtraMembersAreUnknown() {
    FakeMembership conflict = fake();
    conflict.restoreMode = 1;
    assertUnknown(
        remove(
            conflict,
            owner,
            target,
            () -> {
              throw new IllegalStateException("x");
            }));

    FakeMembership wrongName = fake();
    wrongName.restoreMode = 2;
    assertUnknown(
        remove(
            wrongName,
            owner,
            target,
            () -> {
              throw new IllegalStateException("x");
            }));

    FakeMembership extra = fake();
    extra.restoreMode = 3;
    assertUnknown(
        remove(
            extra,
            owner,
            target,
            () -> {
              throw new IllegalStateException("x");
            }));
  }

  @Test
  void secondDirtyFailureIsSuppressedAndStateUnknown() {
    FakeMembership membership = fake();
    var outcome =
        remove(
            membership,
            owner,
            target,
            () -> {
              throw new IllegalStateException("dirty");
            });
    assertUnknown(outcome);
    assertEquals(1, outcome.failure().getSuppressed().length);
  }

  @Test
  void jvmErrorsEscapeFromMutationAndDirtyMarker() {
    FakeMembership membership = fake();
    membership.removeError = new AssertionError("remove");
    assertThrows(AssertionError.class, () -> remove(membership, owner, target, () -> {}));
    assertThrows(
        AssertionError.class,
        () ->
            remove(
                fake(),
                owner,
                target,
                () -> {
                  throw new AssertionError("dirty");
                }));
  }

  private Territory territory(String targetName) {
    Territory territory =
        new Territory(
            territoryId,
            "land",
            owner,
            "owner",
            0,
            64,
            0,
            1,
            64,
            1,
            new BlockPos(0, 64, 0),
            Level.OVERWORLD);
    territory.getAuthorizedPlayers().add(new PlayerInfo(target, targetName));
    territory.getAuthorizedPlayers().add(new PlayerInfo(other, "other"));
    return territory;
  }

  private FakeMembership fake() {
    return new FakeMembership(owner, original());
  }

  private Map<UUID, TerritoryMemberRemovalMutation.MemberSnapshot> original() {
    Map<UUID, TerritoryMemberRemovalMutation.MemberSnapshot> result = new LinkedHashMap<>();
    result.put(target, member(target, "target"));
    result.put(other, member(other, "other"));
    return result;
  }

  private TerritoryMemberRemovalService.RepositoryOutcome remove(
      FakeMembership membership,
      UUID expectedOwner,
      UUID requestedTarget,
      TerritoryMemberRemovalMutation.DirtyMarker marker) {
    return TerritoryMemberRemovalMutation.remove(
        membership, territoryId, "land", expectedOwner, requestedTarget, marker);
  }

  private static TerritoryMemberRemovalMutation.MemberSnapshot member(UUID id, String name) {
    return new TerritoryMemberRemovalMutation.MemberSnapshot(id, name);
  }

  private static void assertIntegrity(TerritoryMemberRemovalService.RepositoryOutcome outcome) {
    assertEquals(TerritoryMemberRemovalService.RepositoryResult.STATE_UNKNOWN, outcome.result());
    assertEquals(
        TerritoryMemberRemovalService.RepositoryFailureKind.INTEGRITY, outcome.failureKind());
  }

  private static void assertUnknown(TerritoryMemberRemovalService.RepositoryOutcome outcome) {
    assertEquals(TerritoryMemberRemovalService.RepositoryResult.STATE_UNKNOWN, outcome.result());
    assertEquals(
        TerritoryMemberRemovalService.RepositoryFailureKind.UNKNOWN, outcome.failureKind());
  }

  private static final class FakeMembership implements TerritoryMemberRemovalMutation.Membership {
    final UUID owner;
    final Map<UUID, TerritoryMemberRemovalMutation.MemberSnapshot> members;
    int removeMode = -1;
    int restoreMode;
    Error removeError;

    FakeMembership(UUID owner, Map<UUID, TerritoryMemberRemovalMutation.MemberSnapshot> members) {
      this.owner = owner;
      this.members = new LinkedHashMap<>(members);
    }

    public UUID ownerId() {
      return owner;
    }

    public Map<UUID, TerritoryMemberRemovalMutation.MemberSnapshot> snapshotAll() {
      return new LinkedHashMap<>(members);
    }

    public boolean removeExact(UUID id, String name) {
      if (removeError != null) throw removeError;
      if (removeMode == 0) throw new IllegalStateException("remove");
      if (removeMode == 1) return false;
      if (removeMode == 2) return true;
      members.remove(id);
      return removeMode != 1;
    }

    public boolean restoreExact(UUID id, String name) {
      if (restoreMode == 1) return false;
      members.put(id, member(id, restoreMode == 2 ? "different" : name));
      if (restoreMode == 3) members.put(UUID.randomUUID(), member(UUID.randomUUID(), "extra"));
      return true;
    }

    public long count(UUID id) {
      return members.containsKey(id) ? 1 : 0;
    }

    public boolean contains(UUID id) {
      return members.containsKey(id);
    }
  }
}
