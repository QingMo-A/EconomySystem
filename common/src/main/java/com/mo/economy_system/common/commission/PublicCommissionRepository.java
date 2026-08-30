package com.mo.economy_system.common.commission;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Persistence boundary for public commissions. Implementations must replace snapshots atomically. */
public interface PublicCommissionRepository {
  Optional<PublicCommission> find(UUID commissionId);

  List<PublicCommission> list();

  void save(PublicCommission commission);

  default void remove(UUID commissionId) {
    Objects.requireNonNull(commissionId, "commissionId");
  }
}
