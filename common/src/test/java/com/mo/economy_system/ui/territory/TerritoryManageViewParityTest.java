package com.mo.economy_system.ui.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.TooltipModel;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import com.mo.economy_system.ui.theme.UiCardStyle;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.text.UiTextSpan;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryManageViewParityTest {
    @Test
    void commonViewProducesDeterministicSemanticOperations() {
        TerritoryManageState state = new TerritoryManageState(
                new UUID(0, 1), "spawn", new UUID(0, 2), "owner",
                List.of(new MemberRow(new UUID(0, 3), "alice")),
                0, 5, 0, "", ScreenState.READY, null, -1,
                Set.of(TerritoryManageAction.values()));
        TerritoryManageLayout.Layout layout = TerritoryManageLayout.calculate(854, 480, state);
        RecordingRenderer forge = new RecordingRenderer();
        RecordingRenderer neoForge = new RecordingRenderer();

        TerritoryManageView.render(forge, state, layout, 0, 0);
        TerritoryManageView.render(neoForge, state, layout, 0, 0);

        assertEquals(forge.operations, neoForge.operations);
        assertTrue(forge.operations.stream().anyMatch(value -> value.kind.equals("playerHead")));
        assertTrue(forge.operations.stream().anyMatch(value ->
                value.kind.equals("scaledIconText")
                        && value.text.startsWith("TERRITORY:领地管理 · spawn:")));
        assertTrue(forge.operations.stream().anyMatch(value ->
                value.kind.equals("card")
                        && value.rect.equals(layout.actionPanel())
                        && !value.hovered));
        assertTrue(forge.operations.stream().anyMatch(value ->
                value.kind.equals("translatedButton")
                        && value.text.equals("message.territory_management.resize_territory")
                        && value.buttonStyle.equals(EconomyUiTheme.TERRITORY_PRIMARY_BUTTON)));
        assertTrue(forge.operations.stream().anyMatch(value ->
                value.kind.equals("translatedButton")
                        && value.text.equals("message.territory_management.delete_territory")
                        && value.buttonStyle.equals(EconomyUiTheme.TERRITORY_DANGER_BUTTON)));
    }

    @Test
    void errorStateProducesAReachableRetryOperation() {
        TerritoryManageState state = new TerritoryManageState(
                new UUID(0, 1), "spawn", new UUID(0, 2), "owner", List.of(),
                0, 4, 0, "", ScreenState.ERROR, "screen.territory.sync_failed", -1,
                Set.of(TerritoryManageAction.RETRY, TerritoryManageAction.BACK));
        TerritoryManageLayout.Layout layout = TerritoryManageLayout.calculate(640, 360, state);
        RecordingRenderer renderer = new RecordingRenderer();

        TerritoryManageView.render(renderer, state, layout,
                layout.retryButton().x(), layout.retryButton().y());

        assertTrue(renderer.operations.stream().anyMatch(value ->
                value.kind.equals("translatedButton")
                        && value.text.equals("screen.territory.retry")
                        && value.rect.equals(layout.retryButton())
                        && value.enabled));
    }

    private static final class RecordingRenderer implements EconomyUiRenderer {
        private final List<Operation> operations = new ArrayList<>();

        @Override
        public void fill(UiRect rect, int argb) {
            operations.add(new Operation("fill", rect, null, null, null, false, true));
        }

        @Override
        public void text(String text, int x, int y, int argb) {
            operations.add(new Operation("text", new UiRect(x, y, 0, 0), text,
                    null, null, false, true));
        }

        @Override
        public void translatedText(String key, List<String> arguments, int x, int y, int argb) {
            operations.add(new Operation("translatedText", new UiRect(x, y, 0, 0),
                    key + arguments, null, null, false, true));
        }

        @Override
        public void textInRect(String text, UiRect rect, int argb, UiTextAlignment alignment) {
            operations.add(new Operation("textInRect", rect, text + ":" + alignment,
                    null, null, false, true));
        }

        @Override
        public void translatedTextInRect(String key, List<String> arguments, UiRect rect,
                                         int argb, UiTextAlignment alignment) {
            operations.add(new Operation("translatedTextInRect", rect,
                    key + arguments + ":" + alignment, null, null, false, true));
        }

        @Override
        public void card(UiRect rect, UiCardStyle style, boolean hovered) {
            operations.add(new Operation("card", rect, null, style, null, hovered, true));
        }

        @Override
        public void button(UiRect rect, UiButtonStyle style, String text,
                           boolean hovered, boolean enabled) {
            operations.add(new Operation("button", rect, text, null, style, hovered, enabled));
        }

        @Override
        public void translatedButton(UiRect rect, UiButtonStyle style, String key,
                                     List<String> arguments, boolean hovered, boolean enabled) {
            operations.add(new Operation("translatedButton", rect, key, null, style, hovered, enabled));
        }

        @Override
        public void translatedIconButton(UiRect rect, UiButtonStyle style, UiIcon icon,
                                         String key, List<String> arguments, boolean hovered,
                                         boolean enabled) {
            operations.add(new Operation("translatedIconButton", rect, key, null, style,
                    hovered, enabled));
        }

        @Override
        public void icon(UiIcon icon, UiRect rect) {
            operations.add(new Operation("icon", rect, icon.name(), null, null, false, true));
        }

        @Override
        public void scaledIconText(UiIcon icon, String text, int originX, int originY,
                                   float scale, int iconSize, int iconAdvance, int textColor) {
            operations.add(new Operation("scaledIconText",
                    new UiRect(originX, originY, iconSize, iconSize),
                    icon.name() + ":" + text + ":" + scale + ":" + iconAdvance,
                    null, null, false, true));
        }

        @Override
        public void scaledIconStyledText(UiIcon icon, List<UiTextSpan> spans, int originX,
                                         int originY, float scale, int iconSize, int iconAdvance) {
            operations.add(new Operation("scaledIconStyledText",
                    new UiRect(originX, originY, iconSize, iconSize),
                    icon.name() + ":" + spans + ":" + scale + ":" + iconAdvance,
                    null, null, false, true));
        }

        @Override
        public UiTextMetrics metrics() {
            return UiTextMetrics.APPROXIMATE;
        }

        @Override
        public void itemWithCount(String itemId, int count, UiRect rect) {
            operations.add(new Operation("itemWithCount", rect, itemId + ":" + count,
                    null, null, false, true));
        }

        @Override
        public void playerHead(UUID playerId, String playerName, UiRect rect) {
            operations.add(new Operation("playerHead", rect, playerId + ":" + playerName,
                    null, null, false, true));
        }

        @Override
        public void tooltip(TooltipModel tooltip, int mouseX, int mouseY) {
            operations.add(new Operation("tooltip", new UiRect(mouseX, mouseY, 0, 0),
                    tooltip.toString(), null, null, false, true));
        }
        @Override public void itemDisplayNameWithSuffix(String itemId, String suffix, UiRect rect, int color, UiTextAlignment alignment) {}
        @Override public void translatedTextWithSuffix(String key, List<String> arguments, String suffix, UiRect rect, int color, UiTextAlignment alignment) {}
    }

    private record Operation(String kind, UiRect rect, String text, UiCardStyle cardStyle,
                             UiButtonStyle buttonStyle, boolean hovered, boolean enabled) {
    }
}
