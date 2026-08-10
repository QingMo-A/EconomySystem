package com.mo.economy_system.ui.check;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

/** Common rendering semantics for the client file-check consent prompt. */
public final class CheckConsentView {
  private CheckConsentView() {}

  public static void render(
      EconomyUiRenderer renderer,
      CheckConsentState state,
      CheckConsentLayout.Layout layout,
      int mouseX,
      int mouseY) {
    renderer.fill(
        new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()),
        CheckConsentLayout.BACKGROUND_COLOR);
    renderer.card(layout.card(), EconomyUiTheme.TERRITORY_CARD, layout.card().contains(mouseX, mouseY));
    renderer.icon(UiIcon.MANAGE, new UiRect(layout.title().x(), layout.title().y() + 2, 14, 14));
    renderer.translatedTextInRect(
        "screen.check_consent.title",
        List.of(),
        new UiRect(layout.title().x() + 18, layout.title().y(), layout.title().width() - 18, layout.title().height()),
        EconomyUiTheme.TEXT_PRIMARY,
        UiTextAlignment.LEFT);
    renderer.translatedTextInRect(
        "screen.check_consent.requester",
        List.of(state.requesterName()),
        layout.requester(),
        EconomyUiTheme.TEXT_SECONDARY,
        UiTextAlignment.CENTER);
    renderer.translatedTextInRect(
        "screen.check_consent.type",
        List.of(state.checkTypeId()),
        layout.type(),
        EconomyUiTheme.TEXT_SECONDARY,
        UiTextAlignment.CENTER);
    renderer.translatedTextInRect(
        "screen.check_consent.folder",
        List.of(state.checkTypeId()),
        layout.folder(),
        EconomyUiTheme.TEXT_SECONDARY,
        UiTextAlignment.CENTER);
    renderer.translatedTextInRect(
        "screen.check_consent.data_notice",
        List.of(),
        layout.dataNotice(),
        EconomyUiTheme.TEXT_MUTED,
        UiTextAlignment.CENTER);
    renderer.translatedTextInRect(
        "screen.check_consent.no_content_notice",
        List.of(),
        layout.noContentNotice(),
        EconomyUiTheme.TEXT_MUTED,
        UiTextAlignment.CENTER);
    renderer.translatedButton(
        layout.allow(),
        EconomyUiTheme.HOME_DELIVERY_BUTTON,
        "button.check_consent.allow",
        List.of(),
        layout.allow().contains(mouseX, mouseY),
        state.can(CheckConsentAction.ALLOW));
    renderer.translatedButton(
        layout.decline(),
        EconomyUiTheme.TERRITORY_DANGER_BUTTON,
        "button.check_consent.decline",
        List.of(),
        layout.decline().contains(mouseX, mouseY),
        state.can(CheckConsentAction.DECLINE));
  }
}
