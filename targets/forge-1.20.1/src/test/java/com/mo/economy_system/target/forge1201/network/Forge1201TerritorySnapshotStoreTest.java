package com.mo.economy_system.target.forge1201.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.territory.TerritoryInviteDecisionService;
import com.mo.economy_system.common.territory.TerritoryInviteRateLimiter;
import com.mo.economy_system.common.territory.TerritoryInviteRequestService;
import com.mo.economy_system.common.territory.TerritoryInviteResult;
import com.mo.economy_system.common.territory.TerritoryInviteStore;
import com.mo.economy_system.common.territory.TerritoryRemovalService;
import com.mo.economy_system.common.territory.TerritoryAdministrationService;
import com.mo.economy_system.common.territory.TerritoryBackpointService;
import com.mo.economy_system.common.territory.TerritoryBuffTransactionService;
import com.mo.economy_system.common.territory.TerritoryManagementResult;
import com.mo.economy_system.common.network.TransferTerritoryOwnershipMessage;
import com.mo.economy_system.common.network.UpdateTerritoryPermissionMessage;
import com.mo.economy_system.common.network.UpdateTerritoryRuleMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.common.territory.TerritoryTeleportTarget;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

class Forge1201TerritorySnapshotStoreTest {
  @Test
  void permissionCompatibilityUsesMembersForMissingAndUnknownValues() {
    assertEquals(RuleLevel.OWNER_ONLY, Forge1201TerritorySnapshotStore.permission("OWNER_ONLY"));
    assertEquals(RuleLevel.MEMBERS, Forge1201TerritorySnapshotStore.permission("MEMBERS"));
    assertEquals(RuleLevel.EVERYONE, Forge1201TerritorySnapshotStore.permission("EVERYONE"));
    assertEquals(RuleLevel.MEMBERS, Forge1201TerritorySnapshotStore.permission(""));
    assertEquals(RuleLevel.MEMBERS, Forge1201TerritorySnapshotStore.permission("FUTURE"));
  }

  @Test
  void dimensionsMustBeCanonicalAndBounded() {
    assertEquals(
        "minecraft:overworld",
        Forge1201TerritorySnapshotStore.canonicalDimension("minecraft:overworld"));
    assertEquals(
        "example:moon", Forge1201TerritorySnapshotStore.canonicalDimension("example:moon"));
    assertThrows(
        IllegalArgumentException.class,
        () -> Forge1201TerritorySnapshotStore.canonicalDimension("overworld"));
    for (String invalid :
        new String[] {"", "bad id", "Minecraft:overworld", "a:b:c", "x".repeat(257)}) {
      assertThrows(
          IllegalArgumentException.class,
          () -> Forge1201TerritorySnapshotStore.canonicalDimension(invalid));
    }
  }

  @Test
  void captureFallsBackUnknownPermissionsWithoutChangingOtherData() {
    CompoundTag tag = validTerritory();
    CompoundTag permissions = new CompoundTag();
    permissions.putString(RuleAction.PLACE_BLOCK.name(), "OWNER_ONLY");
    permissions.putString(RuleAction.BREAK_BLOCK.name(), "FUTURE");
    tag.put("Permissions", permissions);
    var snapshot = Forge1201TerritorySnapshotStore.capture(tag);
    assertEquals(
        RuleLevel.OWNER_ONLY,
        snapshot.rules().stream()
            .filter(rule -> rule.action() == RuleAction.PLACE_BLOCK)
            .findFirst()
            .orElseThrow()
            .level());
    assertEquals(
        RuleLevel.MEMBERS,
        snapshot.rules().stream()
            .filter(rule -> rule.action() == RuleAction.BREAK_BLOCK)
            .findFirst()
            .orElseThrow()
            .level());
  }

  @Test
  void invalidBuffValuesFailClosedInsteadOfBeingClamped() {
    CompoundTag buff = new CompoundTag();
    buff.putString("id", "economy:speed");
    buff.putString("displayText", "Speed");
    buff.putString("effectId", "minecraft:speed");
    buff.putInt("initialLevel", 4);
    buff.putInt("single_Upgrade_Level", 0);
    buff.putInt("max_Level", 3);
    buff.putInt("level", 4);
    assertThrows(IllegalArgumentException.class, () -> Forge1201TerritorySnapshotStore.buff(buff));
  }

