package com.mo.economy_system.ui.renderer;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.theme.UiInputFrameStyle;
import java.util.List;

/** Exact semantic draw plan for a native EditBox frame. */
public record UiInputFramePlan(List<Command> commands) {
    public UiInputFramePlan { commands = List.copyOf(commands); }

    public static UiInputFramePlan frame(UiRect rect, UiInputFrameStyle style, boolean focused) {
        if (rect == null || style == null) throw new IllegalArgumentException("frame inputs");
        return new UiInputFramePlan(List.of(
                new Command(new UiRect(rect.x(), rect.y(), rect.width(), rect.height()), style.background()),
                new Command(new UiRect(rect.x(), rect.y(), rect.width(), 1), style.top(focused)),
                new Command(new UiRect(rect.x(), rect.bottom() - 1, rect.width(), 1), style.bottom(focused)),
                new Command(new UiRect(rect.x(), rect.y(), 1, rect.height()), style.left(focused)),
                new Command(new UiRect(rect.right() - 1, rect.y(), 1, rect.height()), style.right(focused))));
    }

    public record Command(UiRect rect, int argb) {}
}
