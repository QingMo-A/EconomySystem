package com.mo.economy_system.ui.text;

import com.mo.economy_system.ui.geometry.UiRect;
import java.util.List;

/** Dynamic geometry for the small icon-and-version title cards used by list screens. */
public final class UiVersionInfoLayout {
    private static final int ICON_ADVANCE = 14;
    private static final int CARD_PADDING = 16;

    private UiVersionInfoLayout() {
    }

    public static Result calculate(UiTextMetrics metrics, String key, List<String> arguments,
                                   int x, int bottom, int maxContentWidth) {
        if (metrics == null) metrics = UiTextMetrics.APPROXIMATE;
        List<String> args = arguments == null ? List.of() : List.copyOf(arguments);
        int contentWidth = ICON_ADVANCE + Math.max(0, metrics.translatedWidth(key, args));
        int boundedMax = Math.max(1, maxContentWidth);
        float scale = Math.min(1.0f, (float) boundedMax / Math.max(1, contentWidth));
        int cardWidth = Math.max(1, (int) (contentWidth * scale) + CARD_PADDING);
        int cardHeight = Math.max(1, metrics.lineHeight() + 10);
        return new Result(new UiRect(x, bottom - cardHeight, cardWidth, cardHeight), scale,
            contentWidth, boundedMax);
    }

    public record Result(UiRect card, float contentScale, int contentWidth, int maxContentWidth) {
    }
}
