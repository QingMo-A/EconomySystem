package com.mo.economy_system.common.market;

import java.util.UUID;

public interface TransactionalInventory {
    UUID ownerId();
    boolean canAccept(Object template, int quantity);
    InventoryInsertionResult insert(Object template, int quantity);
}
