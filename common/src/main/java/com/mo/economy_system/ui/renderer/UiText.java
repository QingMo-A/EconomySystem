package com.mo.economy_system.ui.renderer;

/** Compatibility facade for callers that keep text primitives beside renderer contracts. */
public final class UiText {
    private UiText() {
    }

    public static String truncate(com.mo.economy_system.ui.text.UiTextMetrics metrics,
                                  String text, int maxWidth) {
        return com.mo.economy_system.ui.text.UiText.truncate(metrics, text, maxWidth);
    }
}
