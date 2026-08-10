package com.mo.economy_system.ui.transfer;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

/** Common semantic rendering for a checked-file transfer consent request. */
public final class TransferConsentView {
  private TransferConsentView() {}

  public static void render(
      EconomyUiRenderer renderer,
      TransferConsentState state,
      TransferConsentLayout.Layout layout,
      int mouseX,
      int mouseY) {
    renderer.fill(
        new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()),
        TransferConsentLayout.BACKGROUND_COLOR);
    renderer.translatedTextInRect(
        "screen.transfer_consent.title",
        List.of(),
        layout.title(),
        EconomyUiTheme.TEXT_PRIMARY,
        UiTextAlignment.CENTER);
    List<UiRect> rows = layout.details();
    renderer.translatedTextInRect(
        "screen.transfer_consent.requester", List.of(state.requesterName()), rows.get(0),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect(
        "screen.transfer_consent.type", List.of(state.checkTypeId()), rows.get(1),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect(
        "screen.transfer_consent.file", List.of(state.fileName()), rows.get(2),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect(
        "screen.transfer_consent.size", List.of(Long.toString(state.byteSize())), rows.get(3),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect(
        "screen.transfer_consent.hash", List.of(state.sha256()), rows.get(4),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.LEFT);
    renderer.translatedTextInRect(
        "screen.transfer_consent.warning", List.of(), layout.warning(), EconomyUiTheme.TEXT_MUTED,
        UiTextAlignment.LEFT);
    renderer.translatedButton(
        layout.allow(), EconomyUiTheme.HOME_DELIVERY_BUTTON, "button.transfer.allow", List.of(),
        layout.allow().contains(mouseX, mouseY), state.can(TransferConsentAction.ALLOW));
    renderer.translatedButton(
        layout.decline(), EconomyUiTheme.TERRITORY_DANGER_BUTTON, "button.transfer.decline", List.of(),
        layout.decline().contains(mouseX, mouseY), state.can(TransferConsentAction.DECLINE));
  }
}
