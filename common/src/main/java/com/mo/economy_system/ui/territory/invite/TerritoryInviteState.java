package com.mo.economy_system.ui.territory.invite;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Immutable invite-directory state shared by both target shells. */
public record TerritoryInviteState(
    UUID territoryId,
    String territoryName,
    UUID ownerId,
    UUID viewerId,
    Set<UUID> existingMemberIds,
    List<PlayerSummary> players,
    String filter,
    int page,
    int pageSize,
    ScreenState screenState,
    String errorKey,
    long requestId,
    long playerRevision,
    long cooldownUntilTick) {
  public TerritoryInviteState {
    Objects.requireNonNull(territoryId, "territoryId");
    if (territoryName == null || territoryName.isBlank()) throw new IllegalArgumentException("territoryName");
    Objects.requireNonNull(ownerId, "ownerId");
    Objects.requireNonNull(viewerId, "viewerId");
    existingMemberIds = Set.copyOf(Objects.requireNonNull(existingMemberIds, "existingMemberIds"));
    if (existingMemberIds.contains(ownerId)) throw new IllegalArgumentException("owner is already a member");
    players = List.copyOf(Objects.requireNonNull(players, "players"));
    filter = Objects.requireNonNullElse(filter, "");
    Objects.requireNonNull(screenState, "screenState");
    if (page < 0 || pageSize < 1 || requestId < -1 || playerRevision < 0 || cooldownUntilTick < 0) {
      throw new IllegalArgumentException("invalid invite state");
    }
  }

  public List<PlayerSummary> eligiblePlayers() {
    String query = filter.trim().toLowerCase(Locale.ROOT);
    return players.stream()
        .filter(player -> !viewerId.equals(player.playerId()))
        .filter(player -> !ownerId.equals(player.playerId()))
        .filter(player -> !existingMemberIds.contains(player.playerId()))
        .filter(player -> query.isEmpty() || player.playerName().toLowerCase(Locale.ROOT).contains(query))
        .toList();
  }

  public int totalPages() {
    return Math.max(1, (eligiblePlayers().size() + pageSize - 1) / pageSize);
  }

  public List<PlayerSummary> visiblePlayers() {
    List<PlayerSummary> values = eligiblePlayers();
    int start = Math.min(page * pageSize, values.size());
    return values.subList(start, Math.min(values.size(), start + pageSize));
  }

  public boolean canInvite(UUID playerId) {
    return screenState == ScreenState.READY && playerId != null
        && visiblePlayers().stream().anyMatch(player -> player.playerId().equals(playerId));
  }
}
