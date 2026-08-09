package com.mo.economy_system.ui.text;

/**
 * Loader-neutral text measurements used by common UI layout code.
 *
 * <p>The common module deliberately has no dependency on Minecraft's {@code Font}; each target
 * adapts its native font to this tiny contract.</p>
 */
public interface UiTextMetrics {
    int width(String text);

    int lineHeight();

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
    };
}
