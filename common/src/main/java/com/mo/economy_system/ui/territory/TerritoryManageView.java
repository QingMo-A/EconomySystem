package com.mo.economy_system.ui.territory;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import java.util.List;

/** Common semantic view; target renderers perform the actual Minecraft drawing. */
public final class TerritoryManageView {
    private TerritoryManageView() {
    }

    public static void render(EconomyUiRenderer renderer, TerritoryManageState state,
                              TerritoryManageLayout.Layout layout, int mouseX, int mouseY) {
        renderer.fill(new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()),
                0xB0000000);
        renderer.icon(UiIcon.TERRITORY,
                new UiRect(layout.title().x(), layout.title().y(), 12, 12));
        renderer.translatedText("screen.territory.manage", List.of(),
                layout.title().x() + 16, layout.title().y(), EconomyUiTheme.TEXT_PRIMARY);
        renderer.text(" / " + state.territoryName(), layout.title().x() + 112,
                layout.title().y(), EconomyUiTheme.TEXT_PRIMARY);
        renderer.translatedText("screen.territory.members", List.of(
                        Integer.toString(state.filteredMembers().size())),
                layout.memberHeader().x(), layout.memberHeader().y(), EconomyUiTheme.TEXT_SECONDARY);
        renderer.card(layout.actionPanel(), EconomyUiTheme.TERRITORY_CARD, false);
        renderer.translatedText("screen.territory.actions", List.of(), layout.actionPanel().x() + 8,
                layout.actionPanel().y() + 8, EconomyUiTheme.TEXT_PRIMARY);

        if (state.screenState() == ScreenState.LOADING) {
            renderer.translatedText("screen.territory.loading", List.of(), layout.memberPanel().x() + 8,
                    layout.memberPanel().y() + 8, EconomyUiTheme.TEXT_PRIMARY);
        } else if (state.screenState() == ScreenState.ERROR) {
            renderer.translatedText(state.errorKey() == null ? "screen.territory.sync_failed" : state.errorKey(),
                    List.of(), layout.memberPanel().x() + 8, layout.memberPanel().y() + 8,
                    EconomyUiTheme.TEXT_ERROR);
            renderer.translatedButton(layout.retryButton(), EconomyUiTheme.TERRITORY_BUTTON,
                    "screen.territory.retry", List.of(),
                    layout.retryButton().contains(mouseX, mouseY), state.can(TerritoryManageAction.RETRY));
        } else if (state.screenState() == ScreenState.EMPTY) {
            renderer.translatedText("screen.territory.manage_empty", List.of(), layout.memberPanel().x() + 8,
                    layout.memberPanel().y() + 8, EconomyUiTheme.TEXT_MUTED);
        }

        for (TerritoryManageLayout.MemberCard card : layout.cards()) {
            renderer.card(card.card(), EconomyUiTheme.TERRITORY_CARD,
                    card.card().contains(mouseX, mouseY));
            renderer.playerHead(card.member().playerId(), card.member().playerName(),
                    new UiRect(card.card().x() + 8, card.card().y() + 8, 32, 32));
            renderer.text(card.member().playerName(), card.card().x() + 44,
                    card.card().y() + 8, EconomyUiTheme.TEXT_PRIMARY);
            renderer.text(card.member().playerId().toString(), card.card().x() + 44,
                    card.card().y() + 25, EconomyUiTheme.TEXT_SECONDARY);
            if (state.can(TerritoryManageAction.KICK)) {
                renderer.translatedButton(card.kickButton(), EconomyUiTheme.TERRITORY_DANGER_BUTTON,
                        "message.territory_management.kick_player", List.of(),
                        card.kickButton().contains(mouseX, mouseY), true);
            }
        }

        for (TerritoryManageLayout.ActionButton action : layout.actionButtons()) {
            renderer.translatedButton(action.rect(), style(action.action()), key(action.action()), List.of(),
                    action.rect().contains(mouseX, mouseY), state.can(action.action()));
        }

        renderer.button(layout.previousButton(), EconomyUiTheme.TERRITORY_BUTTON,
                "<", layout.previousButton().contains(mouseX, mouseY), state.page() > 0);
        renderer.text((state.page() + 1) + " / " + state.totalPages(), layout.pageText().x(),
                layout.pageText().y() + 6, EconomyUiTheme.TEXT_PRIMARY);
        renderer.button(layout.nextButton(), EconomyUiTheme.TERRITORY_BUTTON,
                ">", layout.nextButton().contains(mouseX, mouseY), state.page() + 1 < state.totalPages());
        renderer.translatedButton(layout.backButton(), EconomyUiTheme.TERRITORY_BUTTON, "gui.back", List.of(),
                layout.backButton().contains(mouseX, mouseY), state.can(TerritoryManageAction.BACK));
    }

    private static String key(TerritoryManageAction action) {
        return switch (action) {
            case COPY_ID -> "message.territory_management.copy_id";
            case MODIFY_MODE -> "message.territory_management.resize_territory";
            case INVITE -> "message.territory_management.invite_player";
            case BUFFS -> "message.territory_management.buff";
            case ACCESS -> "message.territory_management.access";
            case PERMISSIONS -> "message.territory_management.permissions";
            case TRANSFER -> "message.territory_management.transfer_ownership";
            case DELETE -> "message.territory_management.delete_territory";
            case KICK -> "message.territory_management.kick_player";
            case RETRY -> "screen.territory.retry";
            case BACK -> "gui.back";
        };
    }

    private static UiButtonStyle style(TerritoryManageAction action) {
        return switch (action) {
            case BUFFS -> EconomyUiTheme.TERRITORY_WARN_BUTTON;
            case DELETE, KICK -> EconomyUiTheme.TERRITORY_DANGER_BUTTON;
            case COPY_ID, ACCESS, PERMISSIONS, TRANSFER -> EconomyUiTheme.TERRITORY_NEUTRAL_BUTTON;
            default -> EconomyUiTheme.TERRITORY_PRIMARY_BUTTON;
        };
    }
}
