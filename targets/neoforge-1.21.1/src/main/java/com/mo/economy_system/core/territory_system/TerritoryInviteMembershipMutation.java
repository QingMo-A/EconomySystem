package com.mo.economy_system.core.territory_system;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.territory.TerritoryInviteDecisionService.WriteResult;
import java.util.Objects;
import java.util.UUID;

/** Package-local transaction core for authoritative invitation membership writes. */
final class TerritoryInviteMembershipMutation {
    interface DirtyMarker { void markDirty(); }

    interface Membership {
        UUID ownerId();
        boolean contains(UUID playerId);
        long count(UUID playerId);
        boolean add(UUID playerId, String playerName);
        void remove(UUID playerId);
    }

    private TerritoryInviteMembershipMutation() {}

    static WriteResult mutate(Territory territory, UUID expectedOwner, UUID playerId,
            String playerName, DirtyMarker dirtyMarker) {
        Objects.requireNonNull(territory, "territory");
        return mutate(new Membership() {
            public UUID ownerId() { return territory.getOwnerUUID(); }
            public boolean contains(UUID id) { return territory.hasPermission(id); }
            public long count(UUID id) { return territory.authorizedPlayerCount(id); }
            public boolean add(UUID id, String name) {
                return territory.addAuthorizedPlayerIfAbsent(id, name);
            }
            public void remove(UUID id) { territory.removeAuthorizedPlayer(id); }
        }, expectedOwner, playerId, playerName, dirtyMarker);
    }

    static WriteResult mutate(Membership membership, UUID expectedOwner, UUID playerId,
            String playerName, DirtyMarker dirtyMarker) {
        if (membership == null || expectedOwner == null || playerId == null || playerName == null
                || playerName.isBlank()
                || playerName.length() > EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH
                || dirtyMarker == null) {
            return WriteResult.PERSIST_FAILED;
        }
        if (!expectedOwner.equals(membership.ownerId())) return WriteResult.OWNER_CHANGED;
        if (expectedOwner.equals(playerId)) return WriteResult.OWNER_CHANGED;
        if (membership.contains(playerId)) return WriteResult.ALREADY_MEMBER;
        try {
            if (!membership.add(playerId, playerName)
                    || !membership.contains(playerId) || membership.count(playerId) != 1) {
                return compensate(membership, playerId, dirtyMarker);
            }
            dirtyMarker.markDirty();
            return WriteResult.ADDED;
        } catch (RuntimeException failure) {
            if (!membership.contains(playerId) && membership.count(playerId) == 0) {
                return WriteResult.PERSIST_FAILED;
            }
            return compensate(membership, playerId, dirtyMarker);
        }
    }

    private static WriteResult compensate(Membership membership, UUID playerId,
            DirtyMarker dirtyMarker) {
        try {
            membership.remove(playerId);
            if (membership.contains(playerId) || membership.count(playerId) != 0) {
                return WriteResult.STATE_UNKNOWN;
            }
            dirtyMarker.markDirty();
            return WriteResult.PERSIST_FAILED;
        } catch (RuntimeException rollbackFailure) {
            return WriteResult.STATE_UNKNOWN;
        }
    }
}
