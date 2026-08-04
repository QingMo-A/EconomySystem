package com.mo.economy_system.target.forge1201.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.common.territory.TerritoryTeleportTarget;
import com.mo.economy_system.common.territory.TerritoryInviteDecisionService;
import com.mo.economy_system.common.territory.TerritoryInviteRateLimiter;
import com.mo.economy_system.common.territory.TerritoryInviteRequestService;
import com.mo.economy_system.common.territory.TerritoryInviteResult;
import com.mo.economy_system.common.territory.TerritoryInviteStore;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

class Forge1201TerritorySnapshotStoreTest {
  @Test void permissionCompatibilityUsesMembersForMissingAndUnknownValues() {
    assertEquals(RuleLevel.OWNER_ONLY, Forge1201TerritorySnapshotStore.permission("OWNER_ONLY"));
    assertEquals(RuleLevel.MEMBERS, Forge1201TerritorySnapshotStore.permission("MEMBERS"));
    assertEquals(RuleLevel.EVERYONE, Forge1201TerritorySnapshotStore.permission("EVERYONE"));
    assertEquals(RuleLevel.MEMBERS, Forge1201TerritorySnapshotStore.permission(""));
    assertEquals(RuleLevel.MEMBERS, Forge1201TerritorySnapshotStore.permission("FUTURE"));
  }

  @Test void dimensionsMustBeCanonicalAndBounded() {
    assertEquals("minecraft:overworld",
        Forge1201TerritorySnapshotStore.canonicalDimension("minecraft:overworld"));
    assertEquals("example:moon", Forge1201TerritorySnapshotStore.canonicalDimension("example:moon"));
    assertThrows(IllegalArgumentException.class,
        () -> Forge1201TerritorySnapshotStore.canonicalDimension("overworld"));
    for (String invalid : new String[] {"", "bad id", "Minecraft:overworld", "a:b:c", "x".repeat(257)}) {
      assertThrows(IllegalArgumentException.class,
          () -> Forge1201TerritorySnapshotStore.canonicalDimension(invalid));
    }
  }

  @Test void captureFallsBackUnknownPermissionsWithoutChangingOtherData() {
    CompoundTag tag = validTerritory();
    CompoundTag permissions = new CompoundTag();
    permissions.putString(RuleAction.PLACE_BLOCK.name(), "OWNER_ONLY");
    permissions.putString(RuleAction.BREAK_BLOCK.name(), "FUTURE");
    tag.put("Permissions", permissions);
    var snapshot = Forge1201TerritorySnapshotStore.capture(tag);
    assertEquals(RuleLevel.OWNER_ONLY, snapshot.rules().stream()
        .filter(rule -> rule.action() == RuleAction.PLACE_BLOCK).findFirst().orElseThrow().level());
    assertEquals(RuleLevel.MEMBERS, snapshot.rules().stream()
        .filter(rule -> rule.action() == RuleAction.BREAK_BLOCK).findFirst().orElseThrow().level());
  }