  @Test
  void findBuildsOwnerAndAuthorizedTeleportTarget() {
    CompoundTag tag = validTerritory();
    CompoundTag backpoint = new CompoundTag();
    backpoint.putInt("BackX", 2);
    backpoint.putInt("BackY", 64);
    backpoint.putInt("BackZ", 3);
    tag.put("Backpoint", backpoint);
    UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    CompoundTag member = new CompoundTag();
    member.putUUID("PlayerUUID", memberId);
    member.putString("PlayerName", "Member");
    ListTag members = new ListTag();
    members.add(member);
    tag.put("AuthorizedPlayers", members);

    var owned = Forge1201TerritorySnapshotStore.capture(tag);
    var store = new Forge1201TerritorySnapshotStore(List.of(owned));
    TerritoryTeleportTarget target = store.find(owned.summary().territoryId()).orElseThrow();
    assertEquals(owned.summary().name(), target.territoryName());
    assertEquals(owned.summary().ownerId(), target.ownerId());
    assertTrue(target.permits(owned.summary().ownerId()));
    assertTrue(target.permits(memberId));
    assertTrue(target.backpoint().isPresent());
    assertTrue(store.find(UUID.randomUUID()).isEmpty());
  }

  @Test
  void authorizeCopyOnWritePreservesUnknownNbtAndReloads() {
    CompoundTag root = new CompoundTag();
    CompoundTag territory = validTerritory();
    territory.putString("FutureTerritoryField", "keep-me");
    territory.put("AuthorizedPlayers", new ListTag());
    root.putString("FutureRootField", "keep-root");
    ListTag territories = new ListTag();
    territories.add(territory);
    root.put("Territories", territories);

    UUID owner = territory.getUUID("OwnerUUID");
    UUID member = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var store = Forge1201TerritorySnapshotStore.load(root);
    assertEquals(
        TerritoryInviteDecisionService.WriteResult.ADDED,
        store.authorize(territory.getUUID("TerritoryID"), owner, member, "Member"));

    CompoundTag saved = store.save(new CompoundTag());
    assertEquals("keep-root", saved.getString("FutureRootField"));
    assertEquals(
        "keep-me",
        saved
            .getList("Territories", Tag.TAG_COMPOUND)
            .getCompound(0)
            .getString("FutureTerritoryField"));
    assertEquals(
        1,
        saved
            .getList("Territories", Tag.TAG_COMPOUND)
            .getCompound(0)
            .getList("AuthorizedPlayers", Tag.TAG_COMPOUND)
            .size());
    assertTrue(Forge1201TerritorySnapshotStore.load(saved).authorized(member).size() == 1);
  }

  @Test
  void authorizeRechecksOwnerAndRejectsDuplicateUuidFailClosed() {
    CompoundTag root = new CompoundTag();
    CompoundTag territory = validTerritory();
    territory.put("AuthorizedPlayers", new ListTag());
    ListTag territories = new ListTag();
    territories.add(territory);
    root.put("Territories", territories);
    var store = Forge1201TerritorySnapshotStore.load(root);
    assertEquals(
        TerritoryInviteDecisionService.WriteResult.OWNER_CHANGED,
        store.authorize(
            territory.getUUID("TerritoryID"), UUID.randomUUID(), UUID.randomUUID(), "x"));

    UUID duplicate = UUID.fromString("00000000-0000-0000-0000-000000000003");
    ListTag members = new ListTag();
    for (int i = 0; i < 2; i++) {
      CompoundTag member = new CompoundTag();
      member.putUUID("PlayerUUID", duplicate);
      member.putString("PlayerName", "Duplicate");
      members.add(member);
    }
    territory.put("AuthorizedPlayers", members);
    root.put("Territories", territories);
    var duplicateStore = Forge1201TerritorySnapshotStore.load(root);
    assertEquals(
        TerritoryInviteDecisionService.WriteResult.STATE_UNKNOWN,
        duplicateStore.authorize(
            territory.getUUID("TerritoryID"),
            territory.getUUID("OwnerUUID"),
            UUID.randomUUID(),
            "new"));
    assertEquals(
        2,
        duplicateStore
            .rawCopy()
            .getList("Territories", Tag.TAG_COMPOUND)
            .getCompound(0)
            .getList("AuthorizedPlayers", Tag.TAG_COMPOUND)
            .size());
  }

  @Test
  void removeUsesRawCopyOnWriteAndPreservesUnknownFieldsAndOrder() {
    CompoundTag first = validTerritory();
    first.putString("FutureTerritoryField", "keep-first");
    CompoundTag second = validTerritory();
    second.putUUID("TerritoryID", UUID.fromString("10000000-0000-0000-0000-000000000002"));
    second.putString("Name", "Second");
    second.putString("FutureTerritoryField", "keep-second");
    ListTag records = new ListTag();
    records.add(first);
    records.add(second);
    CompoundTag root = new CompoundTag();
    root.putString("FutureRootField", "keep-root");
    root.put("Territories", records);

    Forge1201TerritorySnapshotStore store = Forge1201TerritorySnapshotStore.load(root);
    TerritoryRemovalService.RepositoryOutcome outcome =
        store.remove(first.getUUID("TerritoryID"), first.getUUID("OwnerUUID"));
    assertEquals(TerritoryRemovalService.RepositoryResult.REMOVED, outcome.result());
    assertEquals(first.getUUID("TerritoryID"), outcome.removedTerritory().territoryId());
    assertEquals(first.getUUID("OwnerUUID"), outcome.removedTerritory().ownerId());
    assertEquals("Home", outcome.removedTerritory().territoryName());

    CompoundTag saved = store.save(new CompoundTag());
    assertEquals("keep-root", saved.getString("FutureRootField"));
    ListTag remaining = saved.getList("Territories", Tag.TAG_COMPOUND);
    assertEquals(1, remaining.size());
    assertEquals(second.getUUID("TerritoryID"), remaining.getCompound(0).getUUID("TerritoryID"));
    assertEquals("keep-second", remaining.getCompound(0).getString("FutureTerritoryField"));
    assertTrue(
        Forge1201TerritorySnapshotStore.load(saved).find(first.getUUID("TerritoryID")).isEmpty());
  }

