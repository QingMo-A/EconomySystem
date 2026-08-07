package com.mo.economy_system.ui.core;

import java.util.Objects;
import java.util.Optional;

/** Common behavior shell; target Screens only translate lifecycle and drawing APIs. */
public abstract class AbstractEconomyScreenController<S, E> {
    private S state;
    private UiNavigation pendingNavigation = new UiNavigation.None();

    protected AbstractEconomyScreenController(S initialState) {
        state = Objects.requireNonNull(initialState, "initialState");
    }

    public final S state() {
        return state;
    }

    protected final void replaceState(S nextState) {
        state = Objects.requireNonNull(nextState, "nextState");
    }

    protected final void navigate(UiNavigation navigation) {
        pendingNavigation = Objects.requireNonNull(navigation, "navigation");
    }

    public final Optional<UiNavigation> pollNavigation() {
        if (pendingNavigation instanceof UiNavigation.None) return Optional.empty();
        UiNavigation result = pendingNavigation;
        pendingNavigation = new UiNavigation.None();
        return Optional.of(result);
    }

    public abstract void handle(E event);
}
