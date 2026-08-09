package com.mo.economy_system.ui.renderer;

import com.mo.economy_system.ui.geometry.UiRect;

/** One loader-neutral rectangle fill in a shared chrome plan. */
public record UiFillCommand(UiRect rect, int argb) {
    public UiFillCommand {
        if (rect == null) {
            throw new IllegalArgumentException("rect cannot be null");
        }
    }
}
