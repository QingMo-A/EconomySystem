package com.mo.economy_system.common.commission;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Programmatically generates personal commission instances from a validated catalog.
 *
 * <p>The generator has no Minecraft, account, item, or mailbox dependency. A target adapter can
 * supply legality data through the catalog and persist the returned state through a repository.
 */
public final class CommissionGenerator {
  public enum RefreshOutcome {
    NOT_DUE,
    GENERATED,
    AT_CAPACITY,
    NO_LEGAL_TEMPLATES,
    FAILED
  }

  public record RefreshResult(
      RefreshOutcome outcome,
      List<CommissionInstance> current,
      List<CommissionInstance> added,
      PersonalCommissionSchedule schedule,
      List<String> issues) {
    public RefreshResult {
      Objects.requireNonNull(outcome, "outcome");
      current = List.copyOf(Objects.requireNonNull(current, "current"));
      added = List.copyOf(Objects.requireNonNull(added, "added"));
      Objects.requireNonNull(schedule, "schedule");
      issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
    }

    public boolean generated() {
      return outcome == RefreshOutcome.GENERATED && !added.isEmpty();
    }
  }

  @FunctionalInterface
  public interface IdSource {
    UUID nextId();
  }

  private final CommissionCatalog catalog;
  private final CommissionRandom random;
  private final CommissionRewardPolicy rewardPolicy;
  private final IdSource idSource;

  public CommissionGenerator(CommissionCatalog catalog, CommissionRandom random) {
    this(catalog, random, CommissionRewardPolicy.defaultPolicy(), UUID::randomUUID);
  }

  public CommissionGenerator(
      CommissionCatalog catalog, CommissionRandom random, CommissionRewardPolicy rewardPolicy) {
    this(catalog, random, rewardPolicy, UUID::randomUUID);
  }

  public CommissionGenerator(
      CommissionCatalog catalog,
      CommissionRandom random,
      CommissionRewardPolicy rewardPolicy,
      IdSource idSource) {
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.random = Objects.requireNonNull(random, "random");
    this.rewardPolicy = Objects.requireNonNull(rewardPolicy, "rewardPolicy");
    this.idSource = Objects.requireNonNull(idSource, "idSource");
  }

  /** Returns the configured base interval for initializing a player's first schedule. */
  public long refreshIntervalMillis() {
    return catalog.settings().refreshBaseIntervalMillis();
  }

  /**
   * Generates due work and keeps every old instance, including instances which have just expired.
   * A due schedule is always advanced once; it is never coupled to an instance's expiresAt.
   */
  public RefreshResult refresh(
      UUID playerId,
      long nowMillis,
      Collection<CommissionInstance> existing,
      PersonalCommissionSchedule schedule) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(existing, "existing");
    Objects.requireNonNull(schedule, "schedule");
    if (!playerId.equals(schedule.playerId())) throw new IllegalArgumentException("schedule player mismatch");
    if (nowMillis < 0) throw new IllegalArgumentException("nowMillis must be non-negative");

    List<CommissionInstance> current = new ArrayList<>();
    for (CommissionInstance instance : existing) {
      Objects.requireNonNull(instance, "commission instance");
      if (!playerId.equals(instance.ownerPlayerId())) throw new IllegalArgumentException("commission owner mismatch");
      current.add(instance.expireIfDue(nowMillis));
    }
    if (!schedule.due(nowMillis)) {
      return new RefreshResult(RefreshOutcome.NOT_DUE, current, List.of(), schedule, List.of());
    }

    PersonalCommissionSchedule next;
    try {
      next = schedule.reschedule(nowMillis, nextRefreshAt(nowMillis));
    } catch (RuntimeException error) {
      return failed(current, schedule, "could not schedule next refresh: " + message(error));
    }

