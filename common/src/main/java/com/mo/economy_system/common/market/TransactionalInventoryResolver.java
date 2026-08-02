package com.mo.economy_system.common.market;

import java.util.Optional;
import java.util.UUID;

@FunctionalInterface
public interface TransactionalInventoryResolver {
    Optional<TransactionalInventory> resolve(UUID ownerId);
}
