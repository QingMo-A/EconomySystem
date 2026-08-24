package com.mo.economy_system.ui.territory.invite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryInviteViewParityTest {
  @Test
  void bothTargetsReceiveTheSamePlayerAndInviteSemantics() {
    UUID target = new UUID(0, 4);
    TerritoryInviteState state = new TerritoryInviteState(
        new UUID(0, 1), "spawn", new UUID(0, 2), new UUID(0, 3), Set.of(),
        List.of(new PlayerSummary(target, "alice")), "", 0, 4,
        ScreenState.READY, null, -1, 3, 0);
    TerritoryInviteLayout.Layout layout = TerritoryInviteLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer forge = new RecordingEconomyUiRenderer();
    RecordingEconomyUiRenderer neoForge = new RecordingEconomyUiRenderer();

    TerritoryInviteView.render(forge, state, layout, 0, 0, 10);
    TerritoryInviteView.render(neoForge, state, layout, 0, 0, 10);

    assertEquals(forge.operations(), neoForge.operations());
    assertTrue(forge.operations().stream().anyMatch(operation ->
        operation.kind().equals("playerHead") && operation.value().contains("alice")));
    assertTrue(forge.operations().stream().anyMatch(operation ->
        operation.kind().equals("translatedButton")
            && operation.value().startsWith("button.invite.invite") && operation.enabled()));
    assertTrue(forge.operations().stream().anyMatch(operation ->
        operation.kind().equals("icon") && operation.value().equals(UiIcon.MEMBER.name())));
  }

  @Test
  void errorStateExposesRetryThroughTheCommonView() {
    TerritoryInviteState state = new TerritoryInviteState(
        new UUID(0, 1), "spawn", new UUID(0, 2), new UUID(0, 3), Set.of(),
        List.of(), "", 0, 4, ScreenState.ERROR, "screen.invite.sync_failed", -1, 0, 0);
    TerritoryInviteLayout.Layout layout = TerritoryInviteLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();

    TerritoryInviteView.render(renderer, state, layout,
        layout.retryButton().x(), layout.retryButton().y(), 0);

    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("translatedButton")
            && operation.value().startsWith("screen.invite.retry") && operation.enabled()));
  }
}