  @Test
  void removeRejectsMissingOwnerMalformedRootAndDuplicateRecordsWithoutMutation() {
    CompoundTag territory = validTerritory();
    ListTag records = new ListTag();
    records.add(territory);
    CompoundTag root = new CompoundTag();
    root.put("Territories", records);
    Forge1201TerritorySnapshotStore store = Forge1201TerritorySnapshotStore.load(root);
    CompoundTag before = store.rawCopy();

    assertEquals(
        TerritoryRemovalService.RepositoryResult.OWNER_MISMATCH,
        store.remove(territory.getUUID("TerritoryID"), UUID.randomUUID()).result());
    assertEquals(before, store.rawCopy());
    assertEquals(
        TerritoryRemovalService.RepositoryResult.NOT_FOUND,
        store.remove(UUID.randomUUID(), territory.getUUID("OwnerUUID")).result());
    assertEquals(before, store.rawCopy());

    CompoundTag duplicateRoot = new CompoundTag();
    ListTag duplicateRecords = new ListTag();
    duplicateRecords.add(territory);
    duplicateRecords.add(territory.copy());
    duplicateRoot.put("Territories", duplicateRecords);
    Forge1201TerritorySnapshotStore duplicateStore =
        Forge1201TerritorySnapshotStore.load(duplicateRoot);
    CompoundTag duplicateBefore = duplicateStore.rawCopy();
    assertEquals(
        TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN,
        duplicateStore
            .remove(territory.getUUID("TerritoryID"), territory.getUUID("OwnerUUID"))
            .result());
    assertEquals(duplicateBefore, duplicateStore.rawCopy());

    CompoundTag malformedRoot = new CompoundTag();
    malformedRoot.putString("Territories", "not-a-list");
    Forge1201TerritorySnapshotStore malformedStore =
        Forge1201TerritorySnapshotStore.load(malformedRoot);
    assertEquals(
        TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN,
        malformedStore
            .remove(territory.getUUID("TerritoryID"), territory.getUUID("OwnerUUID"))
            .result());
  }

  @Test
  void removeDirtyFailureRollsBackRawAndParsedSnapshots() {
    CompoundTag territory = validTerritory();
    ListTag records = new ListTag();
    records.add(territory);
    CompoundTag root = new CompoundTag();
    root.put("Territories", records);
    AtomicInteger marks = new AtomicInteger();
    DirtyMarker marker =
        () -> {
          if (marks.getAndIncrement() == 0) throw new IllegalStateException("dirty");
        };
    Forge1201TerritorySnapshotStore store = new Forge1201TerritorySnapshotStore(root, marker);
    CompoundTag before = store.rawCopy();
    TerritoryRemovalService.RepositoryOutcome outcome =
        store.remove(territory.getUUID("TerritoryID"), territory.getUUID("OwnerUUID"));
    assertEquals(TerritoryRemovalService.RepositoryResult.PERSIST_FAILED, outcome.result());
    assertEquals(before, store.rawCopy());
    assertEquals(1, store.owned(territory.getUUID("OwnerUUID")).size());
    assertEquals(2, marks.get());
  }

  @Test
  void inviteLookupReadsRawAndFailsClosedForDuplicateTerritoryIds() {
    CompoundTag first = validTerritory();
    first.put("AuthorizedPlayers", new ListTag());
    CompoundTag duplicate = validTerritory();
    duplicate.putString("Name", "Duplicate");
    duplicate.put("AuthorizedPlayers", new ListTag());
    ListTag records = new ListTag();
    records.add(first);
    records.add(duplicate);
    CompoundTag root = new CompoundTag();
    root.put("Territories", records);
    var store = Forge1201TerritorySnapshotStore.load(root);
    assertThrows(
        TerritorySnapshotIntegrityException.class,
        () -> store.inviteTerritory(first.getUUID("TerritoryID")));
  }

