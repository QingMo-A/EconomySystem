package com.mo.economy_system.ui.territory.detail;

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
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Exact detail/access/rules geometry, semantic labels and action styles from legacy screens. */
class TerritoryDetailLegacyReferenceParityTest {
  @Test
  void accessPageKeepsLocalizedTitlePlayerHeadsAndNativePaging() {
    TerritoryDetailState state = state(TerritoryDetailViewKind.ACCESS);
    TerritoryDetailLayout.Layout layout = TerritoryDetailLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    TerritoryDetailView.render(renderer, state, layout, 0, 0);

    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("scaledIconTranslatedText")
            && operation.value().contains("screen.territory.detail.access.title")));
    assertTrue(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("playerHead")));
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("icon") && operation.value().equals(UiIcon.ARROW_LEFT.name())
            && operation.rect().width() == 12 && operation.rect().height() == 12));
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("icon") && operation.value().equals(UiIcon.ARROW_RIGHT.name())
            && operation.rect().width() == 12 && operation.rect().height() == 12));
  }

  @Test
  void rulesPageUsesTranslatedRuleLabelsAndTerritoryActionStyle() {
    TerritoryDetailState state = state(TerritoryDetailViewKind.RULES);
    TerritoryDetailLayout.Layout layout = TerritoryDetailLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    TerritoryDetailView.render(renderer, state, layout, 0, 0);

    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("translatedTextInRect")
            && operation.value().startsWith("message.territory.rule.place_block")));
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("translatedButton")
            && operation.value().startsWith("message.territory.rule.level.owner_only")
            && operation.value().contains("accent="
                + com.mo.economy_system.ui.theme.EconomyUiTheme.TERRITORY_NEUTRAL_BUTTON.accent())));
  }

  private static TerritoryDetailState state(TerritoryDetailViewKind view) {
    Owned owned = new Owned(summary(), List.of(new Member(new UUID(2, 2), "alice")), Optional.empty(),
        rules(), List.of());
    List<PlayerSummary> players = List.of(new PlayerSummary(new UUID(2, 3), "bob"),
        new PlayerSummary(new UUID(2, 4), "cara"));
    return new TerritoryDetailState(owned, players, view, 0, 1, "", ScreenState.READY, null, -1, 0);
  }

  private static Summary summary() {
    return new Summary(new UUID(2, 1), new UUID(9, 9), "owner", "spawn",
        new Position(0, 64, 0), new Position(10, 70, 10), "minecraft:overworld");
  }

  private static List<Rule> rules() {
    return Arrays.stream(RuleAction.values())
        .map(action -> new Rule(action, RuleLevel.OWNER_ONLY)).toList();
  }
}
