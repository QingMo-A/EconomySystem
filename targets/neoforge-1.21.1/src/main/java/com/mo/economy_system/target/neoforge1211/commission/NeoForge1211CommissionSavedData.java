package com.mo.economy_system.target.neoforge1211.commission;

import com.mo.economy_system.common.commission.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/** Durable NeoForge storage for personal commission state and reward idempotency records. */
public final class NeoForge1211CommissionSavedData extends SavedData {
  private static final String DATA_NAME = "economy_commissions";
  private final Map<UUID, CommissionPlayerState> players = new LinkedHashMap<>();
  private final Map<UUID, CommissionRewardRecord> rewards = new LinkedHashMap<>();

  public static NeoForge1211CommissionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
    NeoForge1211CommissionSavedData data = new NeoForge1211CommissionSavedData();
    ListTag rows = tag.getList("players", Tag.TAG_COMPOUND);
    for (Tag raw : rows) { try { data.readPlayer((CompoundTag) raw); } catch (RuntimeException ignored) {} }
    rows = tag.getList("rewards", Tag.TAG_COMPOUND);
    for (Tag raw : rows) { try { data.readReward((CompoundTag) raw); } catch (RuntimeException ignored) {} }
    return data;
  }

  @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    ListTag playerRows = new ListTag();
    players.values().forEach(state -> playerRows.add(writePlayer(state)));
    tag.put("players", playerRows);
    ListTag rewardRows = new ListTag();
    rewards.values().forEach(reward -> rewardRows.add(writeReward(reward)));
    tag.put("rewards", rewardRows);
    return tag;
  }

  public static NeoForge1211CommissionSavedData getInstance(ServerLevel level) {
    ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
    if (overworld == null) throw new IllegalStateException("Overworld is not loaded");
    return overworld.getDataStorage().computeIfAbsent(
        new SavedData.Factory<>(NeoForge1211CommissionSavedData::new, NeoForge1211CommissionSavedData::load), DATA_NAME);
  }

  public synchronized CommissionPlayerState loadState(UUID id) { return players.getOrDefault(id, CommissionPlayerState.empty(id)); }
  public synchronized void saveState(CommissionPlayerState state) { players.put(state.playerId(), state); setDirty(); }
  public synchronized Optional<CommissionRewardRecord> findReward(UUID id) { return Optional.ofNullable(rewards.get(id)); }
  public synchronized Optional<CommissionRewardRecord> findRewardByKey(String key) { return rewards.values().stream().filter(r -> r.idempotencyKey().equals(key)).findFirst(); }
  public synchronized CommissionRewardRecord createReward(CommissionRewardRecord candidate) {
    return findRewardByKey(candidate.idempotencyKey()).orElseGet(() -> { rewards.put(candidate.rewardRecordId(), candidate); setDirty(); return candidate; });
  }
  public synchronized void saveReward(CommissionRewardRecord reward) { rewards.put(reward.rewardRecordId(), reward); setDirty(); }
  public synchronized List<CommissionRewardRecord> rewardsFor(UUID player) { return rewards.values().stream().filter(r -> r.playerId().equals(player)).toList(); }

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
}
