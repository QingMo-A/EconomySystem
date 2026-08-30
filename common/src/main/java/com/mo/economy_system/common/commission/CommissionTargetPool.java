package com.mo.economy_system.common.commission;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A bounded, weighted set of legal targets from which an instance can be generated. */
public record CommissionTargetPool(String id, List<Target> targets) {
  public CommissionTargetPool {
    id = identifier(id, "id");
    targets = List.copyOf(Objects.requireNonNull(targets, "targets"));
    if (targets.isEmpty()) throw new IllegalArgumentException("target pool must not be empty");
    java.util.HashSet<String> ids = new java.util.HashSet<>();
    for (Target target : targets) {
      Objects.requireNonNull(target, "target");
      if (!ids.add(target.id())) throw new IllegalArgumentException("duplicate target: " + target.id());
    }
  }

  public CommissionTargetPool(String id, List<String> targetIds, boolean unweighted) {
    this(id, unweightedTargets(targetIds));
  }

  /** Convenience constructor for the common unweighted JSON shape. */
  public static CommissionTargetPool unweighted(String id, List<String> targetIds) {
    return new CommissionTargetPool(id, unweightedTargets(targetIds));
  }

  public List<String> targetIds() {
    return targets.stream().map(Target::id).toList();
  }

  private static List<Target> unweightedTargets(List<String> values) {
    Objects.requireNonNull(values, "targetIds");
    List<Target> result = new ArrayList<>();
    for (String value : values) result.add(new Target(value, 1));
    return result;
  }

  private static String identifier(String value, String field) {
    String normalized = Objects.requireNonNull(value, field).trim();
    if (normalized.isEmpty() || normalized.length() > 64 || !normalized.matches("[a-zA-Z0-9_.-]+")) {
      throw new IllegalArgumentException(field + " is invalid");
    }
    return normalized;
  }

  public record Target(String id, int weight) {
    public Target {
      id = Objects.requireNonNull(id, "id").trim();
      if (id.isEmpty() || id.length() > 256) throw new IllegalArgumentException("target id is invalid");
      if (weight <= 0) throw new IllegalArgumentException("target weight must be positive");
    }
  }
}
