package com.mo.economy_system.ui.territory.confirm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Exact destructive-confirmation copy and danger/cancel button styles from legacy screens. */
class TerritoryConfirmationLegacyReferenceParityTest {
  @Test
  void territoryRemovalUsesDangerConfirmationAndTranslatedWarning() {
    TerritoryConfirmationState state = state(TerritoryConfirmationKind.REMOVE_TERRITORY);
    TerritoryConfirmationLayout.Layout layout = TerritoryConfirmationLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    TerritoryConfirmationView.render(renderer, state, layout, 0, 0);

    assertTrue(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("translatedTextInRect")
        && operation.value().startsWith("screen.territory.confirm.remove_body[spawn]")));
    assertTrue(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("translatedButton")
        && operation.value().startsWith("screen.territory.confirm.confirm")
        && operation.value().contains("accent=" + EconomyUiTheme.TERRITORY_DANGER_BUTTON.accent())));
    assertTrue(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("translatedButton")
        && operation.value().startsWith("screen.territory.confirm.cancel")
        && operation.value().contains("accent=" + EconomyUiTheme.DISABLED_BUTTON.accent())));
  }

  @Test
  void memberRemovalKeepsMemberSpecificCopy() {
    TerritoryConfirmationState state = state(TerritoryConfirmationKind.REMOVE_MEMBER);
    TerritoryConfirmationLayout.Layout layout = TerritoryConfirmationLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    TerritoryConfirmationView.render(renderer, state, layout, 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("translatedTextInRect")
        && operation.value().startsWith("screen.territory.confirm.member_body[alice]")));
  }

  private static TerritoryConfirmationState state(TerritoryConfirmationKind kind) {
    return new TerritoryConfirmationState(kind, new UUID(6, 1), "spawn",
        kind == TerritoryConfirmationKind.REMOVE_MEMBER ? new UUID(6, 2) : null, "alice",
        ScreenState.READY, Set.of(TerritoryConfirmationAction.values()));
  }
}
