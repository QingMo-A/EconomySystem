package com.mo.economy_system.common.market;

import com.mo.economy_system.platform.item.ItemStackSnapshot;

import java.util.Objects;

/** Loader-neutral result of resolving a registered item's default form. */
public record ResolvedDemandItem(String canonicalItemId, ItemStackSnapshot template, int maxQuantity) {
    public ResolvedDemandItem {
        canonicalItemId = Objects.requireNonNull(canonicalItemId, "canonicalItemId");
        template = Objects.requireNonNull(template, "template");
        if (template.count() != 1) throw new IllegalArgumentException("demand template count must be one");
        if (maxQuantity < 1) throw new IllegalArgumentException("maxQuantity must be positive");
    }
}