  @Test void invalidBuffValuesFailClosedInsteadOfBeingClamped() {
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

  @Test void findBuildsOwnerAndAuthorizedTeleportTarget() {
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

  @Test void authorizeCopyOnWritePreservesUnknownNbtAndReloads() {
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
    assertEquals(TerritoryInviteDecisionService.WriteResult.ADDED,
        store.authorize(territory.getUUID("TerritoryID"), owner, member, "Member"));

    CompoundTag saved = store.save(new CompoundTag());
    assertEquals("keep-root", saved.getString("FutureRootField"));
    assertEquals("keep-me", saved.getList("Territories", Tag.TAG_COMPOUND)
        .getCompound(0).getString("FutureTerritoryField"));
    assertEquals(1, saved.getList("Territories", Tag.TAG_COMPOUND).getCompound(0)
        .getList("AuthorizedPlayers", Tag.TAG_COMPOUND).size());
    assertTrue(Forge1201TerritorySnapshotStore.load(saved).authorized(member).size() == 1);
  }

  @Test void authorizeRechecksOwnerAndRejectsDuplicateUuidFailClosed() {
    CompoundTag root = new CompoundTag();
    CompoundTag territory = validTerritory();
    territory.put("AuthorizedPlayers", new ListTag());
    ListTag territories = new ListTag();
    territories.add(territory);
    root.put("Territories", territories);
    var store = Forge1201TerritorySnapshotStore.load(root);
    assertEquals(TerritoryInviteDecisionService.WriteResult.OWNER_CHANGED,
        store.authorize(territory.getUUID("TerritoryID"), UUID.randomUUID(), UUID.randomUUID(), "x"));

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
    assertEquals(TerritoryInviteDecisionService.WriteResult.STATE_UNKNOWN,
        duplicateStore.authorize(territory.getUUID("TerritoryID"),
            territory.getUUID("OwnerUUID"), UUID.randomUUID(), "new"));
    assertEquals(2, duplicateStore.rawCopy().getList("Territories", Tag.TAG_COMPOUND)
        .getCompound(0).getList("AuthorizedPlayers", Tag.TAG_COMPOUND).size());
  }

  @Test void inviteLookupReadsRawAndFailsClosedForDuplicateTerritoryIds() {
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
    assertThrows(TerritorySnapshotIntegrityException.class,
        () -> store.inviteTerritory(first.getUUID("TerritoryID")));
  }

  @Test void inviteLookupRejectsMalformedAuthorizationEntries() {
    CompoundTag territory = validTerritory();
    ListTag members = new ListTag();
    members.add(net.minecraft.nbt.StringTag.valueOf("not-a-player-record"));
    territory.put("AuthorizedPlayers", members);
    ListTag records = new ListTag();
    records.add(territory);
    CompoundTag root = new CompoundTag();
    root.put("Territories", records);
    var store = Forge1201TerritorySnapshotStore.load(root);
    assertThrows(TerritorySnapshotIntegrityException.class,
        () -> store.inviteTerritory(territory.getUUID("TerritoryID")));
  }

  @Test void inviteLookupDistinguishesMissingFromMalformedRoot() {
    CompoundTag emptyRoot = new CompoundTag();
    assertTrue(Forge1201TerritorySnapshotStore.load(emptyRoot)
        .inviteTerritory(UUID.randomUUID()).isEmpty());
    CompoundTag malformed = new CompoundTag();
    malformed.putString("Territories", "not-a-list");
    assertThrows(TerritorySnapshotIntegrityException.class,
        () -> Forge1201TerritorySnapshotStore.load(malformed).inviteTerritory(UUID.randomUUID()));
  }

  @Test void duplicateTerritoryIsCreateFailedWithRepositoryDiagnostics() {
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
    var service = new TerritoryInviteRequestService(store::inviteTerritory,
        id -> java.util.Optional.of(new TerritoryInviteRequestService.Player(id, "Target")),
        new TerritoryInviteStore(), new TerritoryInviteRateLimiter(), UUID::randomUUID,
        (stage, inviter, territory, target, error) -> stages.add(stage), 1200);
    UUID owner = first.getUUID("OwnerUUID");
    var outcome = service.create(owner, "Owner", first.getUUID("TerritoryID"),
        UUID.randomUUID(), 10);
    assertEquals(TerritoryInviteResult.CREATE_FAILED, outcome.result());
    assertEquals(List.of("repository"), stages);
  }

  private static CompoundTag validTerritory() {
    CompoundTag tag = new CompoundTag();
    tag.putUUID("TerritoryID", UUID.fromString("10000000-0000-0000-0000-000000000001"));
    tag.putUUID("OwnerUUID", UUID.fromString("00000000-0000-0000-0000-000000000001"));
    tag.putString("OwnerName", "Owner");
    tag.putString("Name", "Home");
    tag.putString("Dimension", "minecraft:overworld");
    tag.putInt("X1", 0); tag.putInt("Y1", 64); tag.putInt("Z1", 0);
    tag.putInt("X2", 10); tag.putInt("Y2", 80); tag.putInt("Z2", 10);
    return tag;
  }
}
