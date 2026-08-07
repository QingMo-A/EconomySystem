package com.mo.economy_system.common.client.ui;

import java.util.Optional;

/**
 * Target-owned page factory. The type parameter is the target's native screen
 * type, keeping version-specific rendering APIs outside common.
 */
public interface EconomyUiBridge<S> {
    Optional<S> create(EconomyUiRoute route);

    /** Returns whether this target can render the route without creating a screen. */
    boolean supports(EconomyUiRoute route);

    default Optional<S> createHome() {
        return create(EconomyUiRoute.HOME);
    }
}