  @Test
  void inviteLookupRejectsMalformedAuthorizationEntries() {
    CompoundTag territory = validTerritory();
    ListTag members = new ListTag();
    members.add(net.minecraft.nbt.StringTag.valueOf("not-a-player-record"));
    territory.put("AuthorizedPlayers", members);
    ListTag records = new ListTag();
    records.add(territory);
    CompoundTag root = new CompoundTag();
    root.put("Territories", records);
    var store = Forge1201TerritorySnapshotStore.load(root);
    assertThrows(
        TerritorySnapshotIntegrityException.class,
        () -> store.inviteTerritory(territory.getUUID("TerritoryID")));
  }

  @Test
  void inviteLookupDistinguishesMissingFromMalformedRoot() {
    CompoundTag emptyRoot = new CompoundTag();
    assertTrue(
        Forge1201TerritorySnapshotStore.load(emptyRoot)
            .inviteTerritory(UUID.randomUUID())
            .isEmpty());
    CompoundTag malformed = new CompoundTag();
    malformed.putString("Territories", "not-a-list");
    assertThrows(
        TerritorySnapshotIntegrityException.class,
        () -> Forge1201TerritorySnapshotStore.load(malformed).inviteTerritory(UUID.randomUUID()));
  }

  @Test
  void duplicateTerritoryIsCreateFailedWithRepositoryDiagnostics() {
    CompoundTag first = validTerritory();
    first.put("AuthorizedPlayers", new ListTag());
    CompoundTag duplicate = first.copy();
    ListTag records = new ListTag();
    records.add(first);
    records.add(duplicate);
    CompoundTag root = new CompoundTag();
    root.put("Territories", records);
    var store = Forge1201TerritorySnapshotStore.load(root);
    List<String> stages = new java.util.ArrayList<>();
    var service =
        new TerritoryInviteRequestService(
            store::inviteTerritory,
            id -> java.util.Optional.of(new TerritoryInviteRequestService.Player(id, "Target")),
            new TerritoryInviteStore(),
            new TerritoryInviteRateLimiter(),
            UUID::randomUUID,
            (stage, inviter, territory, target, error) -> stages.add(stage),
            1200);
    UUID owner = first.getUUID("OwnerUUID");
    var outcome =
        service.create(owner, "Owner", first.getUUID("TerritoryID"), UUID.randomUUID(), 10);
    assertEquals(TerritoryInviteResult.CREATE_FAILED, outcome.result());
    assertEquals(List.of("repository"), stages);
  }

  @Test
  void removeMemberPreservesUnknownFieldsAndRemainingOrder() {
    CompoundTag territory = validTerritory();
    territory.putString("FutureField", "kept");
    ListTag members = new ListTag();
    UUID firstId = UUID.randomUUID(), targetId = UUID.randomUUID(), lastId = UUID.randomUUID();
    for (UUID id : List.of(firstId, targetId, lastId)) {
      CompoundTag member = new CompoundTag();
      member.putUUID("PlayerUUID", id);
      member.putString("PlayerName", id.equals(targetId) ? "Target" : "Other");
      member.putString("UnknownMemberField", "kept");
      members.add(member);
    }
    territory.put("AuthorizedPlayers", members);
    ListTag records = new ListTag();
    records.add(territory);
    CompoundTag root = new CompoundTag();
    root.put("Territories", records);
    root.putString("RootFuture", "kept");
    var store = new Forge1201TerritorySnapshotStore(root);
    var outcome =
        store.removeMember(
            territory.getUUID("TerritoryID"), territory.getUUID("OwnerUUID"), targetId);
    assertEquals(
        com.mo.economy_system.common.territory.TerritoryMemberRemovalService.RepositoryResult
            .REMOVED,
        outcome.result());
    assertEquals("Target", outcome.removedMember().targetPlayerName());
    CompoundTag saved = store.rawCopy();
    assertEquals("kept", saved.getString("RootFuture"));
    CompoundTag savedTerritory = saved.getList("Territories", Tag.TAG_COMPOUND).getCompound(0);
    assertEquals("kept", savedTerritory.getString("FutureField"));
    ListTag savedMembers = savedTerritory.getList("AuthorizedPlayers", Tag.TAG_COMPOUND);
    assertEquals(
        List.of(firstId, lastId),
        List.of(
            savedMembers.getCompound(0).getUUID("PlayerUUID"),
            savedMembers.getCompound(1).getUUID("PlayerUUID")));
    assertEquals("kept", savedMembers.getCompound(0).getString("UnknownMemberField"));
  }

  @Test
  void removeMemberDirtyFailureRestoresRawAndCache() {
    CompoundTag territory = validTerritory();
    UUID targetId = UUID.randomUUID();
    CompoundTag member = new CompoundTag();
    member.putUUID("PlayerUUID", targetId);
    member.putString("PlayerName", "Target");
    ListTag members = new ListTag();
    members.add(member);
    territory.put("AuthorizedPlayers", members);
    ListTag records = new ListTag();
    records.add(territory);
    CompoundTag root = new CompoundTag();
    root.put("Territories", records);
    AtomicInteger calls = new AtomicInteger();
    var store =
        new Forge1201TerritorySnapshotStore(
            root,
            () -> {
              if (calls.getAndIncrement() == 0) throw new IllegalStateException("dirty");
            });
    CompoundTag before = store.rawCopy();
    var outcome =
        store.removeMember(
            territory.getUUID("TerritoryID"), territory.getUUID("OwnerUUID"), targetId);
    assertEquals(
        com.mo.economy_system.common.territory.TerritoryMemberRemovalService.RepositoryResult
            .PERSIST_FAILED,
        outcome.result());
    assertEquals(before, store.rawCopy());
    assertEquals(2, calls.get());
  }

