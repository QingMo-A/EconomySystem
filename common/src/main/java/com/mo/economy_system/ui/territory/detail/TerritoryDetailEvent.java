package com.mo.economy_system.ui.territory.detail;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import java.util.List;
import java.util.UUID;

public sealed interface TerritoryDetailEvent permits TerritoryDetailEvent.Initialize,
    TerritoryDetailEvent.TerritoryLoaded, TerritoryDetailEvent.TerritoryFailed,
    TerritoryDetailEvent.PlayersLoaded, TerritoryDetailEvent.ViewSelected,
    TerritoryDetailEvent.FilterChanged, TerritoryDetailEvent.ViewportChanged,
    TerritoryDetailEvent.Scroll, TerritoryDetailEvent.ActionClicked,
    TerritoryDetailEvent.RuleClicked, TerritoryDetailEvent.RuleLevelClicked,
    TerritoryDetailEvent.PresetClicked,
    TerritoryDetailEvent.Retry, TerritoryDetailEvent.Tick {
  record Initialize(long nowNanos) implements TerritoryDetailEvent {}
  record TerritoryLoaded(long requestId, Owned territory) implements TerritoryDetailEvent {}
  record TerritoryFailed(long requestId, String errorKey) implements TerritoryDetailEvent {}
  record PlayersLoaded(long revision, List<PlayerSummary> players) implements TerritoryDetailEvent {}
  record ViewSelected(TerritoryDetailViewKind view) implements TerritoryDetailEvent {}
  record FilterChanged(String value) implements TerritoryDetailEvent {}
  record ViewportChanged(int pageSize) implements TerritoryDetailEvent {
    public ViewportChanged { if (pageSize < 1) throw new IllegalArgumentException("pageSize"); }
  }
  record Scroll(int steps) implements TerritoryDetailEvent {}
  record ActionClicked(TerritoryDetailAction action, UUID targetId, long nowNanos)
      implements TerritoryDetailEvent {
    public ActionClicked(TerritoryDetailAction action, UUID targetId) {
      this(action, targetId, 0L);
    }
  }
  /** Compatibility event retained for old click-to-cycle shells. */
  record RuleClicked(RuleAction action, long nowNanos) implements TerritoryDetailEvent {
    public RuleClicked(RuleAction action) { this(action, 0L); }
    public RuleClicked { java.util.Objects.requireNonNull(action, "action"); }
  }
  record RuleLevelClicked(RuleAction action, RuleLevel level, long nowNanos)
      implements TerritoryDetailEvent {
    public RuleLevelClicked {
      java.util.Objects.requireNonNull(action, "action");
      java.util.Objects.requireNonNull(level, "level");
    }
  }
  record PresetClicked(TerritoryRulePreset preset, long nowNanos) implements TerritoryDetailEvent {
    public PresetClicked { java.util.Objects.requireNonNull(preset, "preset"); }
  }
  record Retry(long nowNanos) implements TerritoryDetailEvent {}
  record Tick(long nowNanos) implements TerritoryDetailEvent {}
}
