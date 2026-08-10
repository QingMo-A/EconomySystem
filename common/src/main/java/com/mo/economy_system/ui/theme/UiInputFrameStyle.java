package com.mo.economy_system.ui.theme;

/** Loader-neutral four-edge chrome for native text inputs. */
public record UiInputFrameStyle(int background, int top, int bottom, int left, int right,
                                int focusedTop, int focusedBottom, int focusedLeft, int focusedRight) {
    public UiInputFrameStyle {
        // Colors are packed ARGB ints and are therefore commonly negative when the alpha bit is set.
    }

    public static UiInputFrameStyle of(int background, int border) {
        return new UiInputFrameStyle(background, border, border, border, border,
                border, border, border, border);
    }

    public int top(boolean focused) { return focused ? focusedTop : top; }
    public int bottom(boolean focused) { return focused ? focusedBottom : bottom; }
    public int left(boolean focused) { return focused ? focusedLeft : left; }
    public int right(boolean focused) { return focused ? focusedRight : right; }
}