  @Test
  void removeMemberDetectsSilentPostDirtyRawCorruptionAndRollsBack() {
    CompoundTag territory = validTerritory();
    UUID targetId = UUID.randomUUID();
    CompoundTag member = new CompoundTag();
    member.putUUID("PlayerUUID", targetId);
    member.putString("PlayerName", "Target");
    ListTag members = new ListTag();
    members.add(member);
    territory.put("AuthorizedPlayers", members);
    ListTag records = new ListTag();
    records.add(territory);
    CompoundTag root = new CompoundTag();
    root.put("Territories", records);
    Forge1201TerritorySnapshotStore[] holder = new Forge1201TerritorySnapshotStore[1];
    AtomicInteger calls = new AtomicInteger();
    holder[0] =
        new Forge1201TerritorySnapshotStore(
            root,
            () -> {
              if (calls.getAndIncrement() == 0)
                holder[0].mutateRawForTest(tag -> tag.putString("SilentCorruption", "bad"));
            });
    CompoundTag before = holder[0].rawCopy();
    var outcome =
        holder[0].removeMember(
            territory.getUUID("TerritoryID"), territory.getUUID("OwnerUUID"), targetId);
    assertEquals(
        com.mo.economy_system.common.territory.TerritoryMemberRemovalService.RepositoryResult
            .PERSIST_FAILED,
        outcome.result());
    assertEquals(before, holder[0].rawCopy());
    assertEquals(2, calls.get());
  }

  @Test
  void removeMemberDetectsSilentPostDirtyCacheCorruptionAndRollsBack() {
    CompoundTag territory = validTerritory();
    UUID targetId = UUID.randomUUID();
    CompoundTag member = new CompoundTag();
    member.putUUID("PlayerUUID", targetId);
    member.putString("PlayerName", "Target");
    ListTag members = new ListTag();
    members.add(member);
    territory.put("AuthorizedPlayers", members);
    ListTag records = new ListTag();
    records.add(territory);
    CompoundTag root = new CompoundTag();
    root.put("Territories", records);
    Forge1201TerritorySnapshotStore[] holder = new Forge1201TerritorySnapshotStore[1];
    AtomicInteger calls = new AtomicInteger();
    holder[0] =
        new Forge1201TerritorySnapshotStore(
            root,
            () -> {
              if (calls.getAndIncrement() == 0) holder[0].replaceCacheForTest(List.of());
            });
    CompoundTag before = holder[0].rawCopy();
    var outcome =
        holder[0].removeMember(
            territory.getUUID("TerritoryID"), territory.getUUID("OwnerUUID"), targetId);
    assertEquals(
        com.mo.economy_system.common.territory.TerritoryMemberRemovalService.RepositoryResult
            .PERSIST_FAILED,
        outcome.result());
    assertEquals(before, holder[0].rawCopy());
    assertEquals(2, calls.get());
  }

  @Test
  void managementMutationsPreserveUnknownFieldsAndPublishEquivalentCache() {
    CompoundTag territory = validTerritory();
    territory.putString("FutureTerritory", "keep");
    territory.put("AuthorizedPlayers", new ListTag());
    territory.put("TerritoryBuffs", buffs(false, 0));
    CompoundTag root = new CompoundTag();
    root.putString("FutureRoot", "keep-root");
    ListTag records = new ListTag();
    records.add(territory);
    root.put("Territories", records);
    Forge1201TerritorySnapshotStore store = Forge1201TerritorySnapshotStore.load(root);
    UUID owner = territory.getUUID("OwnerUUID");
    UUID target = UUID.randomUUID();

    assertEquals(
        TerritoryManagementResult.SUCCESS,
        permission(store, territory.getUUID("TerritoryID"), owner, target, true));
    assertEquals(
        TerritoryManagementResult.SUCCESS,
        rule(store, territory.getUUID("TerritoryID"), owner,
            RuleAction.OPEN_CONTAINER, RuleLevel.OWNER_ONLY));
    assertEquals(
        TerritoryBuffTransactionService.RepositoryResult.SUCCESS,
        store.mutateBuff(
            territory.getUUID("TerritoryID"),
            owner,
            "economy_system:speed",
            false,
            0,
            true,
            0));
    assertEquals(
        TerritoryManagementResult.SUCCESS,
        transfer(store, territory.getUUID("TerritoryID"), owner, target));

    CompoundTag saved = store.rawCopy();
    assertEquals("keep-root", saved.getString("FutureRoot"));
    CompoundTag changed = saved.getList("Territories", Tag.TAG_COMPOUND).getCompound(0);
    assertEquals("keep", changed.getString("FutureTerritory"));
    assertEquals(target, changed.getUUID("OwnerUUID"));
    assertEquals("Target", changed.getString("OwnerName"));
    assertTrue(changed.getList("TerritoryBuffs", Tag.TAG_COMPOUND)
        .getCompound(0).getBoolean("unlocked"));
    assertEquals(
        RuleLevel.OWNER_ONLY,
        store.findOwned(territory.getUUID("TerritoryID")).rules().stream()
            .filter(rule -> rule.action() == RuleAction.OPEN_CONTAINER)
            .findFirst().orElseThrow().level());
  }

