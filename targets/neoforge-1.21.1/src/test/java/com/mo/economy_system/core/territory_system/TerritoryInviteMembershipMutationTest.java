package com.mo.economy_system.core.territory_system;

import static com.mo.economy_system.common.territory.TerritoryInviteDecisionService.WriteResult.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class TerritoryInviteMembershipMutationTest {
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID PLAYER = UUID.randomUUID();

    @Test void unavailableAuthorityDoesNotMutate() {
        Fake membership = new Fake();
        assertEquals(PERSIST_FAILED, TerritoryInviteMembershipMutation.mutate(
                membership, OWNER, PLAYER, "Player", null));
        assertFalse(membership.contains(PLAYER));
    }

    @Test void validatesOwnerAndExistingMembership() {
        Fake membership = new Fake();
        assertEquals(OWNER_CHANGED, TerritoryInviteMembershipMutation.mutate(
                membership, UUID.randomUUID(), PLAYER, "Player", () -> {}));
        membership.members.add(PLAYER);
        assertEquals(ALREADY_MEMBER, TerritoryInviteMembershipMutation.mutate(
                membership, OWNER, PLAYER, "Player", () -> {}));
    }

    @Test void addsExactlyOnceAndMarksDirty() {
        Fake membership = new Fake();
        int[] dirty = {0};
        assertEquals(ADDED, TerritoryInviteMembershipMutation.mutate(
                membership, OWNER, PLAYER, "Player", () -> dirty[0]++));
        assertEquals(1, membership.count(PLAYER));
        assertEquals(1, dirty[0]);
    }

    @Test void dirtyFailureRollsBackAndMarksRollbackDirty() {
        Fake membership = new Fake();
        int[] calls = {0};
        assertEquals(PERSIST_FAILED, TerritoryInviteMembershipMutation.mutate(
                membership, OWNER, PLAYER, "Player", () -> {
                    if (calls[0]++ == 0) throw new IllegalStateException("first dirty");
                }));
        assertFalse(membership.contains(PLAYER));
        assertEquals(2, calls[0]);
    }

    @Test void failedRollbackOrSecondDirtyIsUnknown() {
        Fake cannotRemove = new Fake();
        cannotRemove.removeFails = true;
        assertEquals(STATE_UNKNOWN, TerritoryInviteMembershipMutation.mutate(
                cannotRemove, OWNER, PLAYER, "Player", () -> { throw new IllegalStateException(); }));

        Fake secondDirty = new Fake();
        assertEquals(STATE_UNKNOWN, TerritoryInviteMembershipMutation.mutate(
                secondDirty, OWNER, PLAYER, "Player", () -> { throw new IllegalStateException(); }));
    }

    @Test void addFailureDistinguishesNoWriteFromPartialWrite() {
        Fake noWrite = new Fake();
        noWrite.addFails = true;
        assertEquals(PERSIST_FAILED, TerritoryInviteMembershipMutation.mutate(
                noWrite, OWNER, PLAYER, "Player", () -> {}));

        Fake partial = new Fake();
        partial.addFails = true;
        partial.partialAdd = true;
        assertEquals(PERSIST_FAILED, TerritoryInviteMembershipMutation.mutate(
                partial, OWNER, PLAYER, "Player", () -> {}));
        assertFalse(partial.contains(PLAYER));
    }

    @Test void productionTerritoryMutationSurvivesNbtReload() {
        Territory territory = new Territory(UUID.randomUUID(), "Home", OWNER, "Owner",
                0, 64, 0, 10, 80, 10, new BlockPos(1, 65, 1), Level.OVERWORLD);
        assertEquals(ADDED, TerritoryInviteMembershipMutation.mutate(
                territory, OWNER, PLAYER, "Player", () -> {}));
        Territory reloaded = Territory.fromNBT(territory.toNBT());
        assertTrue(reloaded.hasPermission(PLAYER));
        assertEquals(1, reloaded.authorizedPlayerCount(PLAYER));
    }

    private static final class Fake implements TerritoryInviteMembershipMutation.Membership {
        final List<UUID> members = new ArrayList<>();
        boolean addFails;
        boolean partialAdd;
        boolean removeFails;
        public UUID ownerId() { return OWNER; }
        public boolean contains(UUID id) { return members.contains(id); }
        public long count(UUID id) { return members.stream().filter(id::equals).count(); }
        public boolean add(UUID id, String name) {
            if (partialAdd) members.add(id);
            if (addFails) throw new IllegalStateException("add");
            return members.add(id);
        }
        public void remove(UUID id) {
            if (removeFails) throw new IllegalStateException("remove");
            members.removeIf(id::equals);
        }
    }
}
