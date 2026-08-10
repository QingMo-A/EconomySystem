package com.mo.economy_system.ui.territory.list;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import com.mo.economy_system.ui.theme.UiCardStyle;
import java.util.List;
import java.util.Locale;

/** Semantic territory card-grid view shared by Forge and NeoForge. */
public final class TerritoryListView {
  private TerritoryListView() {}

  public static void render(EconomyUiRenderer renderer, TerritoryListState state,
                            TerritoryListLayout.Layout layout, int mouseX, int mouseY) {
    renderer.fill(new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()),
        TerritoryListLayout.BACKGROUND_COLOR);
    renderer.inputFrame(layout.searchBackground(), EconomyUiTheme.TERRITORY_SEARCH_FRAME,
        layout.search().contains(mouseX, mouseY));
    renderer.translatedTextInRect("screen.territory.list.search", List.of(), layout.search(),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.LEFT);
    renderer.card(layout.title(), EconomyUiTheme.VERSION_CARD, false);
    renderer.scaledIconTranslatedText(UiIcon.TERRITORY, "screen.territory.title", List.of(),
        layout.title().x() + 8, layout.title().y() + 5, 1.0f,
        EconomyUiRenderer.ICON_SIZE, EconomyUiRenderer.ICON_ADVANCE, EconomyUiTheme.TEXT_PRIMARY);
    renderer.translatedTextInRect("screen.territory.list.esc", List.of(), layout.esc(),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.RIGHT);

    if (state.screenState() == ScreenState.LOADING) {
      renderer.translatedTextInRect("screen.territory.list.loading", List.of(), layout.retryButton(),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    } else if (state.screenState() == ScreenState.ERROR) {
      renderer.translatedTextInRect(state.errorKey() == null ? "screen.territory.sync_failed" : state.errorKey(),
          List.of(), layout.retryButton(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
      renderer.translatedButton(layout.retryButton(), EconomyUiTheme.TERRITORY_BUTTON,
          "screen.territory.retry", List.of(), layout.retryButton().contains(mouseX, mouseY),
          state.can(TerritoryListAction.RETRY));
    } else if (state.screenState() == ScreenState.EMPTY) {
      renderer.translatedTextInRect("screen.territory.list.empty", List.of(), layout.retryButton(),
          EconomyUiTheme.TEXT_MUTED, UiTextAlignment.CENTER);
    }

    for (TerritoryListLayout.Card card : layout.cards()) {
      TerritoryListRow row = card.row();
      renderer.card(card.card(), cardStyle(row), card.card().contains(mouseX, mouseY));
      UiIcon dimensionIcon = dimensionIcon(row.dimensionId());
      renderer.icon(dimensionIcon, new UiRect(card.card().x() + 8, card.card().y() + 18, 12, 12));
      renderer.textInRect(row.summary().name(),
          new UiRect(card.card().x() + 24, card.card().y() + 7, card.card().width() - 32, 16),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
      renderer.translatedTextInRect(dimensionKey(row.dimensionId()), List.of(),
          new UiRect(card.card().x() + 24, card.card().y() + 22, card.card().width() - 32, 14),
          dimensionColor(row.dimensionId()), UiTextAlignment.LEFT);
      renderer.textInRect(row.summary().ownerName(),
          new UiRect(card.card().x() + 8, card.card().y() + 40, card.card().width() / 2 - 12, 14),
          row.owned() ? 0xFFFFD700 : 0xFF87CEEB, UiTextAlignment.LEFT);
      renderer.translatedTextInRect(row.owned() ? "screen.territory.list.owned" : "screen.territory.list.authorized",
          List.of(), new UiRect(card.card().x() + card.card().width() / 2,
              card.card().y() + 40, card.card().width() / 2 - 8, 14),
          row.owned() ? EconomyUiTheme.TEXT_SUCCESS : 0xFFDDA0DD, UiTextAlignment.RIGHT);
      renderer.textInRect(row.coordinateText(),
          new UiRect(card.card().x() + 8, card.card().y() + 55, card.card().width() - 16, 14),
          0xFF98FB98, UiTextAlignment.CENTER);
      String id = row.summary().territoryId().toString();
      renderer.textInRect("ID: " + (id.length() > 10 ? id.substring(0, 10) + "..." : id),
          new UiRect(card.card().x() + 8, card.card().y() + 72, card.card().width() - 16, 12),
          EconomyUiTheme.TEXT_MUTED, UiTextAlignment.CENTER);
      renderer.translatedButton(card.teleportButton(), EconomyUiTheme.TERRITORY_BUTTON,
          "button.territory.teleport", List.of(), card.teleportButton().contains(mouseX, mouseY),
          state.can(TerritoryListAction.TELEPORT));
      if (row.owned()) renderer.translatedButton(card.manageButton(), EconomyUiTheme.TERRITORY_PRIMARY_BUTTON,
          "button.territory.manage", List.of(), card.manageButton().contains(mouseX, mouseY),
          state.can(TerritoryListAction.MANAGE));
    }

    if (state.totalPages() > 1) {
      boolean previousEnabled = state.page() > 0;
      renderer.button(layout.previousButton(), previousEnabled
              ? EconomyUiTheme.TERRITORY_PAGE_BUTTON : EconomyUiTheme.TERRITORY_PAGE_BUTTON_DISABLED,
          "", layout.previousButton().contains(mouseX, mouseY), previousEnabled);
      renderer.icon(UiIcon.ARROW_LEFT, new UiRect(layout.previousButton().x() + 19,
          layout.previousButton().y() + 5, 12, 12));
      renderer.textInRect((state.page() + 1) + " / " + state.totalPages(), layout.pageText(),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
      boolean nextEnabled = state.page() + 1 < state.totalPages();
      renderer.button(layout.nextButton(), nextEnabled
              ? EconomyUiTheme.TERRITORY_PAGE_BUTTON : EconomyUiTheme.TERRITORY_PAGE_BUTTON_DISABLED,
          "", layout.nextButton().contains(mouseX, mouseY), nextEnabled);
      renderer.icon(UiIcon.ARROW_RIGHT, new UiRect(layout.nextButton().x() + 19,
          layout.nextButton().y() + 5, 12, 12));
    }
  }

  private static UiCardStyle cardStyle(TerritoryListRow row) {
    return row.owned() ? EconomyUiTheme.TERRITORY_CARD : EconomyUiTheme.TERRITORY_LOCKED_CARD;
  }

  private static UiIcon dimensionIcon(String id) {
    String normalized = id.toLowerCase(Locale.ROOT);
    if (normalized.contains("nether")) return UiIcon.NETHER;
    if (normalized.contains("end")) return UiIcon.END;
    return UiIcon.OVERWORLD;
  }

  private static int dimensionColor(String id) {
    String normalized = id.toLowerCase(Locale.ROOT);
    if (normalized.contains("nether")) return 0xFFFF5722;
    if (normalized.contains("end")) return 0xFF9C27B0;
    return 0xFF4CAF50;
  }

  private static String dimensionKey(String id) {
    String normalized = id.toLowerCase(Locale.ROOT);
    if (normalized.contains("nether")) return "screen.territory.list.dimension.nether";
    if (normalized.contains("end")) return "screen.territory.list.dimension.end";
    return "screen.territory.list.dimension.overworld";
  }
}
