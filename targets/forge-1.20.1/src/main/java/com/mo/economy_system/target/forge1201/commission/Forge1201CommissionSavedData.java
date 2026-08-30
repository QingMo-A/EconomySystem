package com.mo.economy_system.target.forge1201.commission;

import com.mo.economy_system.common.commission.CommissionInstance;
import com.mo.economy_system.common.commission.CommissionPlayerState;
import com.mo.economy_system.common.commission.CommissionRepository;
import com.mo.economy_system.common.commission.CommissionRewardRecord;
import com.mo.economy_system.common.commission.CommissionRewardRepository;
import com.mo.economy_system.common.commission.CommissionRewardSnapshot;
import com.mo.economy_system.common.commission.CommissionRewardStatus;
import com.mo.economy_system.common.commission.CommissionStatus;
import com.mo.economy_system.common.commission.CommissionType;
import com.mo.economy_system.common.commission.PersonalCommissionSchedule;
import com.mo.economy_system.common.commission.PublicCommission;
import com.mo.economy_system.common.commission.PublicCommissionRepository;
import com.mo.economy_system.common.commission.PublicCommissionStatus;
import com.mo.economy_system.common.recycle.RecycleService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Forge 1.20.1 persistence for personal commissions and their independent reward records.
 *
 * <p>The common commission service never sees this NBT representation.  Keeping both maps in one
 * SavedData gives completion and reward-mail creation a single server-side durable checkpoint and
 * makes the idempotency-key insert atomic on the server thread.
 */
