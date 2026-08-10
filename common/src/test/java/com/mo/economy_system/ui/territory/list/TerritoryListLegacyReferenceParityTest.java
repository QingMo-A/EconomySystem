package com.mo.economy_system.ui.territory.list;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.common.territory.TerritorySnapshots.Rule;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Exact list-card geometry and chrome transcribed from legacy Screen_Territory. */
class TerritoryListLegacyReferenceParityTest {
  @Test
  void legacySearchFrameAndLocalizedTitleAreSemanticOperations() {
    TerritoryListState state = listState();
    TerritoryListLayout.Layout layout = TerritoryListLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    TerritoryListView.render(renderer, state, layout, 0, 0);

    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("inputFrame") && operation.rect().equals(layout.searchBackground())),
        "legacy territory search uses a four-edge input frame");
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("scaledIconTranslatedText")
            && operation.value().contains("screen.territory.title")),
        "legacy territory title stays localized");
  }

  @Test
  void legacyPagingUsesTerritoryStyleAndNativeArrowTextures() {
    TerritoryListState state = listState();
    TerritoryListLayout.Layout layout = TerritoryListLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    TerritoryListView.render(renderer, state, layout, 0, 0);

    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("button")
            && operation.value().contains("accent=" + (com.mo.economy_system.ui.theme.EconomyUiTheme.TERRITORY_ACCENT & 0x00FFFFFF))),
        "legacy list pagination uses the territory accent");
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("icon") && operation.value().equals(UiIcon.ARROW_LEFT.name())
            && operation.rect().width() == 12 && operation.rect().height() == 12));
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("icon") && operation.value().equals(UiIcon.ARROW_RIGHT.name())
            && operation.rect().width() == 12 && operation.rect().height() == 12));
    assertFalse(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("button")
        && (operation.value().startsWith("<:") || operation.value().startsWith(">:"))),
        "literal angle brackets are not a legacy arrow substitute");
  }

  private static TerritoryListState listState() {
    TerritoryListRow first = TerritoryListRow.owned(owned(new UUID(1, 1), "spawn"));
    TerritoryListRow second = TerritoryListRow.authorized(summary(new UUID(1, 2), "market"));
    return new TerritoryListState(List.of(first, second), 0, 1, "", ScreenState.READY, null, -1,
        Set.of(TerritoryListAction.values()));
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