  @Test
  void backpointDirtyFailureRestoresRawStateBeforeRetry() {
    CompoundTag territory = validTerritory();
    CompoundTag oldTag = new CompoundTag();
    oldTag.putInt("BackX", 0);
    oldTag.putInt("BackY", 64);
    oldTag.putInt("BackZ", 0);
    territory.put("Backpoint", oldTag);
    CompoundTag root = root(territory);
    AtomicInteger calls = new AtomicInteger();
    Forge1201TerritorySnapshotStore store = new Forge1201TerritorySnapshotStore(
        root, () -> {
          if (calls.getAndIncrement() == 0) throw new IllegalStateException("dirty");
        });
    Position oldPoint = new Position(0, 64, 0);
    Position next = new Position(3, 70, 3);
    CompoundTag before = store.rawCopy();

    assertEquals(
        TerritoryBackpointService.RepositoryResult.PERSIST_FAILED,
        store.setBackpoint(
            territory.getUUID("TerritoryID"),
            territory.getUUID("OwnerUUID"),
            Optional.of(oldPoint),
            next));
    assertEquals(before, store.rawCopy());
    assertEquals(2, calls.get());
    assertEquals(
        TerritoryBackpointService.RepositoryResult.UPDATED,
        store.setBackpoint(
            territory.getUUID("TerritoryID"),
            territory.getUUID("OwnerUUID"),
            Optional.of(oldPoint),
            next));
    assertEquals(Optional.of(next), store.findOwned(territory.getUUID("TerritoryID")).backpoint());
  }

  @Test
  void managementDirtyFailureRestoresRawCacheAndAllowsRetry() {
    CompoundTag territory = validTerritory();
    territory.put("AuthorizedPlayers", new ListTag());
    CompoundTag root = new CompoundTag();
    ListTag records = new ListTag();
    records.add(territory);
    root.put("Territories", records);
    AtomicInteger calls = new AtomicInteger();
    Forge1201TerritorySnapshotStore store = new Forge1201TerritorySnapshotStore(
        root,
        () -> {
          if (calls.getAndIncrement() == 0) throw new IllegalStateException("dirty");
        });
    CompoundTag before = store.rawCopy();
    UUID target = UUID.randomUUID();
    assertEquals(
        TerritoryManagementResult.PERSIST_FAILED,
        permission(store, territory.getUUID("TerritoryID"), territory.getUUID("OwnerUUID"), target, true));
    assertEquals(before, store.rawCopy());
    assertEquals(2, calls.get());
    assertEquals(
        TerritoryManagementResult.SUCCESS,
        permission(store, territory.getUUID("TerritoryID"), territory.getUUID("OwnerUUID"), target, true));
  }

  @Test
  void managementDetectsSilentPostDirtyCorruptionAndRollsBack() {
    CompoundTag territory = validTerritory();
    CompoundTag root = new CompoundTag();
    ListTag records = new ListTag();
    records.add(territory);
    root.put("Territories", records);
    Forge1201TerritorySnapshotStore[] holder = new Forge1201TerritorySnapshotStore[1];
    AtomicInteger calls = new AtomicInteger();
    holder[0] = new Forge1201TerritorySnapshotStore(
        root,
        () -> {
          if (calls.getAndIncrement() == 0) {
            holder[0].mutateRawForTest(value -> value.putString("Corrupt", "yes"));
          }
        });
    CompoundTag before = holder[0].rawCopy();
    assertEquals(
        TerritoryManagementResult.PERSIST_FAILED,
        rule(holder[0], territory.getUUID("TerritoryID"), territory.getUUID("OwnerUUID"),
            RuleAction.PLACE_BLOCK, RuleLevel.OWNER_ONLY));
    assertEquals(before, holder[0].rawCopy());
    assertEquals(2, calls.get());
  }

