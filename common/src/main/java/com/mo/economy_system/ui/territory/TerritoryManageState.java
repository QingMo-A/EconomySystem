package com.mo.economy_system.ui.territory;

import com.mo.economy_system.ui.core.ScreenState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Immutable state rendered by both target Screen shells. */
public record TerritoryManageState(
        UUID territoryId,
        String territoryName,
        UUID ownerId,
        String ownerName,
        List<MemberRow> members,
        int page,
        int pageSize,
        int scrollOffset,
        String filter,
        ScreenState screenState,
        String errorKey,
        long requestId,
        Set<TerritoryManageAction> actions) {
    public TerritoryManageState {
        Objects.requireNonNull(territoryId, "territoryId");
        Objects.requireNonNull(ownerId, "ownerId");
        if (territoryName == null || territoryName.isBlank()
                || ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException("territory and owner names cannot be blank");
        }
        if (page < 0 || pageSize < 1 || scrollOffset < 0 || requestId < -1) {
            throw new IllegalArgumentException("invalid territory page state");
        }
        members = List.copyOf(new ArrayList<>(Objects.requireNonNull(members, "members")));
        filter = Objects.requireNonNullElse(filter, "");
        screenState = Objects.requireNonNull(screenState, "screenState");
        actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
    }

    public List<MemberRow> filteredMembers() {
        String needle = filter.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) return members;
        return members.stream().filter(member ->
                member.playerName().toLowerCase(Locale.ROOT).contains(needle)
                        || member.playerId().toString().contains(needle)).toList();
    }

    public int totalPages() {
        return Math.max(1, (filteredMembers().size() + pageSize - 1) / pageSize);
    }

    public List<MemberRow> visibleMembers() {
        List<MemberRow> filtered = filteredMembers();
        int start = Math.min(page * pageSize, filtered.size());
        return filtered.subList(start, Math.min(filtered.size(), start + pageSize));
    }

    public int maxScrollOffset() {
        return Math.max(0, filteredMembers().size() - pageSize);
    }

    public boolean can(TerritoryManageAction action) {
        return actions.contains(action);
    }
}