public final class Forge1201CommissionSavedData extends SavedData
    implements CommissionRepository, CommissionRewardRepository {
  private static final String DATA_NAME = "economy_system_commissions";
  private static final int SCHEMA_VERSION = 1;

  private final Map<UUID, CommissionPlayerState> players = new LinkedHashMap<>();
  private final Map<UUID, CommissionRewardRecord> rewards = new LinkedHashMap<>();
  private final Map<String, UUID> rewardsByKey = new HashMap<>();
  private final Map<UUID, PublicCommission> publicCommissions = new LinkedHashMap<>();
  private final Set<String> acceptedSubmissionKeys = new LinkedHashSet<>();
  private RecycleService.State recycleState;

  private Forge1201CommissionSavedData() {}

  public static Forge1201CommissionSavedData get(ServerLevel level) {
    ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
    if (overworld == null) throw new IllegalStateException("Overworld is not loaded");
    return overworld.getDataStorage().computeIfAbsent(
        Forge1201CommissionSavedData::load, Forge1201CommissionSavedData::new, DATA_NAME);
  }

  public static Forge1201CommissionSavedData load(CompoundTag tag) {
    Forge1201CommissionSavedData data = new Forge1201CommissionSavedData();
    if (tag.isEmpty()) return data;
    int schema = tag.contains("schemaVersion") ? tag.getInt("schemaVersion") : 1;
    if (schema != SCHEMA_VERSION) {
      throw new IllegalArgumentException("Unsupported commission SavedData schema: " + schema);
    }
    data.readPlayers(tag);
    data.readRewards(tag);
    data.readPublicCommissions(tag);
    data.readAcceptedSubmissions(tag);
    data.readRecycleState(tag);
    return data;
  }

  public synchronized Optional<PublicCommission> findPublic(UUID commissionId) {
    return Optional.ofNullable(publicCommissions.get(commissionId));
  }

  public synchronized List<PublicCommission> listPublic() {
    return List.copyOf(publicCommissions.values());
  }

  public synchronized void savePublic(PublicCommission commission) {
    if (commission == null) throw new NullPointerException("commission");
    publicCommissions.put(commission.commissionId(), commission);
    setDirty();
  }

  public synchronized void removePublic(UUID commissionId) {
    if (commissionId == null) throw new NullPointerException("commissionId");
    if (publicCommissions.remove(commissionId) != null) setDirty();
  }

  @Override
  public synchronized CommissionPlayerState load(UUID playerId) {
    return players.get(playerId);
  }

  @Override
  public synchronized void save(CommissionPlayerState state) {
    if (state == null) throw new NullPointerException("state");
    players.put(state.playerId(), state);
    setDirty();
  }

  @Override
  public synchronized boolean hasAcceptedSubmission(UUID playerId, UUID commissionId,
      UUID submissionId) {
    return acceptedSubmissionKeys.contains(submissionKey(playerId, commissionId, submissionId));
  }

  @Override
  public synchronized void recordAcceptedSubmission(UUID playerId, UUID commissionId,
      UUID submissionId) {
    if (acceptedSubmissionKeys.add(submissionKey(playerId, commissionId, submissionId))) {
      setDirty();
    }
  }

  @Override
  public synchronized Optional<CommissionRewardRecord> find(UUID rewardRecordId) {
    return Optional.ofNullable(rewards.get(rewardRecordId));
  }

  @Override
  public synchronized Optional<CommissionRewardRecord> findByIdempotencyKey(String idempotencyKey) {
    if (idempotencyKey == null) return Optional.empty();
    UUID rewardId = rewardsByKey.get(idempotencyKey);
    return rewardId == null ? Optional.empty() : Optional.ofNullable(rewards.get(rewardId));
  }

  @Override
  public synchronized CommissionRewardRecord createIfAbsent(CommissionRewardRecord candidate) {
    if (candidate == null) throw new NullPointerException("candidate");
    CommissionRewardRecord existing = findByIdempotencyKey(candidate.idempotencyKey()).orElse(null);
    if (existing != null) return existing;
    CommissionRewardRecord byId = rewards.get(candidate.rewardRecordId());
    if (byId != null) {
      if (!byId.idempotencyKey().equals(candidate.idempotencyKey())) {
        throw new IllegalArgumentException("reward id is already used by another idempotency key");
      }
      return byId;
    }
    rewards.put(candidate.rewardRecordId(), candidate);
    rewardsByKey.put(candidate.idempotencyKey(), candidate.rewardRecordId());
    setDirty();
    return candidate;
  }

  @Override
  public synchronized void save(CommissionRewardRecord record) {
    if (record == null) throw new NullPointerException("record");
    CommissionRewardRecord existing = rewards.get(record.rewardRecordId());
    if (existing != null && !existing.idempotencyKey().equals(record.idempotencyKey())) {
      throw new IllegalArgumentException("reward idempotency key cannot change");
    }
    UUID keyed = rewardsByKey.get(record.idempotencyKey());
    if (keyed != null && !keyed.equals(record.rewardRecordId())) {
      throw new IllegalArgumentException("idempotency key is already used by another reward");
    }
    rewards.put(record.rewardRecordId(), record);
    rewardsByKey.put(record.idempotencyKey(), record.rewardRecordId());
    setDirty();
  }

  @Override
  public synchronized List<CommissionRewardRecord> listForPlayer(UUID playerId) {
    List<CommissionRewardRecord> result = new ArrayList<>();
    for (CommissionRewardRecord record : rewards.values()) {
      if (record.playerId().equals(playerId)) result.add(record);
    }
    return List.copyOf(result);
  }

  @Override
  public synchronized CompoundTag save(CompoundTag tag) {
    tag.putInt("schemaVersion", SCHEMA_VERSION);
    ListTag playerList = new ListTag();
    for (CommissionPlayerState state : players.values()) playerList.add(encodePlayer(state));
    tag.put("players", playerList);
    ListTag rewardList = new ListTag();
    for (CommissionRewardRecord record : rewards.values()) rewardList.add(encodeReward(record));
    tag.put("rewards", rewardList);
    ListTag publicList = new ListTag();
    for (PublicCommission commission : publicCommissions.values()) {
      publicList.add(encodePublicCommission(commission));
    }
    tag.put("publicCommissions", publicList);
    ListTag accepted = new ListTag();
    for (String key : acceptedSubmissionKeys) accepted.add(net.minecraft.nbt.StringTag.valueOf(key));
    tag.put("acceptedSubmissionKeys", accepted);
    if (recycleState != null) tag.put("recycleState", encodeRecycleState(recycleState));
    return tag;
  }

  public synchronized RecycleService.State loadRecycleState() {
    return recycleState;
  }

  public synchronized void saveRecycleState(RecycleService.State state) {
    recycleState = state;
    setDirty();
  }

  private void readRecycleState(CompoundTag root) {
    if (!root.contains("recycleState", Tag.TAG_COMPOUND)) return;
    CompoundTag row = root.getCompound("recycleState");
    Map<String, Integer> quotas = new LinkedHashMap<>();
    ListTag quotaList = row.getList("quotas", Tag.TAG_COMPOUND);
    for (int i = 0; i < quotaList.size(); i++) {
      CompoundTag quota = quotaList.getCompound(i);
      quotas.put(quota.getString("item"), Math.max(0, quota.getInt("remaining")));
    }
    Set<UUID> submissions = new HashSet<>();
    ListTag submissionList = row.getList("submissions", Tag.TAG_COMPOUND);
    for (int i = 0; i < submissionList.size(); i++) submissions.add(submissionList.getCompound(i).getUUID("id"));
    recycleState = new RecycleService.State(row.getLong("cycle"), quotas, submissions);
  }

  private static CompoundTag encodeRecycleState(RecycleService.State state) {
    CompoundTag row = new CompoundTag();
    row.putLong("cycle", state.cycleNumber());
    ListTag quotas = new ListTag();
    state.highRemaining().forEach((item, remaining) -> {
      CompoundTag value = new CompoundTag();
      value.putString("item", item);
      value.putInt("remaining", remaining);
      quotas.add(value);
    });
    row.put("quotas", quotas);
    ListTag submissions = new ListTag();
    state.completedSubmissions().forEach(id -> {
      CompoundTag value = new CompoundTag();
      value.putUUID("id", id);
      submissions.add(value);
    });
    row.put("submissions", submissions);
    return row;
  }

  private void readPublicCommissions(CompoundTag root) {
    if (!root.contains("publicCommissions", Tag.TAG_LIST)) return;
    ListTag encoded = root.getList("publicCommissions", Tag.TAG_COMPOUND);
    for (int index = 0; index < encoded.size(); index++) {
      PublicCommission commission = decodePublicCommission(encoded.getCompound(index));
      if (publicCommissions.put(commission.commissionId(), commission) != null) {
        throw new IllegalArgumentException("duplicate public commission id");
      }
    }
  }

  private void readAcceptedSubmissions(CompoundTag root) {
    if (!root.contains("acceptedSubmissionKeys", Tag.TAG_LIST)) return;
    ListTag encoded = root.getList("acceptedSubmissionKeys", Tag.TAG_STRING);
    for (int index = 0; index < encoded.size(); index++) {
      String key = encoded.getString(index);
      if (key.isBlank() || key.length() > 160) {
        throw new IllegalArgumentException("invalid accepted commission submission key");
      }
      if (!acceptedSubmissionKeys.add(key)) {
        throw new IllegalArgumentException("duplicate accepted commission submission key");
      }
    }
  }

  private static String submissionKey(UUID playerId, UUID commissionId, UUID submissionId) {
    return java.util.Objects.requireNonNull(playerId, "playerId") + ":"
        + java.util.Objects.requireNonNull(commissionId, "commissionId") + ":"
        + java.util.Objects.requireNonNull(submissionId, "submissionId");
  }

  private void readPlayers(CompoundTag root) {
    if (!root.contains("players", Tag.TAG_LIST)) return;
    ListTag encoded = root.getList("players", Tag.TAG_COMPOUND);
    Set<UUID> playerIds = new HashSet<>();
    for (int index = 0; index < encoded.size(); index++) {
      CompoundTag row = encoded.getCompound(index);
      UUID playerId = readUuid(row, "playerId");
      if (!playerIds.add(playerId)) throw new IllegalArgumentException("duplicate commission player");
      List<CommissionInstance> commissions = new ArrayList<>();
      if (row.contains("commissions", Tag.TAG_LIST)) {
        ListTag values = row.getList("commissions", Tag.TAG_COMPOUND);
        Set<UUID> commissionIds = new HashSet<>();
        for (int i = 0; i < values.size(); i++) {
          CommissionInstance commission = decodeCommission(values.getCompound(i));
          if (!playerId.equals(commission.ownerPlayerId())) {
            throw new IllegalArgumentException("commission owner does not match player");
          }
          if (!commissionIds.add(commission.commissionId())) {
            throw new IllegalArgumentException("duplicate commission id");
          }
          commissions.add(commission);
        }
      }
      PersonalCommissionSchedule schedule = null;
      if (row.contains("schedule", Tag.TAG_COMPOUND)) {
        CompoundTag scheduleTag = row.getCompound("schedule");
        UUID schedulePlayer = readUuid(scheduleTag, "playerId");
        if (!playerId.equals(schedulePlayer)) throw new IllegalArgumentException("schedule owner mismatch");
        schedule = new PersonalCommissionSchedule(
            schedulePlayer, scheduleTag.getLong("nextRefreshAt"), scheduleTag.getLong("lastRefreshAt"));
      }
      players.put(playerId, new CommissionPlayerState(playerId, commissions, schedule));
    }
  }

  private void readRewards(CompoundTag root) {
    if (!root.contains("rewards", Tag.TAG_LIST)) return;
    ListTag encoded = root.getList("rewards", Tag.TAG_COMPOUND);
    for (int index = 0; index < encoded.size(); index++) {
      CommissionRewardRecord record = decodeReward(encoded.getCompound(index));
      if (rewards.put(record.rewardRecordId(), record) != null) {
        throw new IllegalArgumentException("duplicate commission reward id");
      }
      UUID existing = rewardsByKey.put(record.idempotencyKey(), record.rewardRecordId());
      if (existing != null) throw new IllegalArgumentException("duplicate commission reward key");
    }
  }

  private static CompoundTag encodePlayer(CommissionPlayerState state) {
    CompoundTag row = new CompoundTag();
    row.putUUID("playerId", state.playerId());
    ListTag commissions = new ListTag();
    for (CommissionInstance commission : state.commissions()) commissions.add(encodeCommission(commission));
    row.put("commissions", commissions);
    if (state.schedule() != null) {
      CompoundTag schedule = new CompoundTag();
      schedule.putUUID("playerId", state.schedule().playerId());
      schedule.putLong("nextRefreshAt", state.schedule().nextRefreshAt());
      schedule.putLong("lastRefreshAt", state.schedule().lastRefreshAt());
      row.put("schedule", schedule);
    }
    return row;
  }

  private static CompoundTag encodeCommission(CommissionInstance value) {
    CompoundTag row = new CompoundTag();
    row.putUUID("commissionId", value.commissionId());
    row.putUUID("ownerPlayerId", value.ownerPlayerId());
    row.putString("templateId", value.templateId());
    row.putString("type", value.type().name());
    row.putString("requesterId", value.requesterId());
    row.putString("requesterName", value.requesterName());
    row.putString("targetSnapshot", value.targetSnapshot());
    row.putInt("requiredAmount", value.requiredAmount());
    row.putInt("progress", value.progress());
    row.put("reward", encodeSnapshot(value.rewardSnapshot()));
    row.putLong("generatedAt", value.generatedAt());
    row.putLong("expiresAt", value.expiresAt());
    row.putString("status", value.status().name());
    row.putString("text", value.text());
    return row;
  }

  private static CompoundTag encodeSnapshot(CommissionRewardSnapshot value) {
    CompoundTag row = new CompoundTag();
    row.putString("currencyId", value.currencyId());
    row.putInt("amount", value.amount());
    row.putString("description", value.description());
    return row;
  }

  private static CompoundTag encodeReward(CommissionRewardRecord value) {
    CompoundTag row = new CompoundTag();
    row.putUUID("rewardRecordId", value.rewardRecordId());
    row.putString("idempotencyKey", value.idempotencyKey());
    row.putUUID("playerId", value.playerId());
    row.putUUID("commissionId", value.commissionId());
    row.putString("batchId", value.batchId());
    row.putString("templateId", value.templateId());
    row.putString("requesterId", value.requesterId());
    row.put("reward", encodeSnapshot(value.rewardSnapshot()));
    row.putLong("createdAt", value.createdAt());
    if (value.mailId() != null) row.putUUID("mailId", value.mailId());
    row.putString("status", value.status().name());
    row.putLong("claimedAt", value.claimedAt());
    return row;
  }

  private static CompoundTag encodePublicCommission(PublicCommission value) {
    CompoundTag row = new CompoundTag();
    row.putUUID("commissionId", value.commissionId());
    row.putString("name", value.name());
    row.putString("requesterId", value.requesterId());
    row.putString("requesterName", value.requesterName());
    row.putString("targetSnapshot", value.targetSnapshot());
    row.putInt("targetAmount", value.targetAmount());
    row.putInt("unitReward", value.unitReward());
    row.putLong("generatedAt", value.generatedAt());
    row.putLong("expiresAt", value.expiresAt());
    row.putString("description", value.description());
    row.putString("status", value.status().name());
    row.putInt("remainingAmount", value.remainingAmount());
    row.putInt("remainingBudget", value.remainingBudget());
    return row;
  }

  private static PublicCommission decodePublicCommission(CompoundTag row) {
    return new PublicCommission(
        readUuid(row, "commissionId"),
        row.getString("name"),
        row.getString("requesterId"),
        row.getString("requesterName"),
        row.getString("targetSnapshot"),
        row.getInt("targetAmount"),
        row.getInt("unitReward"),
        row.getLong("generatedAt"),
        row.getLong("expiresAt"),
        row.getString("description"),
        enumValue(PublicCommissionStatus.class, row.getString("status"), "status"),
        row.getInt("remainingAmount"),
        row.getInt("remainingBudget"));
  }

  private static CommissionInstance decodeCommission(CompoundTag row) {
    return new CommissionInstance(
        readUuid(row, "commissionId"),
        readUuid(row, "ownerPlayerId"),
        row.getString("templateId"),
        enumValue(CommissionType.class, row.getString("type"), "type"),
        row.getString("requesterId"),
        row.getString("requesterName"),
        row.getString("targetSnapshot"),
        row.getInt("requiredAmount"),
        row.getInt("progress"),
        decodeSnapshot(requiredCompound(row, "reward")),
        row.getLong("generatedAt"),
        row.getLong("expiresAt"),
        enumValue(CommissionStatus.class, row.getString("status"), "status"),
        row.getString("text"));
  }

  private static CommissionRewardRecord decodeReward(CompoundTag row) {
    return new CommissionRewardRecord(
        readUuid(row, "rewardRecordId"),
        row.getString("idempotencyKey"),
        readUuid(row, "playerId"),
        readUuid(row, "commissionId"),
        row.getString("batchId"),
        row.getString("templateId"),
        row.getString("requesterId"),
        decodeSnapshot(requiredCompound(row, "reward")),
        row.getLong("createdAt"),
        row.contains("mailId", Tag.TAG_INT_ARRAY) ? row.getUUID("mailId") : null,
        enumValue(CommissionRewardStatus.class, row.getString("status"), "status"),
        row.getLong("claimedAt"));
  }

  private static CommissionRewardSnapshot decodeSnapshot(CompoundTag row) {
    return new CommissionRewardSnapshot(
        row.getString("currencyId"), row.getInt("amount"), row.getString("description"));
  }

  private static CompoundTag requiredCompound(CompoundTag row, String key) {
    if (!row.contains(key, Tag.TAG_COMPOUND)) throw new IllegalArgumentException("missing " + key);
    return row.getCompound(key);
  }

  private static UUID readUuid(CompoundTag row, String key) {
    if (!row.contains(key, Tag.TAG_INT_ARRAY)) throw new IllegalArgumentException("missing " + key);
    return row.getUUID(key);
  }

  private static <E extends Enum<E>> E enumValue(Class<E> type, String raw, String field) {
    try {
      return Enum.valueOf(type, raw);
    } catch (RuntimeException error) {
      throw new IllegalArgumentException("invalid commission " + field + ": " + raw, error);
    }
  }
}
