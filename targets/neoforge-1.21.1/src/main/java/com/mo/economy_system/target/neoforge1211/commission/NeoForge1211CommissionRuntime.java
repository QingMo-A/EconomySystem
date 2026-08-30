package com.mo.economy_system.target.neoforge1211.commission;

import com.mo.economy_system.common.commission.*;
import com.mo.economy_system.common.mail.MailRecord;
import com.mo.economy_system.common.mail.MailType;
import com.mo.economy_system.common.mail.MailboxCapacityPolicy;
import com.mo.economy_system.core.economy_system.mailbox.MailboxSavedData;
import com.mo.economy_system.target.neoforge1211.NeoForge1211Platform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.*;
import java.nio.charset.StandardCharsets;

/** NeoForge lifecycle bridge for the common personal commission service. */
public final class NeoForge1211CommissionRuntime {
  private static final Map<MinecraftServer, CommissionService> SERVICES = new WeakHashMap<>();
  private static final Map<MinecraftServer, PublicCommissionService> PUBLIC_SERVICES = new WeakHashMap<>();
  private static final Map<MinecraftServer, CommissionCatalog> CATALOGS = new WeakHashMap<>();
  private NeoForge1211CommissionRuntime() {}

  public static synchronized void initialize(MinecraftServer server) {
    CATALOGS.put(server, loadCatalog(server));
  }

  public static synchronized CommissionService service(MinecraftServer server) {
    return SERVICES.computeIfAbsent(server, s -> {
      NeoForge1211CommissionSavedData data = NeoForge1211CommissionSavedData.getInstance(s.overworld());
      return new CommissionService(new CommissionGenerator(catalog(s), new CommissionRandom() {
            private final java.util.Random random = new java.util.Random();
            public double nextDouble() { return random.nextDouble(); }
            public int nextInt(int bound) { return random.nextInt(bound); }
          }),
          new CommissionRepository() {
            public CommissionPlayerState load(UUID id) { return data.loadState(id); }
            public void save(CommissionPlayerState state) { data.saveState(state); }
            public boolean hasAcceptedSubmission(UUID playerId, UUID commissionId, UUID submissionId) {
              return data.hasAcceptedSubmission(playerId, commissionId, submissionId);
            }
            public void recordAcceptedSubmission(UUID playerId, UUID commissionId, UUID submissionId) {
              data.recordAcceptedSubmission(playerId, commissionId, submissionId);
            }
          },
          new CommissionRewardRepository() { public Optional<CommissionRewardRecord> find(UUID id) { return data.findReward(id); } public Optional<CommissionRewardRecord> findByIdempotencyKey(String key) { return data.findRewardByKey(key); } public CommissionRewardRecord createIfAbsent(CommissionRewardRecord candidate) { return data.createReward(candidate); } public void save(CommissionRewardRecord r) { data.saveReward(r); } public List<CommissionRewardRecord> listForPlayer(UUID id) { return data.rewardsFor(id); } },
          new Delivery(data, s));
    });
  }

  public static CommissionService.RefreshView refresh(ServerPlayer player) {
    return service(player.server).refresh(player.getUUID(), System.currentTimeMillis());
  }

  /** Forces an administrator refresh while retaining all existing personal commissions. */
  public static CommissionService.RefreshView forceRefresh(ServerPlayer player) {
    return service(player.server).forceRefresh(player.getUUID(), System.currentTimeMillis());
  }

  /** Retries personal and public commission mail delivery without requiring another UI packet. */
  public static void retryPendingRewards(ServerPlayer player) {
    service(player.server).retryPendingRewards(player.getUUID());
    publicService(player.server).retryPendingRewards(player.getUUID());
  }
  public static CommissionPlayerState state(ServerPlayer player) { return service(player.server).refresh(player.getUUID(), System.currentTimeMillis()).state(); }
  public static synchronized List<String> templateIds(MinecraftServer server) { return catalog(server).templates().stream().map(CommissionTemplate::id).toList(); }
  public static synchronized int maxActivePersonalCommissions(MinecraftServer server) {
    return catalog(server).settings().maxActivePersonalCommissions();
  }

  /** Marks due public commissions as expired independently of player presence or UI access. */
  public static int expirePublic(MinecraftServer server) {
    if (server == null) return 0;
    return publicService(server).expireDue(Math.max(1L, System.currentTimeMillis()));
  }

