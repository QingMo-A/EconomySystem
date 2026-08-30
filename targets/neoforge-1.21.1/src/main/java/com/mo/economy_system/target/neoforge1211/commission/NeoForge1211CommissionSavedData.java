package com.mo.economy_system.target.neoforge1211.commission;

import com.mo.economy_system.common.commission.*;
import com.mo.economy_system.common.recycle.RecycleService;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/** Durable NeoForge storage for personal commission state and reward idempotency records. */
public final class NeoForge1211CommissionSavedData extends SavedData implements PublicCommissionRepository {
  private static final String DATA_NAME = "economy_commissions";
  private final Map<UUID, CommissionPlayerState> players = new LinkedHashMap<>();
  private final Map<UUID, CommissionRewardRecord> rewards = new LinkedHashMap<>();
  private final Map<UUID, PublicCommission> publicCommissions = new LinkedHashMap<>();
  private final Set<String> acceptedSubmissionKeys = new LinkedHashSet<>();
  private RecycleService.State recycleState;

  public static NeoForge1211CommissionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
    NeoForge1211CommissionSavedData data = new NeoForge1211CommissionSavedData();
    ListTag rows = tag.getList("players", Tag.TAG_COMPOUND);
    for (Tag raw : rows) { data.readPlayer((CompoundTag) raw); }
    rows = tag.getList("rewards", Tag.TAG_COMPOUND);
    for (Tag raw : rows) { data.readReward((CompoundTag) raw); }
    rows = tag.getList("publicCommissions", Tag.TAG_COMPOUND);
    for (Tag raw : rows) {
      PublicCommission commission = readPublic((CompoundTag) raw);
      if (data.publicCommissions.put(commission.commissionId(), commission) != null) {
        throw new IllegalArgumentException("duplicate public commission id");
      }
    }
    for (Tag raw : tag.getList("acceptedSubmissionKeys", Tag.TAG_STRING)) {
      String key = raw.getAsString();
      if (key.isBlank() || key.length() > 160 || !data.acceptedSubmissionKeys.add(key)) {
        throw new IllegalArgumentException("invalid or duplicate accepted commission submission key");
      }
    }
    if (tag.contains("recycleState", Tag.TAG_COMPOUND)) {
      CompoundTag state = tag.getCompound("recycleState");
      Map<String, Integer> quotas = new LinkedHashMap<>();
      for (Tag raw : state.getList("quotas", Tag.TAG_COMPOUND)) {
        CompoundTag quota = (CompoundTag) raw;
        quotas.put(quota.getString("item"), Math.max(0, quota.getInt("remaining")));
      }
      Set<UUID> submissions = new HashSet<>();
      for (Tag raw : state.getList("submissions", Tag.TAG_COMPOUND)) {
        submissions.add(((CompoundTag) raw).getUUID("id"));
      }
      data.recycleState = new RecycleService.State(state.getLong("cycle"), quotas, submissions);
    }
    return data;
  }

  @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    ListTag playerRows = new ListTag();
    players.values().forEach(state -> playerRows.add(writePlayer(state)));
    tag.put("players", playerRows);
    ListTag rewardRows = new ListTag();
    rewards.values().forEach(reward -> rewardRows.add(writeReward(reward)));
    tag.put("rewards", rewardRows);
    ListTag publicRows = new ListTag();
    publicCommissions.values().forEach(commission -> publicRows.add(writePublic(commission)));
    tag.put("publicCommissions", publicRows);
    ListTag accepted = new ListTag();
    acceptedSubmissionKeys.forEach(key -> accepted.add(net.minecraft.nbt.StringTag.valueOf(key)));
    tag.put("acceptedSubmissionKeys", accepted);
    if (recycleState != null) {
      CompoundTag state = new CompoundTag();
      state.putLong("cycle", recycleState.cycleNumber());
      ListTag quotas = new ListTag();
      recycleState.highRemaining().forEach((item, remaining) -> {
        CompoundTag row = new CompoundTag();
        row.putString("item", item);
        row.putInt("remaining", remaining);
        quotas.add(row);
      });
      state.put("quotas", quotas);
      ListTag submissions = new ListTag();
      recycleState.completedSubmissions().forEach(id -> {
        CompoundTag row = new CompoundTag();
        row.putUUID("id", id);
        submissions.add(row);
      });
      state.put("submissions", submissions);
      tag.put("recycleState", state);
    }
    return tag;
  }

  @Override public synchronized Optional<PublicCommission> find(UUID id) {
    return Optional.ofNullable(publicCommissions.get(Objects.requireNonNull(id, "commissionId")));
  }
  @Override public synchronized List<PublicCommission> list() { return List.copyOf(publicCommissions.values()); }
  @Override public synchronized void save(PublicCommission commission) {
    publicCommissions.put(Objects.requireNonNull(commission, "commission").commissionId(), commission);
    setDirty();
  }
  @Override public synchronized void remove(UUID id) {
    if (publicCommissions.remove(Objects.requireNonNull(id, "commissionId")) != null) setDirty();
  }

  public static NeoForge1211CommissionSavedData getInstance(ServerLevel level) {
    ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
    if (overworld == null) throw new IllegalStateException("Overworld is not loaded");
    return overworld.getDataStorage().computeIfAbsent(
        new SavedData.Factory<>(NeoForge1211CommissionSavedData::new, NeoForge1211CommissionSavedData::load), DATA_NAME);
  }

  public synchronized CommissionPlayerState loadState(UUID id) { return players.getOrDefault(id, CommissionPlayerState.empty(id)); }
  public synchronized void saveState(CommissionPlayerState state) { players.put(state.playerId(), state); setDirty(); }
  public synchronized boolean hasAcceptedSubmission(UUID playerId, UUID commissionId, UUID submissionId) {
    return acceptedSubmissionKeys.contains(submissionKey(playerId, commissionId, submissionId));
  }
  public synchronized void recordAcceptedSubmission(UUID playerId, UUID commissionId, UUID submissionId) {
    if (acceptedSubmissionKeys.add(submissionKey(playerId, commissionId, submissionId))) setDirty();
  }
  public synchronized Optional<CommissionRewardRecord> findReward(UUID id) { return Optional.ofNullable(rewards.get(id)); }
  public synchronized Optional<CommissionRewardRecord> findRewardByKey(String key) { return rewards.values().stream().filter(r -> r.idempotencyKey().equals(key)).findFirst(); }
  public synchronized CommissionRewardRecord createReward(CommissionRewardRecord candidate) {
    return findRewardByKey(candidate.idempotencyKey()).orElseGet(() -> { rewards.put(candidate.rewardRecordId(), candidate); setDirty(); return candidate; });
  }
  public synchronized void saveReward(CommissionRewardRecord reward) { rewards.put(reward.rewardRecordId(), reward); setDirty(); }
  public synchronized List<CommissionRewardRecord> rewardsFor(UUID player) { return rewards.values().stream().filter(r -> r.playerId().equals(player)).toList(); }
  public synchronized RecycleService.State loadRecycleState() { return recycleState; }
  public synchronized void saveRecycleState(RecycleService.State state) { recycleState = state; setDirty(); }

  private static String submissionKey(UUID playerId, UUID commissionId, UUID submissionId) {
    return Objects.requireNonNull(playerId, "playerId") + ":"
        + Objects.requireNonNull(commissionId, "commissionId") + ":"
        + Objects.requireNonNull(submissionId, "submissionId");
  }

  private CompoundTag writePlayer(CommissionPlayerState state) {
    CompoundTag out = new CompoundTag(); out.putUUID("id", state.playerId());
    if (state.schedule() != null) { out.putLong("next", state.schedule().nextRefreshAt()); out.putLong("last", state.schedule().lastRefreshAt()); }
    ListTag list = new ListTag(); state.commissions().forEach(c -> { CompoundTag row = new CompoundTag(); row.putUUID("id", c.commissionId()); row.putString("template", c.templateId()); row.putString("type", c.type().name()); row.putString("requester", c.requesterId()); row.putString("requesterName", c.requesterName()); row.putString("target", c.targetSnapshot()); row.putInt("required", c.requiredAmount()); row.putInt("progress", c.progress()); row.putInt("reward", c.rewardSnapshot().amount()); row.putString("rewardDesc", c.rewardSnapshot().description()); row.putLong("generated", c.generatedAt()); row.putLong("expires", c.expiresAt()); row.putString("status", c.status().name()); row.putString("text", c.text()); list.add(row); }); out.put("commissions", list); return out;
  }
  private void readPlayer(CompoundTag row) {
    UUID id = row.getUUID("id"); List<CommissionInstance> list = new ArrayList<>();
    for (Tag raw : row.getList("commissions", Tag.TAG_COMPOUND)) { CompoundTag c = (CompoundTag) raw; list.add(new CommissionInstance(c.getUUID("id"), id, c.getString("template"), CommissionType.valueOf(c.getString("type")), c.getString("requester"), c.getString("requesterName"), c.getString("target"), c.getInt("required"), c.getInt("progress"), new CommissionRewardSnapshot(c.getInt("reward")), c.getLong("generated"), c.getLong("expires"), CommissionStatus.valueOf(c.getString("status")), c.getString("text"))); }
    PersonalCommissionSchedule schedule = row.contains("next", Tag.TAG_LONG) ? new PersonalCommissionSchedule(id, row.getLong("next"), row.getLong("last")) : null; players.put(id, new CommissionPlayerState(id, list, schedule));
  }
  private CompoundTag writeReward(CommissionRewardRecord r) { CompoundTag out = new CompoundTag(); out.putUUID("id", r.rewardRecordId()); out.putString("key", r.idempotencyKey()); out.putUUID("player", r.playerId()); out.putUUID("commission", r.commissionId()); out.putString("batch", r.batchId()); out.putString("template", r.templateId()); out.putString("requester", r.requesterId()); out.putInt("amount", r.rewardSnapshot().amount()); out.putString("description", r.rewardSnapshot().description()); out.putLong("created", r.createdAt()); if (r.mailId()!=null) out.putUUID("mail", r.mailId()); out.putString("status", r.status().name()); out.putLong("claimed", r.claimedAt()); return out; }
  private void readReward(CompoundTag row) { UUID id=row.getUUID("id"); UUID mail=row.hasUUID("mail")?row.getUUID("mail"):null; CommissionRewardRecord r=new CommissionRewardRecord(id,row.getString("key"),row.getUUID("player"),row.getUUID("commission"),row.getString("batch"),row.getString("template"),row.getString("requester"),new CommissionRewardSnapshot(CommissionRewardSnapshot.DEFAULT_CURRENCY_ID, row.getInt("amount"), row.getString("description")),row.getLong("created"),mail,CommissionRewardStatus.valueOf(row.getString("status")),row.getLong("claimed")); rewards.put(id,r); }
  private static CompoundTag writePublic(PublicCommission c) { CompoundTag out=new CompoundTag(); out.putUUID("id",c.commissionId()); out.putString("name",c.name()); out.putString("requesterId",c.requesterId()); out.putString("requesterName",c.requesterName()); out.putString("target",c.targetSnapshot()); out.putInt("targetAmount",c.targetAmount()); out.putInt("unitReward",c.unitReward()); out.putLong("generated",c.generatedAt()); out.putLong("expires",c.expiresAt()); out.putString("description",c.description()); out.putString("status",c.status().name()); out.putInt("remaining",c.remainingAmount()); out.putInt("budget",c.remainingBudget()); return out; }
  private static PublicCommission readPublic(CompoundTag c) { return new PublicCommission(c.getUUID("id"), c.getString("name"), c.getString("requesterId"), c.getString("requesterName"), c.getString("target"), c.getInt("targetAmount"), c.getInt("unitReward"), c.getLong("generated"), c.getLong("expires"), c.getString("description"), PublicCommissionStatus.valueOf(c.getString("status")), c.getInt("remaining"), c.getInt("budget")); }
}
