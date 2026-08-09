package com.mo.economy_system.ui.territory.buff;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.TooltipLine;
import com.mo.economy_system.ui.renderer.TooltipModel;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Common semantic view for the territory-buff page. */
public final class BuffManageView {
    private BuffManageView() {}

    public static void render(EconomyUiRenderer renderer, BuffManageState state,
                              BuffManageLayout.Layout layout, int mouseX, int mouseY) {
        renderer.fill(new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()),
                0xB0000000);
        renderSearchFrame(renderer, layout.search());

        if (state.screenState() == ScreenState.LOADING) {
            renderer.translatedTextInRect("screen.territory.buff.loading", List.of(), layout.header(),
                    EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
        } else if (state.screenState() == ScreenState.ERROR) {
            renderer.translatedTextInRect(
                    state.errorKey() == null ? "screen.territory.buff.sync_failed" : state.errorKey(),
                    List.of(), layout.header(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.LEFT);
            renderer.translatedButton(layout.retryButton(), EconomyUiTheme.TERRITORY_BUTTON,
                    "screen.territory.buff.retry", List.of(),
                    layout.retryButton().contains(mouseX, mouseY), true);
        } else if (state.filteredBuffs().isEmpty()) {
            renderer.translatedTextInRect("screen.territory.buff.empty", List.of(), layout.header(),
                    EconomyUiTheme.TEXT_MUTED, UiTextAlignment.LEFT);
        }

        for (BuffManageLayout.Card card : layout.cards()) {
            BuffRow row = card.buff();
            renderer.card(card.card(), row.buff().unlocked()
                            ? EconomyUiTheme.TERRITORY_CARD : EconomyUiTheme.TERRITORY_LOCKED_CARD,
                    card.card().contains(mouseX, mouseY));
            renderer.icon(UiIcon.BUFF, card.icon());
            renderer.textInRect(row.buff().displayText(), card.name(), EconomyUiTheme.TEXT_PRIMARY,
                    UiTextAlignment.LEFT);
            renderer.translatedTextInRect("screen.territory.buff.level",
                    List.of(Integer.toString(row.buff().level()),
                            Integer.toString(row.buff().maxLevel())),
                    card.level(), EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.RIGHT);
            renderer.translatedTextInRect(row.buff().unlocked()
                            ? "screen.territory.buff.status.unlocked"
                            : "screen.territory.buff.status.locked",
                    List.of(), card.status(), row.buff().unlocked()
                            ? EconomyUiTheme.TEXT_SUCCESS : EconomyUiTheme.TEXT_LOCKED,
                    UiTextAlignment.LEFT);
            renderer.translatedTextInRect("screen.territory.buff.cost", List.of(), card.cost(),
                    row.availability() == BuffAvailability.AVAILABLE
                            || row.availability() == BuffAvailability.MAX_LEVEL
                            ? EconomyUiTheme.TEXT_SECONDARY : EconomyUiTheme.TEXT_ERROR,
                    UiTextAlignment.LEFT);
            String availabilityKey = availabilityKey(row.availability());
            if (availabilityKey != null) {
                renderer.translatedTextInRect(availabilityKey, List.of(), card.availability(),
                        EconomyUiTheme.TEXT_ERROR, UiTextAlignment.LEFT);
            }
            renderer.translatedButton(card.actionButton(), actionStyle(row), actionKey(row.action()),
                    List.of(), card.actionButton().contains(mouseX, mouseY),
                    row.availability() == BuffAvailability.AVAILABLE);
        }

        if (state.totalPages() > 1) {
            renderer.button(layout.previousButton(), EconomyUiTheme.TERRITORY_BUTTON, "<",
                    layout.previousButton().contains(mouseX, mouseY), state.page() > 0);
            renderer.textInRect((state.page() + 1) + " / " + state.totalPages(), layout.pageText(),
                    EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
            renderer.button(layout.nextButton(), EconomyUiTheme.TERRITORY_BUTTON, ">",
                    layout.nextButton().contains(mouseX, mouseY),
                    state.page() + 1 < state.totalPages());
        }

        renderer.icon(UiIcon.TERRITORY, new UiRect(layout.footerTitle().x(),
                layout.footerTitle().y(), 12, 12));
        renderer.translatedTextInRect("screen.territory.buff.title_named",
                List.of(state.territoryName()),
                new UiRect(layout.footerTitle().x() + 16, layout.footerTitle().y(),
                        Math.max(0, layout.footerTitle().width() - 16),
                        layout.footerTitle().height()),
                EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
        renderer.translatedTextInRect("screen.territory.buff.esc", List.of(), layout.escHint(),
                EconomyUiTheme.TEXT_MUTED, UiTextAlignment.RIGHT);
    }

    public static Optional<TooltipModel> tooltipAt(BuffManageState state,
                                                    BuffManageLayout.Layout layout,
                                                    int mouseX, int mouseY) {
        for (BuffManageLayout.Card card : layout.cards()) {
            if (card.icon().contains(mouseX, mouseY)) return Optional.of(buffTooltip(card.buff()));
            if (card.cost().contains(mouseX, mouseY)) return Optional.of(costTooltip(card.buff()));
        }
        return Optional.empty();
    }

    private static void renderSearchFrame(EconomyUiRenderer renderer, UiRect search) {
        UiRect frame = new UiRect(search.x() - 4, search.y() - 2,
                search.width() + 8, search.height() + 4);
        renderer.fill(frame, 0xE04A5568);
        renderer.fill(new UiRect(frame.x(), frame.y(), frame.width(), 1),
                EconomyUiTheme.TERRITORY_ACCENT);
        renderer.fill(new UiRect(frame.x(), frame.bottom() - 1, frame.width(), 1),
                EconomyUiTheme.TERRITORY_ACCENT);
        renderer.fill(new UiRect(frame.x(), frame.y(), 1, frame.height()),
                EconomyUiTheme.TERRITORY_ACCENT);
        renderer.fill(new UiRect(frame.right() - 1, frame.y(), 1, frame.height()),
                EconomyUiTheme.TERRITORY_ACCENT);
    }

    private static UiButtonStyle actionStyle(BuffRow row) {
        if (row.availability() != BuffAvailability.AVAILABLE) {
            return EconomyUiTheme.DISABLED_BUTTON;
        }
        return row.action() == BuffAction.UNLOCK
                ? EconomyUiTheme.TERRITORY_BUFF_UNLOCK_BUTTON
                : EconomyUiTheme.TERRITORY_BUFF_UPGRADE_BUTTON;
    }

    private static String actionKey(BuffAction action) {
        return switch (action) {
            case UNLOCK -> "button.territory.buff.unlock";
            case UPGRADE -> "button.territory.buff.upgrade";
            case MAX -> "button.territory.buff.max";
            case BACK -> "gui.back";
            case RETRY -> "screen.territory.buff.retry";
        };
    }

    private static String availabilityKey(BuffAvailability availability) {
        return switch (availability) {
            case MISSING_ITEMS -> "screen.territory.buff.availability.items";
            case MISSING_EXPERIENCE -> "screen.territory.buff.availability.experience";
            case MISSING_ITEMS_AND_EXPERIENCE -> "screen.territory.buff.availability.both";
            case INVALID_COST -> "screen.territory.buff.availability.invalid";
            case AVAILABLE, MAX_LEVEL -> null;
        };
    }

    private static TooltipModel buffTooltip(BuffRow row) {
        List<TooltipLine> lines = new ArrayList<>();
        lines.add(translated("screen.territory.buff.tooltip.id", row.buff().id()));
        lines.add(translated("screen.territory.buff.tooltip.name", row.buff().displayText()));
        lines.add(translated("screen.territory.buff.tooltip.level",
                Integer.toString(row.buff().level())));
        lines.add(translated("screen.territory.buff.tooltip.max_level",
                Integer.toString(row.buff().maxLevel())));
        lines.add(translated("screen.territory.buff.tooltip.effect", row.buff().effectId()));
        lines.add(translated(row.buff().unlocked()
                ? "screen.territory.buff.tooltip.unlocked"
                : "screen.territory.buff.tooltip.locked"));
        return new TooltipModel(lines);
    }

    private static TooltipModel costTooltip(BuffRow row) {
        List<TooltipLine> lines = new ArrayList<>();
        lines.add(translated("screen.territory.buff.tooltip.cost"));
        if (row.availability() == BuffAvailability.INVALID_COST) {
            lines.add(translated("screen.territory.buff.tooltip.cost.invalid"));
            return new TooltipModel(lines);
        }
        row.cost().items().forEach((itemId, required) -> lines.add(new TooltipLine.Item(
                row.resources().known()
                        ? "screen.territory.buff.tooltip.cost.item_known"
                        : "screen.territory.buff.tooltip.cost.item_unknown",
                itemId, row.resources().known()
                        ? List.of(Integer.toString(required),
                                Integer.toString(row.resources().itemCount(itemId)))
                        : List.of(Integer.toString(required)))));
        if (row.cost().experience() > 0) {
            lines.add(translated(row.resources().known()
                            ? "screen.territory.buff.tooltip.cost.experience_known"
                            : "screen.territory.buff.tooltip.cost.experience_unknown",
                    row.resources().known()
                            ? List.of(Integer.toString(row.cost().experience()),
                                    Integer.toString(row.resources().experienceLevel()))
                            : List.of(Integer.toString(row.cost().experience()))));
        }
        if (row.cost().currency() > 0) {
            lines.add(translated("screen.territory.buff.tooltip.cost.currency",
                    Integer.toString(row.cost().currency())));
        }
        if (lines.size() == 1) {
            lines.add(translated("screen.territory.buff.tooltip.cost.none"));
        }
        return new TooltipModel(lines);
    }

    private static TooltipLine.Translated translated(String key, String... arguments) {
        return new TooltipLine.Translated(key, List.of(arguments));
    }

    private static TooltipLine.Translated translated(String key, List<String> arguments) {
        return new TooltipLine.Translated(key, arguments);
    }
}
