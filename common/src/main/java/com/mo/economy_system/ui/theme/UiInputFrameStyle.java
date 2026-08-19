package com.mo.economy_system.ui.theme;

/** Loader-neutral chrome for native text inputs. Transparent colors omit that surface/edge. */
public record UiInputFrameStyle(int background, int top, int bottom, int left, int right,
                                int focusedTop, int focusedBottom, int focusedLeft, int focusedRight) {
    public UiInputFrameStyle {
        // Colors are packed ARGB ints and are therefore commonly negative when the alpha bit is set.
    }

    public static UiInputFrameStyle of(int background, int border) {
        return new UiInputFrameStyle(background, border, border, border, border,
                border, border, border, border);
    }

    /** Creates the global transparent-field treatment: an idle underline and focused accent. */
    public static UiInputFrameStyle underline(int idleLine, int focusedLine) {
        return new UiInputFrameStyle(0, 0, idleLine, 0, 0,
                0, focusedLine, 0, 0);
    }

    public int top(boolean focused) { return focused ? focusedTop : top; }
    public int bottom(boolean focused) { return focused ? focusedBottom : bottom; }
    public int left(boolean focused) { return focused ? focusedLeft : left; }
    public int right(boolean focused) { return focused ? focusedRight : right; }
}
