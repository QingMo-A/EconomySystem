package com.mo.economy_system.ui.core;

public sealed interface UiEvent permits UiEvent.Click, UiEvent.Scroll, UiEvent.Key, UiEvent.Tick {
    record Click(int x, int y, int button) implements UiEvent {
    }

    record Scroll(double delta) implements UiEvent {
    }

    record Key(int key, int modifiers) implements UiEvent {
    }

    record Tick(long nowNanos) implements UiEvent {
    }
}
