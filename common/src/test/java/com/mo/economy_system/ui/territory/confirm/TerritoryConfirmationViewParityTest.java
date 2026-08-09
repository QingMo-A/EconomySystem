package com.mo.economy_system.ui.territory.confirm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryConfirmationViewParityTest {
  @Test
  void bothTargetsReceiveTheSameTerritoryRemovalDecision() {
    TerritoryConfirmationState state = state(TerritoryConfirmationKind.REMOVE_TERRITORY);
    TerritoryConfirmationLayout.Layout layout = TerritoryConfirmationLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer forge = new RecordingEconomyUiRenderer();
    RecordingEconomyUiRenderer neoForge = new RecordingEconomyUiRenderer();

    TerritoryConfirmationView.render(forge, state, layout, layout.confirm().x(), layout.confirm().y());
    TerritoryConfirmationView.render(neoForge, state, layout, layout.confirm().x(), layout.confirm().y());

    assertEquals(forge.operations(), neoForge.operations());
    assertTrue(forge.operations().stream().anyMatch(operation ->
        operation.kind().equals("translatedTextInRect")
            && operation.value().startsWith("screen.territory.confirm.remove_body[spawn]")));
    assertTrue(forge.operations().stream().anyMatch(operation ->
        operation.kind().equals("translatedButton")
            && operation.value().startsWith("screen.territory.confirm.confirm") && operation.enabled()));
  }

  @Test
  void memberRemovalUsesMemberSpecificCopy() {
    TerritoryConfirmationState state = state(TerritoryConfirmationKind.REMOVE_MEMBER);
    TerritoryConfirmationLayout.Layout layout = TerritoryConfirmationLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    TerritoryConfirmationView.render(renderer, state, layout, 0, 0);

    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("translatedTextInRect")
            && operation.value().startsWith("screen.territory.confirm.member_body[alice]")));
  }

  private static TerritoryConfirmationState state(TerritoryConfirmationKind kind) {
    return new TerritoryConfirmationState(kind, new UUID(1, 1), "spawn",
        kind == TerritoryConfirmationKind.REMOVE_MEMBER ? new UUID(2, 2) : null, "alice",
        ScreenState.READY, Set.of(TerritoryConfirmationAction.values()));
  }
}
