package com.mo.economy_system.ui.core;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;

public sealed interface UiNavigation permits UiNavigation.None, UiNavigation.Back, UiNavigation.Route, UiNavigation.Target {
    record None() implements UiNavigation {
    }

    record Back() implements UiNavigation {
    }

    record Route(EconomyUiRoute route) implements UiNavigation {
    }

    record Target(String targetId) implements UiNavigation {
        public Target {
            if (targetId == null || targetId.isBlank()) throw new IllegalArgumentException("targetId cannot be blank");
        }
    }
}
