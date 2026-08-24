package com.mo.economy_system.ui.territory;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.text.UiText;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import java.util.List;

/** Common semantic Territory Manage view; targets only translate renderer operations. */
public final class TerritoryManageView {
    private TerritoryManageView() {
    }

    public static void render(EconomyUiRenderer renderer, TerritoryManageState state,
                              TerritoryManageLayout.Layout layout, int mouseX, int mouseY) {
        UiTextMetrics metrics = layout.metrics();
        int width = layout.scale().virtualWidth();
        int height = layout.scale().virtualHeight();
        renderer.fill(new UiRect(0, 0, width, height), 0xB0000000);

        // FooterIdentity / title-card semantic, matching CardRenderer.drawVersionInfo from the
        // legacy 1.21.1 reference rather than a plain bottom-corner string.
        UiRect footer = layout.footer();
        renderer.card(footer, EconomyUiTheme.VERSION_CARD, false);
        renderer.scaledIconText(UiIcon.TERRITORY, layout.footerText(),
                footer.x() + 8, footer.y() + 5, layout.footerContentScale(),
                TerritoryManageLayout.FOOTER_ICON_SIZE,
                TerritoryManageLayout.FOOTER_ICON_ADVANCE, EconomyUiTheme.TEXT_PRIMARY);
        renderer.fill(new UiRect(footer.x() + 8, Math.max(footer.y(), footer.bottom() - 3),
                Math.max(0, footer.width() - 16), 1), 0x30FFFFFF);

        renderer.textInRect("按 ESC 返回", layout.escHint(), 0x90FFFFFF, UiTextAlignment.RIGHT);

        // Member header and action panel.
        renderer.translatedText("screen.territory.members",
                List.of(Integer.toString(state.filteredMembers().size())),
                layout.memberHeader().x(), layout.memberHeader().y(), EconomyUiTheme.TEXT_SECONDARY);
        renderer.card(layout.actionPanel(), EconomyUiTheme.TERRITORY_CARD, false);
        UiRect panelHeader = new UiRect(layout.actionPanel().x() + 8,
                layout.actionPanel().y() + 6,
                Math.max(1, layout.actionPanel().width() - 16), metrics.lineHeight());
        renderer.textInRect("管理操作", panelHeader, EconomyUiTheme.TEXT_PRIMARY,
                UiTextAlignment.LEFT);
        UiRect panelSubtitle = new UiRect(panelHeader.x(), panelHeader.y() + metrics.lineHeight(),
                panelHeader.width(), metrics.lineHeight());
        renderer.textInRect(UiText.truncate(metrics, state.territoryName(), panelSubtitle.width()),
                panelSubtitle, EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);

        if (state.screenState() == ScreenState.LOADING) {
            renderer.translatedText("screen.territory.loading", List.of(),
                    layout.memberPanel().x() + 8, layout.memberPanel().y() + 8,
                    EconomyUiTheme.TEXT_PRIMARY);
        } else if (state.screenState() == ScreenState.ERROR) {
            renderer.translatedText(state.errorKey() == null ? "screen.territory.sync_failed" : state.errorKey(),
                    List.of(), layout.memberPanel().x() + 8, layout.memberPanel().y() + 8,
                    EconomyUiTheme.TEXT_ERROR);
            renderer.translatedButton(layout.retryButton(), EconomyUiTheme.TERRITORY_BUTTON,
                    "screen.territory.retry", List.of(),
                    layout.retryButton().contains(mouseX, mouseY), state.can(TerritoryManageAction.RETRY));
        } else if (state.screenState() == ScreenState.EMPTY) {
            renderer.textInRect("暂无成员", layout.memberPanel(), 0x80FFFFFF,
                    UiTextAlignment.CENTER);
        }

        for (TerritoryManageLayout.MemberCard memberCard : layout.cards()) {
            boolean hovered = memberCard.card().contains(mouseX, mouseY);
            renderer.card(memberCard.card(), EconomyUiTheme.TERRITORY_CARD, hovered);
            renderer.playerHead(memberCard.member().playerId(), memberCard.member().playerName(),
                    new UiRect(memberCard.card().x() + 8, memberCard.card().y() + 8,
                            TerritoryManageLayout.PLAYER_ICON_SIZE,
                            TerritoryManageLayout.PLAYER_ICON_SIZE));

            UiRect nameRect = memberCard.nameRect(metrics);
            renderer.textInRect(UiText.truncate(metrics, memberCard.member().playerName(), nameRect.width()),
                    nameRect, EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);

            UiRect uuidRect = memberCard.uuidRect(metrics);
            int uuidWidth = Math.max(1, memberCard.kickButton().x() - uuidRect.x() - 4);
            renderer.textInRect(UiText.truncate(metrics,
                            memberCard.member().playerId().toString(), uuidWidth),
                    new UiRect(uuidRect.x(), uuidRect.y(), uuidWidth, uuidRect.height()),
                    EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
            if (state.can(TerritoryManageAction.KICK)) {
                renderer.translatedButton(memberCard.kickButton(), EconomyUiTheme.TERRITORY_DANGER_BUTTON,
                        "message.territory_management.kick_player", List.of(),
                        memberCard.kickButton().contains(mouseX, mouseY), true);
            }
        }

        for (TerritoryManageLayout.ActionButton action : layout.actionButtons()) {
            renderer.translatedButton(action.rect(), style(action.action()), key(action.action()), List.of(),
                    action.rect().contains(mouseX, mouseY), state.can(action.action()));
        }

        if (state.totalPages() > 1) {
            String pageText = (state.page() + 1) + " / " + state.totalPages();
            renderer.button(layout.previousButton(), pageStyle(state.page() > 0), "<",
                    layout.previousButton().contains(mouseX, mouseY), state.page() > 0);
            renderer.textInRect(pageText, layout.pageText(), EconomyUiTheme.TEXT_PRIMARY,
                    UiTextAlignment.CENTER);
            renderer.button(layout.nextButton(), pageStyle(state.page() + 1 < state.totalPages()), ">",
                    layout.nextButton().contains(mouseX, mouseY), state.page() + 1 < state.totalPages());
        }
    }

    private static UiButtonStyle pageStyle(boolean enabled) {
        return enabled ? EconomyUiTheme.PAGE_BUTTON : EconomyUiTheme.PAGE_BUTTON_DISABLED;
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
            case BUFFS -> EconomyUiTheme.TERRITORY_BUFF_BUTTON;
            case DELETE, KICK -> EconomyUiTheme.TERRITORY_DANGER_BUTTON;
            case COPY_ID, ACCESS, PERMISSIONS, TRANSFER -> EconomyUiTheme.TERRITORY_NEUTRAL_BUTTON;
            default -> EconomyUiTheme.TERRITORY_PRIMARY_BUTTON;
        };
    }
}
