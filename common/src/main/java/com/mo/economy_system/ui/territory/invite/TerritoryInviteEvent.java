package com.mo.economy_system.ui.territory.invite;

import com.mo.economy_system.common.network.PlayerSummary;
import java.util.List;
import java.util.UUID;

public sealed interface TerritoryInviteEvent permits TerritoryInviteEvent.Initialize,
    TerritoryInviteEvent.PlayersLoaded, TerritoryInviteEvent.PlayersFailed,
    TerritoryInviteEvent.FilterChanged,
    TerritoryInviteEvent.ViewportChanged, TerritoryInviteEvent.Scroll,
    TerritoryInviteEvent.InviteClicked, TerritoryInviteEvent.Retry,
    TerritoryInviteEvent.Back, TerritoryInviteEvent.Tick {
  record Initialize(long nowNanos) implements TerritoryInviteEvent {}
  record PlayersLoaded(long requestId, long revision, List<PlayerSummary> players)
      implements TerritoryInviteEvent {
    public PlayersLoaded {
      if (requestId < 0 || revision < 0) throw new IllegalArgumentException("player response identity");
      players = List.copyOf(players);
    }
  }
  record PlayersFailed(long requestId, String errorKey) implements TerritoryInviteEvent {
    public PlayersFailed {
      if (requestId < 0) throw new IllegalArgumentException("requestId");
      errorKey = errorKey == null || errorKey.isBlank() ? "screen.invite.sync_failed" : errorKey;
    }
  }
  record FilterChanged(String value) implements TerritoryInviteEvent {}
  record ViewportChanged(int pageSize) implements TerritoryInviteEvent {
    public ViewportChanged { if (pageSize < 1) throw new IllegalArgumentException("pageSize"); }
  }
  record Scroll(int steps) implements TerritoryInviteEvent {}
  record InviteClicked(UUID playerId, long tick) implements TerritoryInviteEvent {}
  record Retry(long nowNanos) implements TerritoryInviteEvent {}
  record Back() implements TerritoryInviteEvent {}
  record Tick(long nowNanos) implements TerritoryInviteEvent {}
}
