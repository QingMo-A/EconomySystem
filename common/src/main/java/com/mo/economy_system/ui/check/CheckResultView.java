package com.mo.economy_system.ui.check;

import com.mo.economy_system.common.check.ClientFileCheckResultController;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;
import java.util.Locale;

/** Semantic drawing for both loader implementations of checked-file results. */
public final class CheckResultView {
  private CheckResultView() {}

  public static void render(
      EconomyUiRenderer renderer,
      CheckResultState state,
      CheckResultLayout.Layout layout,
      int mouseX,
      int mouseY) {
    renderer.fill(
        new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()), 0xB0000000);
    renderer.icon(UiIcon.MANAGE, new UiRect(layout.title().x(), layout.title().y() + 2, 14, 14));
    renderer.translatedText(
        "screen.check_result.title", List.of(), layout.title().x() + 18, layout.title().y(), EconomyUiTheme.TEXT_PRIMARY);
    renderer.card(layout.searchCard(), EconomyUiTheme.DELIVERY_CARD, layout.searchCard().contains(mouseX, mouseY));
    renderer.translatedTextInRect(
        "screen.check_result.search", List.of(), layout.search(), EconomyUiTheme.TEXT_MUTED, UiTextAlignment.LEFT);
    renderer.card(layout.status(), EconomyUiTheme.TERRITORY_CARD, false);
    renderer.translatedText(
        "screen.check_result.target", List.of(state.targetName()), layout.status().x() + 10, layout.status().y() + 8,
        EconomyUiTheme.TEXT_SECONDARY);
    renderer.translatedText(
        "screen.check_result.type", List.of(state.checkTypeId()), layout.status().x() + 10, layout.status().y() + 22,
        EconomyUiTheme.TEXT_SECONDARY);
    renderer.translatedText(
        "screen.check_result.status_" + state.remoteStatus().name().toLowerCase(Locale.ROOT),
        List.of(), layout.status().x() + 10, layout.status().y() + 36, EconomyUiTheme.TEXT_MUTED);
    renderer.translatedText(
        "screen.check_result.counts",
        List.of(
            Integer.toString(state.remoteFileCount()),
            Integer.toString(state.remoteSkippedCount()),
            Long.toString(state.rows().stream().filter(CheckResultRow::skipped).count())),
        layout.status().x() + 10, layout.status().y() + 50, EconomyUiTheme.TEXT_MUTED);
    renderStatus(renderer, state, layout.status());
    renderer.card(layout.rows(), EconomyUiTheme.DELIVERY_CARD, false);
    int y = layout.rows().y() + 3;
    for (CheckResultRow row : state.visibleRows()) {
      UiRect line = new UiRect(layout.rows().x() + 6, y, layout.rows().width() - 12, 17);
      renderer.textInRect(row.fileName(), new UiRect(line.x(), line.y(), Math.max(28, line.width() / 2), line.height()),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
      renderer.translatedTextInRect(
          row.reasonKey(),
          List.of(),
          new UiRect(line.x() + line.width() / 2, line.y(), line.width() - line.width() / 2, line.height()),
          row.skipped() ? EconomyUiTheme.TEXT_MUTED : EconomyUiTheme.TEXT_SECONDARY,
          UiTextAlignment.RIGHT);
      y += 20;
    }
    renderer.translatedButton(
        layout.retry(),
        EconomyUiTheme.HOME_DELIVERY_BUTTON,
        "button.check_result.retry",
        List.of(),
        layout.retry().contains(mouseX, mouseY),
        state.can(CheckResultAction.RETRY));
    renderer.translatedButton(
        layout.back(),
        EconomyUiTheme.DISABLED_BUTTON,
        "button.transfer.close",
        List.of(),
        layout.back().contains(mouseX, mouseY),
        state.can(CheckResultAction.BACK));
  }

  private static void renderStatus(EconomyUiRenderer renderer, CheckResultState state, UiRect status) {
    int x = status.x() + status.width() / 2;
    int y = status.y() + 36;
    if (state.localState() != ClientFileCheckResultController.LocalState.NOT_REQUIRED
        && state.localState() != ClientFileCheckResultController.LocalState.READY) {
      String key = switch (state.localState()) {
        case LOADING -> "screen.check_result.loading";
        case BUSY -> "screen.check_result.local_scan_busy";
        case FAILED -> "screen.check_result.local_scan_failed";
        case READY_INCOMPLETE -> "screen.check_result.local_incomplete";
        default -> "screen.check_result.loading";
      };
      renderer.translatedTextInRect(
          key, List.of(), new UiRect(x, y, status.right() - x - 8, 14), EconomyUiTheme.TEXT_ERROR,
          UiTextAlignment.RIGHT);
    }
    if (state.remoteErrorCode() != null) {
      renderer.translatedTextInRect(
          "screen.check_result.error",
          List.of(state.remoteErrorCode()),
          new UiRect(x, y + 14, status.right() - x - 8, 14),
          EconomyUiTheme.TEXT_ERROR,
          UiTextAlignment.RIGHT);
    } else if (state.localErrorCode() != null) {
      renderer.translatedTextInRect(
          "screen.check_result.local_error",
          List.of(state.localErrorCode()),
          new UiRect(x, y + 14, status.right() - x - 8, 14),
          EconomyUiTheme.TEXT_ERROR,
          UiTextAlignment.RIGHT);
    }
    if (state.remoteStatus() == com.mo.economy_system.common.check.ClientFileCheckStatus.TRUNCATED) {
      renderer.translatedTextInRect(
          "screen.check_result.incomplete",
          List.of(),
          new UiRect(status.x() + 8, status.y() + 50, status.width() - 16, 14),
          EconomyUiTheme.TEXT_MUTED,
          UiTextAlignment.LEFT);
    }
    if (state.localIncomplete()) {
      renderer.translatedTextInRect(
          "screen.check_result.local_comparison_warning",
          List.of(),
          new UiRect(status.x() + 8, status.y() + 64, status.width() - 16, 14),
          EconomyUiTheme.TEXT_MUTED,
          UiTextAlignment.LEFT);
    }
  }
}
