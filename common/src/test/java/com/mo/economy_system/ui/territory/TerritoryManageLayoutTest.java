package com.mo.economy_system.ui.territory;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryManageLayoutTest {
    @Test
    void layoutsRemainInsideVirtualViewport() {
        TerritoryManageState state = state(12, ScreenState.READY);
        for (int[] size : new int[][]{{320, 180}, {640, 360}, {854, 480}, {1280, 720}, {1920, 1080}, {180, 120}, {640, 80}}) {
            var layout = TerritoryManageLayout.calculate(size[0], size[1], state);
            int width = layout.scale().virtualWidth();
            int height = layout.scale().virtualHeight();
            for (UiRect rect : List.of(layout.memberHeader(), layout.memberPanel(),
                    layout.actionPanel(), layout.previousButton(), layout.nextButton(),
                    layout.pageText(), layout.retryButton(), layout.footer(), layout.escHint())) {
                assertTrue(rect.x() >= 0 && rect.y() >= 0);
                assertTrue(rect.right() <= width && rect.bottom() <= height);
            }
            assertFalse(layout.memberPanel().overlaps(layout.actionPanel()));
            assertFalse(layout.previousButton().overlaps(layout.nextButton()));
            for (var card : layout.cards()) {
                assertTrue(layout.memberPanel().contains(card.card()));
                assertTrue(card.card().right() <= width);
                assertTrue(card.card().bottom() <= height);
                assertTrue(card.card().contains(card.kickButton()));
            }
            assertNoOverlap(layout.cards().stream().map(TerritoryManageLayout.MemberCard::card).toList());
            assertNoOverlap(layout.actionButtons().stream().map(TerritoryManageLayout.ActionButton::rect).toList());
        }
    }

    @Test
    void emptyStateHasNoMemberCardsAndStableNavigation() {
        var layout = TerritoryManageLayout.calculate(640, 360, state(0, ScreenState.EMPTY));
        assertTrue(layout.cards().isEmpty());
        assertTrue(layout.previousButton().width() > 0);
        assertTrue(layout.nextButton().width() > 0);
    }

    private static TerritoryManageState state(int count, ScreenState status) {
        List<MemberRow> members = java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new MemberRow(new UUID(0, i + 1), "member-" + i)).toList();
        return new TerritoryManageState(new UUID(0, 100), "territory", new UUID(0, 101), "owner",
                members, 0, 8, 0, "", status, null, -1,
                Set.of(TerritoryManageAction.BACK, TerritoryManageAction.KICK));
    }

    private static void assertNoOverlap(List<UiRect> rectangles) {
        for (int left = 0; left < rectangles.size(); left++) {
            for (int right = left + 1; right < rectangles.size(); right++) {
                assertFalse(rectangles.get(left).overlaps(rectangles.get(right)),
                        rectangles.get(left) + " overlaps " + rectangles.get(right));
            }
        }
    }
}
