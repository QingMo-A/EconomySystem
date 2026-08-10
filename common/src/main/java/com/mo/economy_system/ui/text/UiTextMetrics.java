package com.mo.economy_system.ui.text;

import java.util.List;

/**
 * Loader-neutral text measurements used by common UI layout code.
 *
 * <p>The common module deliberately has no dependency on Minecraft's {@code Font}; each target
 * adapts its native font to this tiny contract.</p>
 */
public interface UiTextMetrics {
    int width(String text);

    int lineHeight();

    /** Measures a localized string using the target's native font and translation resolver. */
    default int translatedWidth(String key, List<String> arguments) {
        return width(key == null ? "" : key);
    }

    /** A deterministic approximation for pure common tests and tooling. */
    UiTextMetrics APPROXIMATE = new UiTextMetrics() {
        @Override
        public int width(String text) {
            return text == null ? 0 : text.length() * 6;
        }

        @Override
        public int lineHeight() {
            return 9;
        }

        @Override
        public int translatedWidth(String key, List<String> arguments) {
            String value = switch (key == null ? "" : key) {
                case "screen.market.filter.all" -> "全部";
                case "screen.market.filter.mine" -> "我的";
                case "screen.market.filter.sales" -> "卖单";
                case "screen.market.filter.demand" -> "求单";
                default -> key == null ? "" : key;
            };
            return width(value);
        }
    };
}
