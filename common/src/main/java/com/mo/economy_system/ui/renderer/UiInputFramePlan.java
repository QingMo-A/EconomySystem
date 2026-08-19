package com.mo.economy_system.ui.renderer;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.theme.UiInputFrameStyle;
import java.util.ArrayList;
import java.util.List;

/** Exact semantic draw plan for a native EditBox frame. */
public record UiInputFramePlan(List<Command> commands) {
    public UiInputFramePlan { commands = List.copyOf(commands); }

    public static UiInputFramePlan frame(UiRect rect, UiInputFrameStyle style, boolean focused) {
        if (rect == null || style == null) throw new IllegalArgumentException("frame inputs");
        List<Command> commands = new ArrayList<>(5);
        addVisible(commands, new UiRect(rect.x(), rect.y(), rect.width(), rect.height()), style.background());
        addVisible(commands, new UiRect(rect.x(), rect.y(), rect.width(), 1), style.top(focused));
        addVisible(commands, new UiRect(rect.x(), rect.bottom() - 1, rect.width(), 1), style.bottom(focused));
        addVisible(commands, new UiRect(rect.x(), rect.y(), 1, rect.height()), style.left(focused));
        addVisible(commands, new UiRect(rect.right() - 1, rect.y(), 1, rect.height()), style.right(focused));
        return new UiInputFramePlan(commands);
    }

    private static void addVisible(List<Command> commands, UiRect rect, int argb) {
        if ((argb >>> 24) != 0) commands.add(new Command(rect, argb));
    }

    public record Command(UiRect rect, int argb) {}
}