  public static int reloadCommand(net.minecraft.commands.CommandSourceStack source) {
    try {
      MinecraftServer server = source.getServer();
      CATALOGS.put(server, loadCatalog(server));
      SERVICES.remove(server);
      PUBLIC_SERVICES.remove(server);
      source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("个人委托库已重载。"), true);
      return 1;
    } catch (RuntimeException failure) {
      source.sendFailure(net.minecraft.network.chat.Component.literal("个人委托库重载失败: "
          + (failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage())));
      return 0;
    }
  }

  public static synchronized PublicCommissionService publicService(MinecraftServer server) {
    return PUBLIC_SERVICES.computeIfAbsent(server, s -> {
      NeoForge1211CommissionSavedData data = NeoForge1211CommissionSavedData.getInstance(s.overworld());
      CommissionRewardRepository rewards = rewardRepository(data);
      return new PublicCommissionService(data, rewards, new Delivery(data, s));
    });
  }

  public static PublicCommission createPublic(MinecraftServer server, String name, String requesterId,
                                               String requesterName, String target, int targetAmount,
                                               int unitReward, long expiresAt, String description) {
    validatePublicTarget(target);
    long now = Math.max(1L, System.currentTimeMillis());
    PublicCommission commission = PublicCommission.create(UUID.randomUUID(), name, requesterId,
        requesterName, target, targetAmount, unitReward, now, expiresAt, description);
    publicService(server).create(commission);
    return commission;
  }

  private static void validatePublicTarget(String target) {
    String normalized = target == null ? "" : target.trim();
    net.minecraft.resources.ResourceLocation id =
        net.minecraft.resources.ResourceLocation.tryParse(normalized);
    if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
      throw new IllegalArgumentException("公共委托目标物品未注册: " + normalized);
    }
    Item item = BuiltInRegistries.ITEM.get(id);
    if (item == null || item == Items.AIR) {
      throw new IllegalArgumentException("公共委托目标物品不可用: " + normalized);
    }
  }

  public static List<PublicCommission> listPublic(MinecraftServer server) {
    return publicService(server).list(Math.max(1L, System.currentTimeMillis()));
  }

  public static Optional<PublicCommission> findPublic(MinecraftServer server, UUID id) {
    return publicService(server).list(Math.max(1L, System.currentTimeMillis())).stream()
        .filter(c -> c.commissionId().equals(id)).findFirst();
  }

  public static boolean cancelPublic(MinecraftServer server, UUID id) { return publicService(server).cancel(id); }

  public static void removePublic(MinecraftServer server, UUID id) { publicService(server).remove(id); }

  public static PublicCommissionService.SubmitResult submitPublicItem(ServerPlayer player, UUID id, int requested) {
    return submitPublicItem(player, id, UUID.randomUUID(), requested);
  }

  public static PublicCommissionService.SubmitResult submitPublicItem(ServerPlayer player, UUID id,
                                                                        UUID submissionId, int requested) {
    if (requested <= 0) throw new IllegalArgumentException("提交数量必须大于 0");
    java.util.Objects.requireNonNull(submissionId, "submissionId");
    PublicCommissionService service = publicService(player.server);
    PublicCommission commission = findPublic(player.server, id).orElseThrow(() -> new IllegalArgumentException("找不到公共委托"));
    if (commission.status() != PublicCommissionStatus.AVAILABLE) {
      return service.submit(player.getUUID(), id, submissionId, requested, System.currentTimeMillis());
    }
    int amount = Math.min(requested, commission.remainingAmount());
    if (amount <= 0) {
      return service.submit(player.getUUID(), id, submissionId, requested,
          System.currentTimeMillis());
    }
    var itemId = net.minecraft.resources.ResourceLocation.tryParse(commission.targetSnapshot());
    if (itemId == null) throw new IllegalArgumentException("目标物品 ID 无效");
    var target = BuiltInRegistries.ITEM.get(itemId);
    if (target == null || target == net.minecraft.world.item.Items.AIR) throw new IllegalArgumentException("目标物品不可用");
    int available = countMatching(player, target);
    if (available < amount) throw new IllegalArgumentException("背包中没有足够的目标物品");
    List<ItemStack> originals = new ArrayList<>();
    for (ItemStack stack : player.getInventory().items) originals.add(stack.copy());
    consumeMatching(player, target, amount);
    try {
      PublicCommissionService.SubmitResult result = service.submit(player.getUUID(), id, submissionId, amount, System.currentTimeMillis());
      if (result.acceptedAmount() != amount) {
        restoreInventory(player, originals);
        consumeMatching(player, target, result.acceptedAmount());
      }
      player.containerMenu.broadcastChanges();
      return result;
    } catch (RuntimeException failure) {
      restoreInventory(player, originals);
      player.containerMenu.broadcastChanges();
      throw failure;
    }
  }

  /** Synchronizes the durable reward record after its mailbox currency attachment is claimed. */
  public static void markRewardClaimed(ServerPlayer player, UUID rewardRecordId) {
    if (rewardRecordId == null) return;
    try {
      NeoForge1211CommissionSavedData data = NeoForge1211CommissionSavedData.getInstance(player.serverLevel());
      data.findReward(rewardRecordId).ifPresent(record -> {
        if (record.status() != CommissionRewardStatus.CLAIMED) {
          data.saveReward(record.claimed(Math.max(1L, System.currentTimeMillis())));
        }
      });
    } catch (RuntimeException failure) {
      // The mailbox marker is already the duplicate-claim guard. Keep the currency in the account
      // and retry only this audit marker later; never debit a successful claim.
      com.mo.economy_system.EconomySystem.LOGGER.error(
          "Commission reward claim marker reconciliation failed reward={}",
          rewardRecordId, failure);
    }
  }

  public static CommissionService.SubmitResult submitItem(ServerPlayer player, UUID id, int amount) {
    return submitItem(player, id, UUID.randomUUID(), amount);
  }

  public static CommissionService.SubmitResult submitItem(ServerPlayer player, UUID id,
                                                            UUID submissionId, int amount) {
    if (amount <= 0) {
      return service(player.server).submitProgress(player.getUUID(), id, submissionId, amount,
          System.currentTimeMillis());
    }
    CommissionPlayerState state = state(player);
    CommissionInstance commission = state.commissions().stream().filter(c -> c.commissionId().equals(id)).findFirst().orElse(null);
    if (commission == null) {
      return service(player.server).submitProgress(player.getUUID(), id, submissionId, amount,
          System.currentTimeMillis());
    }
    if (commission.type() != CommissionType.ITEM_DELIVERY) {
      throw new IllegalArgumentException("该委托不是物资提交委托");
    }
    if (commission.status().terminal()) {
      return service(player.server).submitProgress(player.getUUID(), id, submissionId, amount,
          System.currentTimeMillis());
    }
    int acceptedAmount = Math.min(amount,
        Math.max(0, commission.requiredAmount() - commission.progress()));
    if (acceptedAmount <= 0) {
      return service(player.server).submitProgress(player.getUUID(), id, submissionId, amount,
          System.currentTimeMillis());
    }
    var itemId = net.minecraft.resources.ResourceLocation.tryParse(commission.targetSnapshot());
    if (itemId == null) throw new IllegalStateException("委托目标物品 ID 无效");
    Item target = BuiltInRegistries.ITEM.get(itemId);
    if (target == null || target == net.minecraft.world.item.Items.AIR) {
      throw new IllegalStateException("委托目标物品不可用");
    }
    if (countMatching(player, target) < acceptedAmount) {
      throw new IllegalStateException("背包中没有足够的目标物品");
    }
    List<ItemStack> originals = new ArrayList<>();
    for (ItemStack stack : player.getInventory().items) originals.add(stack.copy());
    consumeMatching(player, target, acceptedAmount);
    try {
      CommissionService.SubmitResult result = service(player.server).submitProgress(
          player.getUUID(), id, submissionId, acceptedAmount, System.currentTimeMillis());
      if (!result.accepted()) restoreInventory(player, originals);
      player.containerMenu.broadcastChanges();
      return result;
    } catch (RuntimeException failure) {
      restoreInventory(player, originals);
      player.containerMenu.broadcastChanges();
      throw failure;
    }
  }

  /** Compatibility entry point for callers without an authoritative event identity. */
  @Deprecated
  public static void onKill(ServerPlayer player, LivingEntity entity) {
    onKill(player, entity, UUID.randomUUID());
  }

  /** ENTITY_KILL progress with the killed entity UUID as a stable event identity. */
  public static void onKill(ServerPlayer player, LivingEntity entity, UUID killedEntityId) {
    if (player == null || entity == null || killedEntityId == null) return;
    String target = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    UUID submissionId = CommissionEventIds.entityKill(player.getUUID(), killedEntityId);
    CommissionPlayerState state = state(player);
    for (CommissionInstance c : state.commissions()) {
      if (c.type() == CommissionType.ENTITY_KILL && c.targetSnapshot().equals(target)
          && c.status().countsAsActive()) {
        service(player.server).submitProgress(player.getUUID(), c.commissionId(), submissionId, 1,
            System.currentTimeMillis());
      }
    }
  }

  public static void clear(MinecraftServer server) { SERVICES.remove(server); PUBLIC_SERVICES.remove(server); CATALOGS.remove(server); }

  private static int countMatching(ServerPlayer player, Item target) {
    int count = 0;
    ItemStack template = new ItemStack(target);
    for (ItemStack stack : player.getInventory().items) {
      if (NeoForge1211Platform.nativeItemStacks().sameItemAndData(stack, template)) {
        count = Math.addExact(count, stack.getCount());
      }
    }
    return count;
  }

  private static void consumeMatching(ServerPlayer player, Item target, int amount) {
    if (amount <= 0) return;
    int remaining = amount;
    ItemStack template = new ItemStack(target);
    for (ItemStack stack : player.getInventory().items) {
      if (remaining == 0) break;
      if (!NeoForge1211Platform.nativeItemStacks().sameItemAndData(stack, template)) continue;
      int take = Math.min(remaining, stack.getCount());
      stack.shrink(take);
      remaining -= take;
    }
    if (remaining != 0) throw new IllegalStateException("背包在提交期间发生变化");
  }

  private static void restoreInventory(ServerPlayer player, List<ItemStack> originals) {
    if (originals.size() != player.getInventory().items.size()) {
      throw new IllegalStateException("背包槽位在提交期间发生变化");
    }
    for (int index = 0; index < originals.size(); index++) {
      player.getInventory().items.set(index, originals.get(index).copy());
    }
  }

  private static CommissionRewardRepository rewardRepository(NeoForge1211CommissionSavedData data) {
    return new CommissionRewardRepository() {
      public Optional<CommissionRewardRecord> find(UUID id) { return data.findReward(id); }
      public Optional<CommissionRewardRecord> findByIdempotencyKey(String key) { return data.findRewardByKey(key); }
      public CommissionRewardRecord createIfAbsent(CommissionRewardRecord candidate) { return data.createReward(candidate); }
      public void save(CommissionRewardRecord reward) { data.saveReward(reward); }
      public List<CommissionRewardRecord> listForPlayer(UUID id) { return data.rewardsFor(id); }
    };
  }

  private static CommissionCatalog catalog(MinecraftServer server) {
    return CATALOGS.computeIfAbsent(server, NeoForge1211CommissionRuntime::loadCatalog);
  }

  private static CommissionCatalog loadCatalog(MinecraftServer server) {
    java.nio.file.Path path = server.getServerDirectory()
        .resolve("config").resolve("economysystem").resolve("commissions").resolve("catalog.json");
    CommissionCatalog catalog = CommissionCatalogConfigLoader.loadOrCreate(path);
    validateCatalog(catalog, path);
    com.mo.economy_system.EconomySystem.LOGGER.info("Loaded personal commission catalog {} ({} templates)",
        path, catalog.templates().size());
    return catalog;
  }

  /**
   * Resolves target-pool IDs against the NeoForge registries before a catalog becomes active.
   *
   * <p>Common catalog parsing deliberately stays loader-neutral, so this target-side boundary
   * checks Minecraft item/entity availability. Invalid administrator data must fail startup or
   * reload instead of silently producing an unusable commission.
   */
  private static void validateCatalog(CommissionCatalog catalog, java.nio.file.Path path) {
    for (CommissionTemplate template : catalog.templates()) {
      if (template.type() != CommissionType.ITEM_DELIVERY
          && template.type() != CommissionType.ENTITY_KILL) {
        continue;
      }
      CommissionTargetPool pool = catalog.targetPool(template.targetPool()).orElseThrow(() ->
          catalogError(path, template, template.targetPool(), "<missing>",
              "target pool is missing"));
      for (CommissionTargetPool.Target target : pool.targets()) {
        net.minecraft.resources.ResourceLocation id =
            net.minecraft.resources.ResourceLocation.tryParse(target.id());
        if (id == null) {
          throw catalogError(path, template, pool.id(), target.id(),
              "target ID is not a valid resource location");
        }
        if (template.type() == CommissionType.ITEM_DELIVERY) {
          if (!BuiltInRegistries.ITEM.containsKey(id)) {
            throw catalogError(path, template, pool.id(), target.id(),
                "item is not registered");
          }
          Item item = BuiltInRegistries.ITEM.get(id);
          if (item == null || item == Items.AIR) {
            throw catalogError(path, template, pool.id(), target.id(),
                "item resolves to AIR or is unavailable");
          }
        } else if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)
            || BuiltInRegistries.ENTITY_TYPE.get(id) == null) {
          throw catalogError(path, template, pool.id(), target.id(),
              "entity type is not registered");
        }
      }
    }
  }

  private static IllegalStateException catalogError(java.nio.file.Path path,
      CommissionTemplate template, String poolId, String targetId, String reason) {
    return new IllegalStateException("Invalid NeoForge 1.21.1 commission catalog at " + path
        + " [target=NeoForge 1.21.1, template=" + template.id() + ", pool=" + poolId
        + ", targetId=" + targetId + ", type=" + template.type() + "]: " + reason);
  }

  private static final class Delivery implements CommissionRewardDeliveryPort {
    private final NeoForge1211CommissionSavedData data; private final MinecraftServer server;
    Delivery(NeoForge1211CommissionSavedData d, MinecraftServer s) { data=d; server=s; }
    public DeliveryResult deliver(CommissionRewardRecord r) {
      if (r == null) return DeliveryResult.STATE_UNKNOWN;
      var mailbox = MailboxSavedData.getInstance(server.overworld());
      UUID mailId = UUID.nameUUIDFromBytes(("economysystem:commission-mail:" + r.rewardRecordId())
          .getBytes(StandardCharsets.UTF_8));
      MailRecord existing = mailbox.ledger().findPersonal(r.playerId(), mailId);
      if (existing != null) {
        if (!r.rewardRecordId().equals(existing.rewardRecordId())
            || existing.currencyRewardAmount() != r.currencyRewardAmount()) {
          return DeliveryResult.STATE_UNKNOWN;
        }
        if (r.status() != CommissionRewardStatus.MAIL_CREATED
            && r.status() != CommissionRewardStatus.CLAIMED) {
          data.saveReward(r.mailCreated(mailId));
        }
        return DeliveryResult.ALREADY_DELIVERED;
      }
      if (r.status() == CommissionRewardStatus.CLAIMED
          || (r.status() == CommissionRewardStatus.MAIL_CREATED
              && !mailId.equals(r.mailId()))) return DeliveryResult.STATE_UNKNOWN;
      if (!MailboxCapacityPolicy.canAddPersonal(
          MailType.SYSTEM, mailbox.ledger().listPersonal(r.playerId()).size())) {
        return DeliveryResult.RETRYABLE_FAILURE;
      }
      try {
        mailbox.ledger().addPersonal(r.playerId(), new MailRecord(mailId, MailType.SYSTEM, null,
            "系统", "委托奖励结算", r.rewardSnapshot().description(), "commission.reward",
            Math.max(System.currentTimeMillis(), Math.max(1L, r.createdAt())), 0, List.of(),
            r.rewardRecordId(), r.currencyRewardAmount(), false, false, true), mailbox::markDirty);
        data.saveReward(r.mailCreated(mailId));
        return DeliveryResult.CREATED;
      } catch (IllegalStateException full) {
        return DeliveryResult.RETRYABLE_FAILURE;
      } catch (RuntimeException failure) {
        return DeliveryResult.STATE_UNKNOWN;
      }
    }
    public ClaimResult claim(UUID rewardId, UUID playerId, long now) { CommissionRewardRecord r=data.findReward(rewardId).orElse(null); if(r==null)return ClaimResult.NOT_FOUND; if(!r.playerId().equals(playerId))return ClaimResult.WRONG_PLAYER; return r.status()==CommissionRewardStatus.CLAIMED?ClaimResult.ALREADY_CLAIMED:ClaimResult.STATE_UNKNOWN; }
  }
}
