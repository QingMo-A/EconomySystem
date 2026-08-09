package com.mo.economy_system.ui.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.text.UiTextMetrics;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Reference-derived geometry assertions for the 1.21.1 Territory Manage screen. */
class TerritoryManageGoldenParityTest {
    private static final UiTextMetrics FONT = new UiTextMetrics() {
        @Override public int width(String text) { return text == null ? 0 : text.length() * 6; }
        @Override public int lineHeight() { return 9; }
    };

    @Test
    void matchesLegacy1211Reference() {
        List<MemberRow> members = java.util.stream.IntStream.range(0, 12)
                .mapToObj(i -> new MemberRow(new UUID(0, i + 1), "member-" + i)).toList();
        TerritoryManageState state = new TerritoryManageState(new UUID(0, 100), "spawn",
                new UUID(0, 101), "owner", members, 0, 4, 0, "", ScreenState.READY,
                null, -1, Set.of(TerritoryManageAction.values()));
        TerritoryManageLayout.Layout layout = TerritoryManageLayout.calculate(640, 360, state, FONT);

        assertEquals(640, layout.scale().virtualWidth());
        assertEquals(360, layout.scale().virtualHeight());
        assertEquals(new com.mo.economy_system.ui.geometry.UiRect(12, 55, 424, 48),
                layout.cards().get(0).card());
        assertEquals(new com.mo.economy_system.ui.geometry.UiRect(362, 68, 66, 22),
                layout.cards().get(0).kickButton());
        assertEquals(180, layout.actionPanel().width());
        assertEquals(55, layout.actionPanel().y());
        assertEquals(50, layout.previousButton().width());
        assertEquals(24, layout.previousButton().height());
        assertEquals(8, layout.actionButtons().size());
        assertEquals(78, layout.actionButtons().get(0).rect().y());
        assertEquals(329, layout.footer().y());
        assertEquals(19, layout.footer().height());
        assertEquals(348, layout.footer().bottom());
        assertEquals(9, layout.escHint().height());
    }

    @Test
    void matchesLegacy1211ReferenceByScalingLongFooterWithoutEllipsis() {
        String longName = "territory-".repeat(20);
        TerritoryManageState state = new TerritoryManageState(new UUID(0, 100), longName,
                new UUID(0, 101), "owner", List.of(), 0, 4, 0, "", ScreenState.EMPTY,
                null, -1, Set.of(TerritoryManageAction.values()));
        TerritoryManageLayout.Layout layout = TerritoryManageLayout.calculate(640, 360, state, FONT);

        assertTrue(FONT.width(layout.footerText()) + TerritoryManageLayout.FOOTER_ICON_ADVANCE
                > TerritoryManageLayout.FOOTER_MAX_CONTENT_WIDTH);
        assertTrue(layout.footerContentScale() < 1.0f);
        assertFalse(layout.footerText().contains("..."));
        assertEquals("领地管理 · " + longName, layout.footerText());
        assertEquals(348, layout.footer().bottom());
    }
}
