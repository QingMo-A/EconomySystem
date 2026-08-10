package com.mo.economy_system.ui.check;

import com.mo.economy_system.common.check.ClientFileCheckResultController;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

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
        new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()),
        CheckResultLayout.BACKGROUND_COLOR);
    renderer.translatedTextInRect(
        "screen.check_result.title", List.of(), layout.title(), EconomyUiTheme.TEXT_PRIMARY,
        UiTextAlignment.CENTER);
    renderer.inputFrame(layout.searchCard(), EconomyUiTheme.MARKET_SEARCH_FRAME,
        layout.search().contains(mouseX, mouseY));
    renderer.translatedTextInRect(
        "screen.check_result.search", List.of(), layout.search(), EconomyUiTheme.TEXT_MUTED, UiTextAlignment.LEFT);
    renderer.translatedText(
        "screen.check_result.target", List.of(state.targetName()), layout.status().x(), layout.status().y(),
        EconomyUiTheme.TEXT_SECONDARY);
    renderer.translatedText(
        "screen.check_result.type", List.of(state.checkTypeId()), layout.status().x(), layout.status().y() + 10,
        EconomyUiTheme.TEXT_SECONDARY);
    int statusX = Math.max(240, layout.scale().virtualWidth() / 2);
    renderer.textInRect(
        state.remoteStatus().name() + "  files=" + state.remoteFileCount()
            + " skipped=" + state.remoteSkippedCount(),
        new UiRect(statusX, 66, Math.max(1, layout.scale().virtualWidth() - statusX - 8), 14),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.LEFT);
    renderStatus(renderer, state, layout.status());
    int y = layout.rows().y();
    for (CheckResultRow row : state.visibleRows()) {
      UiRect line = new UiRect(layout.rows().x(), y, layout.rows().width(), 12);
      // The old page painted one plain, truncated line per row.  Keep the reason
      // key in the semantic string so target renderers can localize it if desired.
      renderer.textInRect(row.fileName() + "  " + row.reasonKey(), line,
          row.skipped() ? EconomyUiTheme.TEXT_MUTED : EconomyUiTheme.TEXT_SECONDARY,
          UiTextAlignment.LEFT);
      y += 12;
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
