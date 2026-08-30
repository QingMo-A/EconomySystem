package com.mo.economy_system.common.commission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable, atomically replaceable lookup of administrator-authored personal commission data. */
public record CommissionCatalog(
    List<CommissionTemplate> templates,
    Map<String, List<CommissionRequester>> requesterPools,
    Map<String, CommissionTargetPool> targetPools,
    PersonalCommissionSettings settings) {

  public CommissionCatalog {
    templates = copyUniqueTemplates(templates);
    requesterPools = copyRequesterPools(requesterPools);
    targetPools = copyTargetPools(targetPools);
    Objects.requireNonNull(settings, "settings");
  }

  public CommissionCatalog(
      List<CommissionTemplate> templates,
      Map<String, List<CommissionRequester>> requesterPools,
      Map<String, CommissionTargetPool> targetPools) {
    this(templates, requesterPools, targetPools, PersonalCommissionSettings.defaults());
  }

  public Optional<CommissionTemplate> template(String id) {
    if (id == null) return Optional.empty();
    return templates.stream().filter(value -> value.id().equals(id)).findFirst();
  }

  public List<CommissionRequester> requesters(String poolId) {
    if (poolId == null) return List.of();
    return requesterPools.getOrDefault(poolId, List.of());
  }

  public Optional<CommissionTargetPool> targetPool(String poolId) {
    if (poolId == null) return Optional.empty();
    return Optional.ofNullable(targetPools.get(poolId));
  }

  /** Returns only templates whose referenced pools contain at least one legal weighted entry. */
  public List<CommissionTemplate> legalTemplates() {
    return templates.stream()
        .filter(template -> !requesters(template.requesterPool()).isEmpty())
        .filter(template -> targetPool(template.targetPool()).map(pool -> !pool.targets().isEmpty()).orElse(false))
        .toList();
  }

  private static List<CommissionTemplate> copyUniqueTemplates(List<CommissionTemplate> values) {
    Objects.requireNonNull(values, "templates");
    List<CommissionTemplate> copy = List.copyOf(values);
    java.util.HashSet<String> ids = new java.util.HashSet<>();
    for (CommissionTemplate value : copy) {
      Objects.requireNonNull(value, "template");
      if (!ids.add(value.id())) throw new IllegalArgumentException("duplicate template: " + value.id());
    }
    return copy;
  }

  private static Map<String, List<CommissionRequester>> copyRequesterPools(
      Map<String, List<CommissionRequester>> values) {
    Objects.requireNonNull(values, "requesterPools");
    Map<String, List<CommissionRequester>> copy = new LinkedHashMap<>();
    for (Map.Entry<String, List<CommissionRequester>> entry : values.entrySet()) {
      String id = identifier(entry.getKey(), "requester pool");
      List<CommissionRequester> requesters = List.copyOf(Objects.requireNonNull(entry.getValue(), "requesters"));
      if (requesters.isEmpty()) throw new IllegalArgumentException("requester pool is empty: " + id);
      java.util.HashSet<String> ids = new java.util.HashSet<>();
      for (CommissionRequester requester : requesters) {
        Objects.requireNonNull(requester, "requester");
        if (!ids.add(requester.id())) throw new IllegalArgumentException("duplicate requester: " + requester.id());
      }
      if (copy.putIfAbsent(id, requesters) != null) throw new IllegalArgumentException("duplicate requester pool: " + id);
    }
    return Map.copyOf(copy);
  }

  private static Map<String, CommissionTargetPool> copyTargetPools(
      Map<String, CommissionTargetPool> values) {
    Objects.requireNonNull(values, "targetPools");
    Map<String, CommissionTargetPool> copy = new LinkedHashMap<>();
    for (Map.Entry<String, CommissionTargetPool> entry : values.entrySet()) {
      String id = identifier(entry.getKey(), "target pool");
      CommissionTargetPool pool = Objects.requireNonNull(entry.getValue(), "target pool");
      if (!id.equals(pool.id())) throw new IllegalArgumentException("target pool key does not match id: " + id);
      if (copy.putIfAbsent(id, pool) != null) throw new IllegalArgumentException("duplicate target pool: " + id);
    }
    return Map.copyOf(copy);
  }

  private static String identifier(String value, String field) {
    String normalized = Objects.requireNonNull(value, field).trim();
    if (normalized.isEmpty() || normalized.length() > 64 || !normalized.matches("[a-zA-Z0-9_.-]+")) {
      throw new IllegalArgumentException(field + " is invalid");
    }
    return normalized;
  }
}
