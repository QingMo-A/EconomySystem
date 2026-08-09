package com.mo.economy_system.ui.transfer;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

/** Common semantic drawing for checked-file transfer completion and terminal outcomes. */
public final class TransferResultView {
  private TransferResultView() {}

  public static void render(
      EconomyUiRenderer renderer,
      TransferResultState state,
      TransferResultLayout.Layout layout,
      int mouseX,
      int mouseY) {
    renderer.fill(
        new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()), 0xB0000000);
    renderer.card(layout.card(), EconomyUiTheme.DELIVERY_CARD, layout.card().contains(mouseX, mouseY));
    renderer.icon(UiIcon.DELIVERY, new UiRect(layout.title().x(), layout.title().y() + 2, 14, 14));
    renderer.translatedTextInRect(
        state.terminal() ? "screen.transfer_terminal.title" : "screen.transfer_result.title",
        List.of(),
        new UiRect(layout.title().x() + 18, layout.title().y(), layout.title().width() - 18, layout.title().height()),
        EconomyUiTheme.TEXT_PRIMARY,
        UiTextAlignment.LEFT);
    if (state.terminal()) renderTerminal(renderer, state, layout);
    else renderArtifact(renderer, state, layout);
  }

  private static void renderTerminal(
      EconomyUiRenderer renderer, TransferResultState state, TransferResultLayout.Layout layout) {
    renderer.translatedTextInRect(
        state.terminalStatusKey(), List.of(), layout.details().get(0), EconomyUiTheme.TEXT_ERROR,
        UiTextAlignment.LEFT);
    renderer.translatedTextInRect(
        state.terminalErrorKey(), List.of(), layout.details().get(1), EconomyUiTheme.TEXT_ERROR,
        UiTextAlignment.LEFT);
    renderer.translatedButton(
        layout.close(), EconomyUiTheme.DISABLED_BUTTON, "button.transfer.close", List.of(), false,
        state.can(TransferResultAction.CLOSE));
  }

  private static void renderArtifact(
      EconomyUiRenderer renderer, TransferResultState state, TransferResultLayout.Layout layout) {
    List<UiRect> rows = layout.details();
    renderer.translatedTextInRect(
        "screen.transfer_result.source", List.of(state.sourceName()), rows.get(0),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect(
        "screen.transfer_result.type", List.of(state.checkTypeId()), rows.get(1),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect(
        "screen.transfer_result.file", List.of(state.fileName()), rows.get(2),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect(
        "screen.transfer_result.size", List.of(Long.toString(state.byteSize())), rows.get(3),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect(
        "screen.transfer_result.hash", List.of(state.sha256()), rows.get(4),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.LEFT);
    renderer.translatedTextInRect(
        state.artifactStateKey(), List.of(), rows.get(5), EconomyUiTheme.TEXT_SECONDARY,
        UiTextAlignment.LEFT);
    if (!state.actionErrorKey().isBlank()) {
      renderer.translatedTextInRect(
          state.actionErrorKey(), List.of(), rows.get(6), EconomyUiTheme.TEXT_ERROR,
          UiTextAlignment.LEFT);
    }
    renderer.translatedButton(
        layout.primary(), EconomyUiTheme.HOME_DELIVERY_BUTTON, "button.transfer.save", List.of(), false,
        state.can(TransferResultAction.SAVE));
    renderer.translatedButton(
        layout.secondary(), EconomyUiTheme.TERRITORY_DANGER_BUTTON, "button.transfer.discard", List.of(), false,
        state.can(TransferResultAction.DISCARD));
  }
}
