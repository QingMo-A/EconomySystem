package com.mo.economy_system.common.market;

import java.util.UUID;

public interface TransactionalInventoryRemoval {
  UUID ownerId();

  long countMatching(Object template);

  InventoryRemovalResult removeMatching(Object template, int quantity);
}