    int active = (int) current.stream()
        .filter(value -> value.status().countsAsActive())
        .filter(value -> value.expiresAt() > nowMillis)
        .count();
    List<CommissionTemplate> legalTemplates = catalog.legalTemplates().stream()
        .filter(template -> template.allowsPlayerCount(active))
        .toList();
    if (legalTemplates.isEmpty()) {
      return new RefreshResult(
          RefreshOutcome.NO_LEGAL_TEMPLATES, current, List.of(), next,
          List.of("no legal personal commission templates are configured"));
    }
    int remaining = catalog.settings().maxActivePersonalCommissions() - active;
    if (remaining <= 0) {
      return new RefreshResult(
          RefreshOutcome.AT_CAPACITY, current, List.of(), next,
          List.of("maximum active personal commission count reached"));
    }

    int requested;
    try {
      requested = inclusiveRandom(
          catalog.settings().minCommissionsPerRefresh(),
          catalog.settings().maxCommissionsPerRefresh());
    } catch (RuntimeException error) {
      return failed(current, schedule, "could not choose refresh count: " + message(error));
    }
    int count = Math.min(requested, remaining);
    List<CommissionInstance> additions = new ArrayList<>();
    try {
      for (int index = 0; index < count; index++) {
        additions.add(generateOne(playerId, nowMillis, legalTemplates));
      }
    } catch (RuntimeException error) {
      return failed(current, schedule, "commission generation failed: " + message(error));
    }
    current.addAll(additions);
    return new RefreshResult(RefreshOutcome.GENERATED, current, additions, next, List.of());
  }

  /** Generates a fixed number without changing a player's schedule; useful for admin preview/tools. */
  public List<CommissionInstance> generate(
      UUID playerId, long nowMillis, int count, Collection<CommissionInstance> existing) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(existing, "existing");
    if (nowMillis < 0 || count < 0) throw new IllegalArgumentException("invalid generation arguments");
    int active = (int) existing.stream()
        .filter(Objects::nonNull)
        .filter(value -> value.status().countsAsActive())
        .filter(value -> value.expiresAt() > nowMillis)
        .count();
    List<CommissionTemplate> legalTemplates = catalog.legalTemplates().stream()
        .filter(template -> template.allowsPlayerCount(active))
        .toList();
    if (count > catalog.settings().maxActivePersonalCommissions() - active) {
      throw new IllegalArgumentException("requested commissions exceed active limit");
    }
    List<CommissionInstance> result = new ArrayList<>();
    for (int index = 0; index < count; index++) result.add(generateOne(playerId, nowMillis, legalTemplates));
    return List.copyOf(result);
  }

  private CommissionInstance generateOne(
      UUID playerId, long nowMillis, List<CommissionTemplate> legalTemplates) {
    CommissionTemplate template = chooseTemplate(legalTemplates);
    CommissionRequester requester = choose(catalog.requesters(template.requesterPool()), CommissionRequester::weight);
    CommissionTargetPool.Target target = choose(
        catalog.targetPool(template.targetPool()).orElseThrow().targets(), CommissionTargetPool.Target::weight);
    int quantity = quantity(template, requester);
    long duration = duration(template);
    long expiresAt = Math.addExact(nowMillis, duration);
    CommissionRewardSnapshot reward = Objects.requireNonNull(
        rewardPolicy.calculate(template, requester, target.id(), quantity, random), "reward snapshot");
    String text = render(template.textTemplate(), template, requester, target.id(), quantity, reward.amount());
    return new CommissionInstance(
        Objects.requireNonNull(idSource.nextId(), "commission id"),
        playerId,
        template.id(),
        template.type(),
        requester.id(),
        requester.displayName(),
        target.id(),
        quantity,
        0,
        reward,
        nowMillis,
        expiresAt,
        CommissionStatus.AVAILABLE,
        text);
  }

  private CommissionTemplate chooseTemplate(List<CommissionTemplate> templates) {
    return choose(templates, value -> {
      Integer categoryWeight = catalog.settings().categoryWeights().get(value.category());
      return categoryWeight == null && !catalog.settings().categoryWeights().isEmpty()
          ? 0
          : Math.multiplyExact(value.weight(), categoryWeight == null ? 1 : categoryWeight);
    });
  }

  private int quantity(CommissionTemplate template, CommissionRequester requester) {
    int slots = (template.quantityMax() - template.quantityMin()) / template.quantityStep() + 1;
    int chosen = template.quantityMin() + Math.multiplyExact(random.nextInt(slots), template.quantityStep());
    long adjusted = Math.round(chosen * requester.quantityMultiplier());
    return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, adjusted));
  }

  private long duration(CommissionTemplate template) {
    long range = template.expirationMaxMillis() - template.expirationMinMillis();
    if (range == 0) return template.expirationMinMillis();
    if (range < 0) throw new IllegalArgumentException("expiration range overflow");
    long offset;
    if (range >= Integer.MAX_VALUE) {
      double roll = random.nextDouble();
      if (!Double.isFinite(roll) || roll < 0.0D || roll >= 1.0D) throw new IllegalArgumentException("invalid duration random");
      offset = (long) Math.floor(roll * (range + 1.0D));
    } else {
      offset = random.nextInt((int) range + 1);
    }
    return Math.addExact(template.expirationMinMillis(), offset);
  }

  private long nextRefreshAt(long nowMillis) {
    long interval = catalog.settings().refreshBaseIntervalMillis();
    long jitter = catalog.settings().refreshJitterMillis();
    long offset = jitter == 0 ? 0 : random.nextInt(Math.toIntExact(Math.multiplyExact(jitter, 2L) + 1L));
    long signedOffset = offset - jitter;
    return Math.addExact(nowMillis, Math.addExact(interval, signedOffset));
  }

  private int inclusiveRandom(int min, int max) {
    long width = (long) max - min + 1L;
    int offset = random.nextInt(Math.toIntExact(width));
    if (offset < 0 || offset >= width) throw new IllegalArgumentException("random count is out of range");
    return min + offset;
  }

  private <T> T choose(List<T> values, java.util.function.ToIntFunction<T> weight) {
    Objects.requireNonNull(values, "values");
    long total = 0;
    for (T value : values) {
      int entryWeight = weight.applyAsInt(Objects.requireNonNull(value, "weighted value"));
      if (entryWeight < 0) throw new IllegalArgumentException("weight overflow");
      total = Math.addExact(total, entryWeight);
    }
    if (total <= 0) throw new IllegalArgumentException("no positive weighted entries");
    // UUID-independent selection avoids an unbounded int cast when administrators use large weights.
    double roll = selectionRandom();
    long selected = (long) Math.floor(roll * total);
    long cursor = 0;
    for (T value : values) {
      int entryWeight = weight.applyAsInt(value);
      cursor += entryWeight;
      if (selected < cursor) return value;
    }
    return values.get(values.size() - 1);
  }

  private double selectionRandom() {
    double roll = random.nextDouble();
    if (!Double.isFinite(roll) || roll < 0.0D || roll >= 1.0D) {
      throw new IllegalArgumentException("selection random is invalid");
    }
    return roll;
  }

  private static String render(String template, CommissionTemplate definition,
      CommissionRequester requester, String target, int quantity, int reward) {
    String value = Objects.requireNonNullElse(template, "");
    return value.replace("{template}", definition.id())
        .replace("{requester}", requester.displayName())
        .replace("{target}", target)
        .replace("{count}", Integer.toString(quantity))
        .replace("{reward}", Integer.toString(reward));
  }

  private static RefreshResult failed(
      List<CommissionInstance> current, PersonalCommissionSchedule schedule, String issue) {
    return new RefreshResult(RefreshOutcome.FAILED, current, List.of(), schedule, List.of(issue));
  }

  private static String message(RuntimeException error) {
    return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
  }
}
