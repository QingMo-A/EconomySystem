package com.mo.economy_system.ui.text;

import java.util.Objects;

/** A small loader-neutral styled text span used by common UI primitives. */
public record UiTextSpan(String text, int color) {
    public UiTextSpan {
        text = Objects.requireNonNull(text, "text");
    }
}
