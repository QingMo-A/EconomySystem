package com.mo.economy_system.ui.territory.detail;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.territory.TerritorySnapshots.Member;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Rule;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Shared state machine for the unified territory management center. */
public final class TerritoryDetailController
    extends AbstractEconomyScreenController<TerritoryDetailState, TerritoryDetailEvent> {
  public static final long TIMEOUT_NANOS = 10_000_000_000L;

  private final TerritoryDetailPort port;
  private final UUID territoryId;
  private long startedAt;
  private boolean territoryInFlight;
  private Owned mutationRollback;
  private boolean terminalActionSubmitted;

  public TerritoryDetailController(Owned initial, TerritoryDetailPort port) {
    this(initial, List.of(), 0, port);
  }

  public TerritoryDetailController(
      Owned initial, List<PlayerSummary> players, long playerRevision, TerritoryDetailPort port) {
    super(new TerritoryDetailState(
        Objects.requireNonNull(initial, "initial"),
        Objects.requireNonNull(players, "players"),
        TerritoryDetailViewKind.MAIN,
        0,
        1,
        "",
        ScreenState.IDLE,
        null,
        -1,
        Math.max(0, playerRevision)));
    this.port = Objects.requireNonNull(port, "port");
    this.territoryId = initial.summary().territoryId();
  }

  /** Selects the requested section before the first request starts. */
  public void selectInitialView(TerritoryDetailViewKind view) {
    Objects.requireNonNull(view, "view");
    if (state().screenState() != ScreenState.IDLE) return;
    replaceState(copy(state().territory(), state().players(), view, 0, state().pageSize(),
        "", state().screenState(), state().errorKey(), state().requestId(),
        state().playerRevision()));
  }

  @Override
  public void handle(TerritoryDetailEvent event) {
    Objects.requireNonNull(event, "event");
    if (event instanceof TerritoryDetailEvent.Initialize value) {
      request(value.nowNanos());
    } else if (event instanceof TerritoryDetailEvent.Retry value) {
      request(value.nowNanos());
    } else if (event instanceof TerritoryDetailEvent.TerritoryLoaded value) {
      loaded(value);
    } else if (event instanceof TerritoryDetailEvent.TerritoryFailed value) {
      failed(value);
    } else if (event instanceof TerritoryDetailEvent.PlayersLoaded value) {
      playersLoaded(value);
    } else if (event instanceof TerritoryDetailEvent.ViewSelected value) {
      selectView(value.view());
    } else if (event instanceof TerritoryDetailEvent.FilterChanged value) {
      filter(value.value());
    } else if (event instanceof TerritoryDetailEvent.ViewportChanged value) {
      viewport(value.pageSize());
    } else if (event instanceof TerritoryDetailEvent.Scroll value) {
      page(Integer.signum(value.steps()));
    } else if (event instanceof TerritoryDetailEvent.RuleLevelClicked value) {
      setRuleLevel(value.action(), value.level(), value.nowNanos());
    } else if (event instanceof TerritoryDetailEvent.PresetClicked value) {
      applyPreset(value.preset(), value.nowNanos());
    } else if (event instanceof TerritoryDetailEvent.RuleClicked value) {
      cycleRule(value.action(), value.nowNanos());
    } else if (event instanceof TerritoryDetailEvent.ActionClicked value) {
      action(value);
    } else if (event instanceof TerritoryDetailEvent.Tick value) {
      tick(value.nowNanos());
    }
  }

  private void request(long nowNanos) {
    long requestId = port.nextRequestId();
    if (requestId < 0) throw new IllegalStateException("territory detail request id must be non-negative");
    startedAt = nowNanos;
    territoryInFlight = true;
    replaceState(copy(
        state().territory(), state().players(), state().view(), state().scroll(), state().pageSize(),
        state().filter(), ScreenState.LOADING, null, requestId, state().playerRevision()));
    port.requestTerritory(territoryId, requestId);
    port.requestPlayers();
  }

  private void loaded(TerritoryDetailEvent.TerritoryLoaded event) {
    if (!territoryInFlight || state().screenState() != ScreenState.LOADING
        || state().requestId() != event.requestId()) return;
    territoryInFlight = false;
    mutationRollback = null;
    Owned value = Objects.requireNonNull(event.territory(), "territory");
    replaceState(copy(value, state().players(), state().view(),
        clampPageFor(value, state().view(), state().scroll(), state().pageSize()), state().pageSize(),
        state().filter(), ScreenState.READY, null, -1, state().playerRevision()));
  }

  private void failed(TerritoryDetailEvent.TerritoryFailed event) {
    if (!territoryInFlight || state().screenState() != ScreenState.LOADING
        || state().requestId() != event.requestId()) return;
    territoryInFlight = false;
    Owned territory = mutationRollback == null ? state().territory() : mutationRollback;
    mutationRollback = null;
    replaceState(copy(territory, state().players(), state().view(), state().scroll(),
        state().pageSize(), state().filter(), ScreenState.ERROR,
        event.errorKey() == null || event.errorKey().isBlank()
            ? "screen.territory.detail.sync_failed" : event.errorKey(),
        -1, state().playerRevision()));
  }

  private void playersLoaded(TerritoryDetailEvent.PlayersLoaded event) {
    if (event.revision() < state().playerRevision()) return;
    List<PlayerSummary> players = Objects.requireNonNull(event.players(), "players");
    TerritoryDetailState next = copy(state().territory(), players, state().view(),
        state().scroll(), state().pageSize(), state().filter(), state().screenState(),
        state().errorKey(), state().requestId(), event.revision());
    int page = Math.max(0, Math.min(next.scroll(), next.totalPages() - 1));
    replaceState(copy(next.territory(), next.players(), next.view(), page, next.pageSize(),
        next.filter(), next.screenState(), next.errorKey(), next.requestId(), next.playerRevision()));
  }

  private void selectView(TerritoryDetailViewKind view) {
    Objects.requireNonNull(view, "view");
    if (state().screenState() != ScreenState.IDLE
        && state().screenState() != ScreenState.READY
        && state().screenState() != ScreenState.EMPTY
        && state().screenState() != ScreenState.LOADING
        && state().screenState() != ScreenState.ERROR) return;
    replaceState(copy(state().territory(), state().players(), view, 0, state().pageSize(),
        "", state().screenState(), state().errorKey(), state().requestId(),
        state().playerRevision()));
  }

  private void filter(String value) {
    String next = value == null ? "" : value;
    replaceState(copy(state().territory(), state().players(), state().view(), 0,
        state().pageSize(), next, state().screenState(), state().errorKey(),
        state().requestId(), state().playerRevision()));
  }

  private void viewport(int size) {
    int pageSize = Math.max(1, size);
    replaceState(copy(state().territory(), state().players(), state().view(),
        clampPage(state().scroll(), pageSize), pageSize, state().filter(), state().screenState(),
        state().errorKey(), state().requestId(), state().playerRevision()));
  }

  private void page(int delta) {
    if (delta == 0) return;
    replaceState(copy(state().territory(), state().players(), state().view(),
        clampPage(state().scroll() + delta), state().pageSize(), state().filter(),
        state().screenState(), state().errorKey(), state().requestId(), state().playerRevision()));
  }

  private void action(TerritoryDetailEvent.ActionClicked event) {
    TerritoryDetailAction action = event.action();
    if (action == null) return;
    if (action == TerritoryDetailAction.BACK) {
      if (state().view() == TerritoryDetailViewKind.TRANSFER) selectView(TerritoryDetailViewKind.SETTINGS);
      else navigate(new UiNavigation.Back());
      return;
    }
    if (action == TerritoryDetailAction.RETRY) return;
    if (!state().can(action)) return;
    switch (action) {
      case OVERVIEW -> selectView(TerritoryDetailViewKind.MAIN);
      case ACCESS -> selectView(TerritoryDetailViewKind.ACCESS);
      case RULES -> selectView(TerritoryDetailViewKind.RULES);
      case SETTINGS -> selectView(TerritoryDetailViewKind.SETTINGS);
      case BUFFS -> navigate(new UiNavigation.Target("territory-buffs"));
      case INVITE -> navigate(new UiNavigation.Target("territory-invite"));
      case COPY_ID -> port.copyTerritoryId(territoryId);
      case DELETE -> navigate(new UiNavigation.Target("territory-delete"));
      case RESIZE -> port.resize(territoryId);
      case TRANSFER -> selectView(TerritoryDetailViewKind.TRANSFER);
      case TOGGLE_ACCESS -> toggleAccess(event.targetId(), event.nowNanos());
      case TRANSFER_OWNERSHIP -> transfer(event.targetId());
      default -> { }
    }
  }

  private void cycleRule(RuleAction action, long nowNanos) {
    Rule current = state().territory().rules().stream()
        .filter(rule -> rule.action() == action).findFirst().orElse(null);
    if (current == null) return;
    RuleLevel next = switch (current.level()) {
      case OWNER_ONLY -> RuleLevel.MEMBERS;
      case MEMBERS -> RuleLevel.EVERYONE;
      case EVERYONE -> RuleLevel.OWNER_ONLY;
    };
    setRuleLevel(action, next, nowNanos);
  }

  private void setRuleLevel(RuleAction action, RuleLevel level, long nowNanos) {
    if (state().view() != TerritoryDetailViewKind.RULES || territoryInFlight) return;
    Rule current = state().territory().rules().stream()
        .filter(rule -> rule.action() == action).findFirst().orElse(null);
    if (current == null || current.level() == level) return;
    Owned previous = state().territory();
    Owned optimistic = replaceRule(previous, action, level);
    mutationRollback = previous;
    replaceState(copy(optimistic, state().players(), state().view(), state().scroll(), state().pageSize(),
        state().filter(), ScreenState.READY, null, -1, state().playerRevision()));
    port.submitRule(territoryId, action, level);
    request(nowNanos);
  }

  private void applyPreset(TerritoryRulePreset preset, long nowNanos) {
    if (state().view() != TerritoryDetailViewKind.RULES || territoryInFlight) return;
    Owned previous = state().territory();
    List<Rule> rules = previous.rules().stream()
        .map(rule -> new Rule(rule.action(), preset.levelFor(rule.action())))
        .toList();
    if (rules.equals(previous.rules())) return;
    Owned optimistic = new Owned(previous.summary(), previous.authorizedMembers(), previous.backpoint(),
        rules, previous.buffs());
    mutationRollback = previous;
    replaceState(copy(optimistic, state().players(), state().view(), state().scroll(), state().pageSize(),
        state().filter(), ScreenState.READY, null, -1, state().playerRevision()));
    for (Rule rule : rules) {
      Rule old = previous.rules().stream().filter(value -> value.action() == rule.action()).findFirst().orElseThrow();
      if (old.level() != rule.level()) port.submitRule(territoryId, rule.action(), rule.level());
    }
    request(nowNanos);
  }

  private void toggleAccess(UUID playerId, long nowNanos) {
    if (playerId == null || territoryInFlight) return;
    TerritoryAccessRow row = state().accessRows().stream()
        .filter(value -> value.playerId().equals(playerId)).findFirst().orElse(null);
    if (row == null || !row.allowed()) return;
    Owned previous = state().territory();
    List<Member> members = previous.authorizedMembers().stream()
        .filter(member -> !member.playerId().equals(playerId)).toList();
    Owned optimistic = new Owned(previous.summary(), members, previous.backpoint(), previous.rules(), previous.buffs());
    mutationRollback = previous;
    replaceState(copy(optimistic, state().players(), state().view(), state().scroll(), state().pageSize(),
        state().filter(), ScreenState.READY, null, -1, state().playerRevision()));
    port.submitAccess(territoryId, playerId, false);
    request(nowNanos);
  }

  private void transfer(UUID playerId) {
    if (terminalActionSubmitted || playerId == null || state().transferRows().stream()
        .noneMatch(value -> value.playerId().equals(playerId))) return;
    terminalActionSubmitted = true;
    port.submitTransfer(territoryId, playerId);
    navigate(new UiNavigation.Back());
  }

  private void tick(long nowNanos) {
    if (territoryInFlight && nowNanos - startedAt >= TIMEOUT_NANOS) {
      territoryInFlight = false;
      Owned territory = mutationRollback == null ? state().territory() : mutationRollback;
      mutationRollback = null;
      replaceState(copy(territory, state().players(), state().view(), state().scroll(),
          state().pageSize(), state().filter(), ScreenState.ERROR,
          "screen.territory.detail.sync_timeout", -1, state().playerRevision()));
    }
  }

  private static Owned replaceRule(Owned source, RuleAction action, RuleLevel level) {
    List<Rule> rules = source.rules().stream()
        .map(rule -> rule.action() == action ? new Rule(action, level) : rule)
        .toList();
    return new Owned(source.summary(), source.authorizedMembers(), source.backpoint(), rules, source.buffs());
  }

  private int clampPage(int page) { return clampPage(page, state().pageSize()); }

  private int clampPage(int page, int pageSize) {
    int pages = Math.max(1, (rowCount(state()) + pageSize - 1) / pageSize);
    return Math.max(0, Math.min(page, pages - 1));
  }

  private int clampPageFor(Owned territory, TerritoryDetailViewKind view, int page, int pageSize) {
    TerritoryDetailState probe = copy(territory, state().players(), view, 0, pageSize,
        state().filter(), state().screenState(), state().errorKey(), state().requestId(), state().playerRevision());
    int pages = Math.max(1, (probe.rowCount() + pageSize - 1) / pageSize);
    return Math.max(0, Math.min(page, pages - 1));
  }

  private static int rowCount(TerritoryDetailState state) { return state.rowCount(); }

  private TerritoryDetailState copy(
      Owned territory, List<PlayerSummary> players, TerritoryDetailViewKind view, int scroll,
      int pageSize, String filter, ScreenState screenState, String errorKey, long requestId,
      long playerRevision) {
    return new TerritoryDetailState(territory, players, view, Math.max(0, scroll),
        Math.max(1, pageSize), filter, screenState, errorKey, requestId,
        Math.max(0, playerRevision));
  }
}
