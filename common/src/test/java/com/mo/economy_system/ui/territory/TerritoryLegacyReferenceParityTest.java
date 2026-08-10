package com.mo.economy_system.ui.territory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.territory.TerritorySnapshots.Member;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.common.territory.TerritorySnapshots.Rule;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailLayout;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailState;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailView;
import com.mo.economy_system.ui.territory.detail.TerritoryDetailViewKind;
import com.mo.economy_system.ui.territory.list.TerritoryListAction;
import com.mo.economy_system.ui.territory.list.TerritoryListLayout;
import com.mo.economy_system.ui.territory.list.TerritoryListRow;
import com.mo.economy_system.ui.territory.list.TerritoryListState;
import com.mo.economy_system.ui.territory.list.TerritoryListView;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Exact territory-list/detail chrome assertions transcribed from the legacy screens. */
class TerritoryLegacyReferenceParityTest {
  @Test
  void territoryListUsesReferenceSearchFrameAndLocalizedTitle() {
    TerritoryListState state = listState();
    TerritoryListLayout.Layout layout = TerritoryListLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();

    TerritoryListView.render(renderer, state, layout, 0, 0);

    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("inputFrame")
            && operation.rect().equals(layout.searchBackground())),
        "legacy territory search uses a four-edge input frame");
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("scaledIconTranslatedText")
            && operation.value().contains("screen.territory.title")),
        "territory title stays localized");
  }

  @Test
  void territoryListPagingUsesNativeArrowTextures() {
    TerritoryListState state = listState();
    TerritoryListLayout.Layout layout = TerritoryListLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();

    TerritoryListView.render(renderer, state, layout, 0, 0);

    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("icon") && operation.value().equals(UiIcon.ARROW_LEFT.name())
            && operation.rect().width() == 12 && operation.rect().height() == 12),
        "legacy previous page control renders the real 12px arrow texture");
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("icon") && operation.value().equals(UiIcon.ARROW_RIGHT.name())
            && operation.rect().width() == 12 && operation.rect().height() == 12),
        "legacy next page control renders the real 12px arrow texture");
    assertFalse(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("button")
            && (operation.value().startsWith("<:") || operation.value().startsWith(">:"))),
        "pagination must not fall back to literal angle brackets");
  }

  @Test
  void territoryDetailUsesLocalizedTitleAndNativePagingArrows() {
    TerritoryDetailState state = detailState();
    TerritoryDetailLayout.Layout layout = TerritoryDetailLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();

    TerritoryDetailView.render(renderer, state, layout, 0, 0);

    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("scaledIconTranslatedText")
            && operation.value().contains("screen.territory.detail.access.title")),
        "detail title remains localized");
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("icon") && operation.value().equals(UiIcon.ARROW_LEFT.name())
            && operation.rect().equals(new UiRect(layout.previousButton().x() + 19,
                layout.previousButton().y() + 4, 12, 12))),
        "detail previous control uses a 12px arrow texture");
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("icon") && operation.value().equals(UiIcon.ARROW_RIGHT.name())
            && operation.rect().equals(new UiRect(layout.nextButton().x() + 19,
                layout.nextButton().y() + 4, 12, 12))),
        "detail next control uses a 12px arrow texture");
  }

  private static TerritoryListState listState() {
    TerritoryListRow first = TerritoryListRow.owned(owned(new UUID(1, 1), "spawn"));
    TerritoryListRow second = TerritoryListRow.authorized(summary(new UUID(1, 2), "market"));
    return new TerritoryListState(List.of(first, second), 0, 1, "", ScreenState.READY, null, -1,
        Set.of(TerritoryListAction.values()));
  }

  private static TerritoryDetailState detailState() {
    Owned owned = new Owned(summary(new UUID(2, 1), "spawn"),
        List.of(new Member(new UUID(2, 2), "alice")), Optional.empty(),
        rules(), List.of());
    List<PlayerSummary> players = List.of(new PlayerSummary(new UUID(2, 3), "bob"),
        new PlayerSummary(new UUID(2, 4), "cara"));
    return new TerritoryDetailState(owned, players, TerritoryDetailViewKind.ACCESS, 0, 1, "",
        ScreenState.READY, null, -1, 0);
  }

  private static Summary summary(UUID id, String name) {
    return new Summary(id, new UUID(9, 9), "owner", name, new Position(0, 64, 0),
        new Position(10, 70, 10), "minecraft:overworld");
  }

  private static Owned owned(UUID id, String name) {
    return new Owned(summary(id, name), List.of(), Optional.empty(), rules(), List.of());
  }

  private static List<Rule> rules() {
    return Arrays.stream(RuleAction.values())
        .map(action -> new Rule(action, RuleLevel.OWNER_ONLY)).toList();
  }
}
