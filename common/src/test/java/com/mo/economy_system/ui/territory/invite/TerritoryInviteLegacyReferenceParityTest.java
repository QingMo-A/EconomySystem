package com.mo.economy_system.ui.territory.invite;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Exact invite-directory search, row order, action style and pagination assertions. */
class TerritoryInviteLegacyReferenceParityTest {
  @Test
  void inviteDirectoryUsesTerritorySearchAndActionChrome() {
    TerritoryInviteState state = state(0);
    TerritoryInviteLayout.Layout layout = TerritoryInviteLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    TerritoryInviteView.render(renderer, state, layout, 0, 0, 10);

    assertTrue(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("inputFrame")
        && operation.rect().equals(new com.mo.economy_system.ui.geometry.UiRect(
            layout.search().x() - 4, layout.search().y() - 2,
            layout.search().width() + 8, layout.search().height() + 4))),
        "legacy invite search uses the territory input frame");
    assertTrue(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("playerHead")
        && operation.value().contains("alice")), "invite rows preserve player-head identity");
    assertTrue(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("translatedButton")
        && operation.value().startsWith("button.invite.invite")
        && operation.value().contains("accent=" + EconomyUiTheme.TERRITORY_BUTTON.accent())));
    assertTrue(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("translatedButton")
        && operation.value().startsWith("button.invite.back")
        && operation.value().contains("accent=" + EconomyUiTheme.HOME_ABOUT_BUTTON.accent())));
  }

  @Test
  void invitePagingUsesNativeArrows() {
    TerritoryInviteState state = state(1);
    TerritoryInviteLayout.Layout layout = TerritoryInviteLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    TerritoryInviteView.render(renderer, state, layout, 0, 0, 10);

    assertTrue(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("icon")
        && operation.value().equals(UiIcon.ARROW_LEFT.name()) && operation.rect().width() == 12
        && operation.rect().height() == 12));
    assertTrue(renderer.operations().stream().anyMatch(operation -> operation.kind().equals("icon")
        && operation.value().equals(UiIcon.ARROW_RIGHT.name()) && operation.rect().width() == 12
        && operation.rect().height() == 12));
  }

  private static TerritoryInviteState state(int page) {
    UUID owner = new UUID(5, 1);
    UUID viewer = new UUID(5, 2);
    return new TerritoryInviteState(new UUID(5, 3), "spawn", owner, viewer, Set.of(),
        List.of(new PlayerSummary(new UUID(5, 4), "alice"), new PlayerSummary(new UUID(5, 5), "bob")),
        "", page, 1, ScreenState.READY, null, -1, 0, 0);
  }
}
