package com.mo.economy_system.common.commission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Deterministic in-memory public commission repository for tests and server bootstrap. */
public final class InMemoryPublicCommissionRepository implements PublicCommissionRepository {
  private final Map<UUID, PublicCommission> values = new LinkedHashMap<>();

  @Override public synchronized Optional<PublicCommission> find(UUID id) {
    return Optional.ofNullable(values.get(Objects.requireNonNull(id, "commissionId")));
  }

  @Override public synchronized List<PublicCommission> list() {
    return List.copyOf(new ArrayList<>(values.values()));
  }

  @Override public synchronized void save(PublicCommission commission) {
    Objects.requireNonNull(commission, "commission");
    values.put(commission.commissionId(), commission);
  }

  @Override public synchronized void remove(UUID id) {
    values.remove(Objects.requireNonNull(id, "commissionId"));
  }
}