  @Test
  void managementRejectsDuplicateRecordsAndChangedExpectedState() {
    CompoundTag territory = validTerritory();
    territory.put("TerritoryBuffs", buffs(false, 0));
    CompoundTag root = new CompoundTag();
    ListTag records = new ListTag();
    records.add(territory);
    records.add(territory.copy());
    root.put("Territories", records);
    Forge1201TerritorySnapshotStore duplicate = Forge1201TerritorySnapshotStore.load(root);
    assertEquals(
        TerritoryManagementResult.STATE_UNKNOWN,
        rule(duplicate, territory.getUUID("TerritoryID"), territory.getUUID("OwnerUUID"),
            RuleAction.PLACE_BLOCK, RuleLevel.OWNER_ONLY));

    records.remove(1);
    Forge1201TerritorySnapshotStore store = Forge1201TerritorySnapshotStore.load(root);
    assertEquals(
        TerritoryBuffTransactionService.RepositoryResult.BUFF_CHANGED,
        store.mutateBuff(
            territory.getUUID("TerritoryID"),
            territory.getUUID("OwnerUUID"),
            "economy_system:speed",
            true,
            0,
            true,
            1));
  }

  @Test
  void resizePrepareAndCommitPreserveUnknownNbtAndPublishBounds() {
    CompoundTag territory = validTerritory();
    territory.putString("FutureTerritory", "keep");
    CompoundTag backpoint = new CompoundTag();
    backpoint.putInt("BackX", 0);
    backpoint.putInt("BackY", 64);
    backpoint.putInt("BackZ", 0);
    territory.put("Backpoint", backpoint);
    CompoundTag root = root(territory);
    root.putString("FutureRoot", "keep-root");
    Forge1201TerritorySnapshotStore store = Forge1201TerritorySnapshotStore.load(root);

    var prepared = store.prepareResize(
        territory.getUUID("TerritoryID"),
        territory.getUUID("OwnerUUID"),
        "minecraft:overworld",
        new Position(-1, 70, -1),
        new Position(11, 70, 11));
    assertEquals(Forge1201TerritorySnapshotStore.ResizePrepareResult.READY, prepared.result());
    assertEquals(121, prepared.plan().oldArea());
    assertEquals(169, prepared.plan().newArea());
    assertEquals(960, prepared.plan().charge());
    assertEquals(
        Forge1201TerritorySnapshotStore.ResizeCommitResult.SUCCESS,
        store.commitResize(prepared.plan()));

    CompoundTag saved = store.rawCopy();
    assertEquals("keep-root", saved.getString("FutureRoot"));
    CompoundTag changed = saved.getList("Territories", Tag.TAG_COMPOUND).getCompound(0);
    assertEquals("keep", changed.getString("FutureTerritory"));
    assertEquals(-1, changed.getInt("X1"));
    assertEquals(70, changed.getInt("Y1"));
    assertEquals(11, changed.getInt("Z2"));
    assertEquals(-1, changed.getCompound("Backpoint").getInt("BackX"));
    assertEquals(
        new Position(-1, 70, -1),
        store.findOwned(territory.getUUID("TerritoryID")).summary().pos1());
  }

  @Test
  void resizeRejectsOverlapWrongAuthorityAndStalePlan() {
    CompoundTag first = validTerritory();
    CompoundTag second = validTerritory();
    second.putUUID("TerritoryID", UUID.randomUUID());
    second.putInt("X1", 20);
    second.putInt("X2", 30);
    second.putInt("Z1", 20);
    second.putInt("Z2", 30);
    CompoundTag root = new CompoundTag();
    ListTag records = new ListTag();
    records.add(first);
    records.add(second);
    root.put("Territories", records);
    Forge1201TerritorySnapshotStore store = Forge1201TerritorySnapshotStore.load(root);

    assertEquals(
        Forge1201TerritorySnapshotStore.ResizePrepareResult.OVERLAP,
        store.prepareResize(
                first.getUUID("TerritoryID"),
                first.getUUID("OwnerUUID"),
                "minecraft:overworld",
                new Position(0, 64, 0),
                new Position(20, 64, 20))
            .result());
    assertEquals(
        Forge1201TerritorySnapshotStore.ResizePrepareResult.NOT_OWNER,
        store.prepareResize(
                first.getUUID("TerritoryID"),
                UUID.randomUUID(),
                "minecraft:overworld",
                new Position(0, 64, 0),
                new Position(12, 64, 12))
            .result());
    assertEquals(
        Forge1201TerritorySnapshotStore.ResizePrepareResult.WRONG_DIMENSION,
        store.prepareResize(
                first.getUUID("TerritoryID"),
                first.getUUID("OwnerUUID"),
                "minecraft:the_nether",
                new Position(0, 64, 0),
                new Position(12, 64, 12))
            .result());

    var prepared = store.prepareResize(
        first.getUUID("TerritoryID"),
        first.getUUID("OwnerUUID"),
        "minecraft:overworld",
        new Position(0, 64, 0),
        new Position(12, 64, 12));
    assertEquals(Forge1201TerritorySnapshotStore.ResizePrepareResult.READY, prepared.result());
    assertEquals(
        TerritoryManagementResult.SUCCESS,
        rule(store, first.getUUID("TerritoryID"), first.getUUID("OwnerUUID"),
            RuleAction.PLACE_BLOCK, RuleLevel.OWNER_ONLY));
    assertEquals(
        Forge1201TerritorySnapshotStore.ResizeCommitResult.CHANGED,
        store.commitResize(prepared.plan()));
  }

