package com.mo.economy_system.protocol;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable, loader-neutral wire identity for one EconomySystem message. */
public record EconomyMessageSpec(
        int discriminator,
        String id,
        EconomyMessageDirection direction
) {
    private static final Pattern ID_PATTERN = Pattern.compile(
            "[a-z0-9_.-]+:[a-z0-9/._-]+"
    );

    public EconomyMessageSpec {
        if (discriminator < 0 || discriminator > 255) {
            throw new IllegalArgumentException(
                    "Message discriminator must fit the Forge 1.20.1 byte range: "
                            + discriminator
            );
        }

        Objects.requireNonNull(id, "id");
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid namespaced message id: " + id);
        }

        Objects.requireNonNull(direction, "direction");
    }
}