  @Test
  void resizeDirtyFailureRestoresRawAndCacheForRetry() {
    CompoundTag territory = validTerritory();
    CompoundTag root = root(territory);
    AtomicInteger calls = new AtomicInteger();
    Forge1201TerritorySnapshotStore store = new Forge1201TerritorySnapshotStore(
        root,
        () -> {
          if (calls.getAndIncrement() == 0) throw new IllegalStateException("dirty");
        });
    CompoundTag before = store.rawCopy();
    var prepared = store.prepareResize(
        territory.getUUID("TerritoryID"),
        territory.getUUID("OwnerUUID"),
        "minecraft:overworld",
        new Position(0, 64, 0),
        new Position(12, 64, 12));
    assertEquals(
        Forge1201TerritorySnapshotStore.ResizeCommitResult.PERSIST_FAILED,
        store.commitResize(prepared.plan()));
    assertEquals(before, store.rawCopy());
    assertEquals(new Position(0, 64, 0), store.findOwned(territory.getUUID("TerritoryID")).summary().pos1());
    assertEquals(2, calls.get());
  }

  private static TerritoryManagementResult permission(
      Forge1201TerritorySnapshotStore store,
      UUID territoryId,
      UUID owner,
      UUID target,
      boolean allowed) {
    return TerritoryAdministrationService.permission(
        new UpdateTerritoryPermissionMessage(territoryId, target, allowed),
        owner,
        adminContext(store));
  }

  private static TerritoryManagementResult transfer(
      Forge1201TerritorySnapshotStore store,
      UUID territoryId,
      UUID owner,
      UUID target) {
    return TerritoryAdministrationService.transfer(
        new TransferTerritoryOwnershipMessage(territoryId, target),
        owner,
        adminContext(store));
  }

  private static TerritoryManagementResult rule(
      Forge1201TerritorySnapshotStore store,
      UUID territoryId,
      UUID owner,
      RuleAction action,
      RuleLevel level) {
    return TerritoryAdministrationService.rule(
        new UpdateTerritoryRuleMessage(territoryId, action, level),
        owner,
        adminContext(store));
  }

  private static TerritoryAdministrationService.Context adminContext(
      Forge1201TerritorySnapshotStore store) {
    return new TerritoryAdministrationService.Context(
        new TerritoryAdministrationService.Repository() {
          @Override
          public com.mo.economy_system.common.territory.TerritorySnapshots.Owned find(UUID id) {
            return store.findOwned(id);
          }

          @Override
          public TerritoryAdministrationService.RepositoryResult apply(
              com.mo.economy_system.common.territory.TerritorySnapshots.Owned expected,
              com.mo.economy_system.common.territory.TerritorySnapshots.Owned replacement) {
            return store.applyAdministration(expected, replacement);
          }
        },
        id -> Optional.of("Target"),
        TerritoryAdministrationService.FailureSink.noop());
  }

  private static CompoundTag root(CompoundTag territory) {
    CompoundTag root = new CompoundTag();
    ListTag records = new ListTag();
    records.add(territory);
    root.put("Territories", records);
    return root;
  }

  private static ListTag buffs(boolean unlocked, int level) {
    CompoundTag buff = new CompoundTag();
    buff.putString("id", "economy_system:speed");
    buff.putString("displayText", "Speed");
    buff.putString("effectId", "minecraft:speed");
    buff.putBoolean("initialUnlockState", false);
    buff.putInt("initialLevel", 0);
    buff.putInt("single_Upgrade_Level", 1);
    buff.putInt("max_Level", 3);
    buff.putBoolean("unlocked", unlocked);
    buff.putInt("level", level);
    buff.put("upgrade_Cost", new ListTag());
    ListTag buffs = new ListTag();
    buffs.add(buff);
    return buffs;
  }

  private static CompoundTag validTerritory() {
    CompoundTag tag = new CompoundTag();
    tag.putUUID("TerritoryID", UUID.fromString("10000000-0000-0000-0000-000000000001"));
    tag.putUUID("OwnerUUID", UUID.fromString("00000000-0000-0000-0000-000000000001"));
    tag.putString("OwnerName", "Owner");
    tag.putString("Name", "Home");
    tag.putString("Dimension", "minecraft:overworld");
    tag.putInt("X1", 0);
    tag.putInt("Y1", 64);
    tag.putInt("Z1", 0);
    tag.putInt("X2", 10);
    tag.putInt("Y2", 80);
    tag.putInt("Z2", 10);
    return tag;
  }
}
