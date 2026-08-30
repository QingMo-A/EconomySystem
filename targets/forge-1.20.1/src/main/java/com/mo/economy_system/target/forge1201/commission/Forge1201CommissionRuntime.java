package com.mo.economy_system.target.forge1201.commission;

import com.mo.economy_system.common.commission.CommissionCatalog;
import com.mo.economy_system.common.commission.CommissionCatalogConfigLoader;
import com.mo.economy_system.common.commission.CommissionCatalogDefaults;
import com.mo.economy_system.common.commission.CommissionEventIds;
import com.mo.economy_system.common.commission.CommissionGenerator;
import com.mo.economy_system.common.commission.CommissionInstance;
import com.mo.economy_system.common.commission.CommissionRandom;
import com.mo.economy_system.common.commission.CommissionService;
import com.mo.economy_system.common.commission.CommissionStatus;
import com.mo.economy_system.common.commission.CommissionTargetPool;
import com.mo.economy_system.common.commission.CommissionTemplate;
import com.mo.economy_system.common.commission.CommissionType;
import com.mo.economy_system.common.commission.PublicCommission;
import com.mo.economy_system.common.commission.PublicCommissionService;
import com.mo.economy_system.common.commission.PublicCommissionStatus;
import com.mo.economy_system.target.forge1201.Forge1201Platform;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.nio.file.Path;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Forge 1.20.1 server-authoritative personal commission adapter. */
public final class Forge1201CommissionRuntime {
  private static final Logger LOGGER = LoggerFactory.getLogger(Forge1201CommissionRuntime.class);
  private static final CommissionCatalog BUILTIN_CATALOG = CommissionCatalogDefaults.create();
  private static volatile CommissionCatalog activeCatalog = BUILTIN_CATALOG;
  /** Keep the packet idempotency cache alive for the whole server session. */
  private static final Map<MinecraftServer, CommissionService> SERVICES = new WeakHashMap<>();
  private static final Map<MinecraftServer, PublicCommissionService> PUBLIC_SERVICES =
      new WeakHashMap<>();

  private Forge1201CommissionRuntime() {}

  /** Ensures the SavedData is registered in the overworld during server startup. */
  public static void initialize(ServerLevel level) {
    activeCatalog = loadCatalog(level.getServer());
    synchronized (Forge1201CommissionRuntime.class) {
      SERVICES.remove(level.getServer());
      PUBLIC_SERVICES.remove(level.getServer());
    }
    Forge1201CommissionSavedData.get(level);
  }

  public static void shutdown(MinecraftServer server) {
    synchronized (Forge1201CommissionRuntime.class) {
      SERVICES.remove(server);
      PUBLIC_SERVICES.remove(server);
    }
    activeCatalog = BUILTIN_CATALOG;
  }

  /** Refreshes overdue work for a player.  The clock is server wall time, not client time. */
  public static CommissionService.RefreshView refresh(ServerPlayer player) {
    Forge1201CommissionSavedData data = data(player.serverLevel());
    return service(player.serverLevel(), data).refresh(player.getUUID(), now());
  }

  /** Forces an administrator refresh while retaining all existing personal commissions. */
  public static CommissionService.RefreshView forceRefresh(ServerPlayer player) {
    Forge1201CommissionSavedData data = data(player.serverLevel());
    return service(player.serverLevel(), data).forceRefresh(player.getUUID(), now());
  }

  public static List<String> templateIds() {
    return activeCatalog.templates().stream().map(value -> value.id()).toList();
  }

  public static int maxActivePersonalCommissions() {
    return activeCatalog.settings().maxActivePersonalCommissions();
  }

  public static int reloadCommand(net.minecraft.commands.CommandSourceStack source) {
    try {
      activeCatalog = loadCatalog(source.getServer());
      synchronized (Forge1201CommissionRuntime.class) {
        // Rebuild generators against the new catalog, while persisted instances remain frozen.
        SERVICES.remove(source.getServer());
        PUBLIC_SERVICES.remove(source.getServer());
      }
      source.sendSuccess(() -> Component.literal("个人委托库已重载。"), true);
      return 1;
    } catch (RuntimeException failure) {
      LOGGER.error("Unable to reload personal commission catalog", failure);
      source.sendFailure(Component.literal("个人委托库重载失败: " + safeMessage(failure)));
      return 0;
    }
  }

  /** Called from the login hook and periodically from the server tick hook. */
  public static void refreshOnlinePlayers(MinecraftServer server) {
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      try {
        refresh(player);
        retryPendingRewards(player);
      } catch (RuntimeException failure) {
        LOGGER.error("Personal commission refresh failed player={}", player.getUUID(), failure);
      }
    }
  }

  /** Retries personal and public commission mail delivery without requiring another UI packet. */
  public static void retryPendingRewards(ServerPlayer player) {
    Forge1201CommissionSavedData data = data(player.serverLevel());
    service(player.serverLevel(), data).retryPendingRewards(player.getUUID());
    publicService(player.serverLevel(), data).retryPendingRewards(player.getUUID());
  }

  public static void onLogin(ServerPlayer player) {
    try {
      CommissionService.RefreshView view = refresh(player);
      retryPendingRewards(player);
      if (view.generation().generated()) {
        player.sendSystemMessage(Component.literal(
            "个人委托已刷新，新增 " + view.generation().added().size() + " 条委托。"));
      }
    } catch (RuntimeException failure) {
      LOGGER.error("Personal commission login refresh failed player={}", player.getUUID(), failure);
      player.sendSystemMessage(Component.literal("个人委托刷新失败，请稍后重试。"));
    }
  }

  /**
   * Handles a player command and prints a compact state summary.  Listing also performs a due
   * refresh so a player never sees a stale schedule after a long offline period.
   */
  public static int listCommand(net.minecraft.commands.CommandSourceStack source) {
    ServerPlayer player;
    try {
      player = source.getPlayerOrException();
    } catch (Exception failure) {
      source.sendFailure(Component.literal("个人委托命令只能由玩家执行。"));
      return 0;
    }
    try {
      CommissionService.RefreshView view = refresh(player);
      var state = view.state();
      source.sendSuccess(() -> Component.literal("个人委托（" + state.commissions().size() + " 条）"), false);
      if (state.schedule() != null) {
        long remaining = Math.max(0L, state.schedule().nextRefreshAt() - now());
        source.sendSuccess(() -> Component.literal("下次刷新: " + formatDuration(remaining)), false);
      }
      if (state.commissions().isEmpty()) {
        source.sendSuccess(() -> Component.literal("当前没有可用个人委托。"), false);
        return 1;
      }
      for (CommissionInstance commission : state.commissions()) {
        source.sendSuccess(() -> Component.literal(formatCommission(commission)), false);
      }
      return 1;
    } catch (RuntimeException failure) {
      source.sendFailure(Component.literal("读取个人委托失败: " + safeMessage(failure)));
      return 0;
    }
  }

  public static int refreshCommand(net.minecraft.commands.CommandSourceStack source) {
    ServerPlayer player;
    try {
      player = source.getPlayerOrException();
    } catch (Exception failure) {
      source.sendFailure(Component.literal("个人委托命令只能由玩家执行。"));
      return 0;
    }
    try {
      CommissionService.RefreshView view = refresh(player);
      String result = view.generation().generated()
          ? "已刷新个人委托，新增 " + view.generation().added().size() + " 条。"
          : switch (view.generation().outcome()) {
            case NOT_DUE -> "个人委托尚未到刷新时间。";
            case AT_CAPACITY -> "个人委托已达到同时存在上限。";
            case NO_LEGAL_TEMPLATES -> "当前没有合法的个人委托模板。";
            case FAILED -> "个人委托刷新失败: " + String.join("; ", view.generation().issues());
            case GENERATED -> "个人委托刷新完成。";
          };
      source.sendSuccess(() -> Component.literal(result), false);
      return view.generation().outcome() == CommissionGenerator.RefreshOutcome.FAILED ? 0 : 1;
    } catch (RuntimeException failure) {
      source.sendFailure(Component.literal("刷新个人委托失败: " + safeMessage(failure)));
      return 0;
    }
  }

  /**
   * Validates and consumes only the accepted amount of an ITEM_DELIVERY commission before common
   * progress is persisted.  Item removal is server-side and is restored if orchestration throws.
   */
  public static SubmitFeedback submitItem(ServerPlayer player, UUID commissionId, int requestedAmount) {
    return submitItem(player, commissionId, UUID.randomUUID(), requestedAmount);
  }

  public static SubmitFeedback submitItem(ServerPlayer player, UUID commissionId,
                                          UUID submissionId, int requestedAmount) {
    if (requestedAmount <= 0) return SubmitFeedback.failure("提交数量必须大于 0。");
    Forge1201CommissionSavedData data = data(player.serverLevel());
    CommissionService service = service(player.serverLevel(), data);
    try {
      CommissionService.RefreshView refreshed = service.refresh(player.getUUID(), now());
      CommissionInstance commission = refreshed.state().commissions().stream()
          .filter(value -> value.commissionId().equals(commissionId)).findFirst().orElse(null);
      if (commission == null) return SubmitFeedback.failure("找不到该委托。");
      if (commission.type() != CommissionType.ITEM_DELIVERY) {
        return SubmitFeedback.failure("该委托不是物资提交委托。");
      }
      if (commission.status().terminal()) {
        return SubmitFeedback.failure(statusMessage(commission.status()));
      }
      int acceptedAmount = Math.min(requestedAmount,
          Math.max(0, commission.requiredAmount() - commission.progress()));
      if (acceptedAmount <= 0) return SubmitFeedback.failure("该委托已经达到完成数量。");
      ResourceLocation targetId = ResourceLocation.tryParse(commission.targetSnapshot());
      if (targetId == null || !BuiltInRegistries.ITEM.containsKey(targetId)) {
        return SubmitFeedback.failure("委托目标物品不可用: " + commission.targetSnapshot());
      }
      Item target = BuiltInRegistries.ITEM.get(targetId);
      Map<Integer, ItemStack> originals = matchingStacks(player, target, acceptedAmount);
      if (originals == null) {
        return SubmitFeedback.failure("背包中没有足够的 " + commission.targetSnapshot() + "。");
      }
      consume(player, originals, acceptedAmount);
      try {
        CommissionService.SubmitResult result = service.submitProgress(
            player.getUUID(), commissionId, submissionId, acceptedAmount, now());
        if (!result.accepted()) {
          restore(player, originals);
          return SubmitFeedback.failure(outcomeMessage(result));
        }
        player.containerMenu.broadcastChanges();
        return SubmitFeedback.success(outcomeMessage(result));
      } catch (RuntimeException failure) {
        restore(player, originals);
        throw failure;
      }
    } catch (RuntimeException failure) {
      LOGGER.error("Item commission submission failed player={} commission={}",
          player.getUUID(), commissionId, failure);
      return SubmitFeedback.failure("提交委托失败: " + safeMessage(failure));
    }
  }

  /** ENTITY_KILL progress is derived exclusively from the server death event. */
  @Deprecated
  public static void handleEntityKill(ServerPlayer player, String entityId) {
    // Compatibility callers only have the legacy entity type.  The actual Forge death hook uses
    // the overload below with the killed entity UUID, which provides a stable event identity.
    handleEntityKill(player, entityId, UUID.randomUUID());
  }

  /** ENTITY_KILL progress with a stable killed-entity event identity. */
  public static void handleEntityKill(ServerPlayer player, String entityId, UUID killedEntityId) {
    if (entityId == null || entityId.isBlank()) return;
    if (killedEntityId == null) return;
    Forge1201CommissionSavedData data = data(player.serverLevel());
    CommissionService service = service(player.serverLevel(), data);
    UUID submissionId = CommissionEventIds.entityKill(player.getUUID(), killedEntityId);
    try {
      CommissionService.RefreshView refreshed = service.refresh(player.getUUID(), now());
      List<UUID> matching = refreshed.state().commissions().stream()
          .filter(value -> value.type() == CommissionType.ENTITY_KILL)
          .filter(value -> value.targetSnapshot().equals(entityId))
          .filter(value -> !value.status().terminal())
          .map(CommissionInstance::commissionId)
          .toList();
      for (UUID commissionId : matching) {
        CommissionService.SubmitResult result = service.submitProgress(
            player.getUUID(), commissionId, submissionId, 1, now());
        if (result.outcome() == CommissionService.SubmitOutcome.COMPLETED
            || result.outcome() == CommissionService.SubmitOutcome.REWARD_PENDING_MAIL) {
          player.sendSystemMessage(Component.literal("委托完成，奖励已发送至邮箱。"));
        } else if (result.outcome() == CommissionService.SubmitOutcome.REWARD_DELIVERY_RETRY) {
          player.sendSystemMessage(Component.literal("委托已完成，但奖励邮件暂未投递成功，将自动重试。"));
        }
      }
    } catch (RuntimeException failure) {
      LOGGER.error("Entity-kill commission progress failed player={} target={}",
          player.getUUID(), entityId, failure);
    }
  }

  public static PublicCommission createPublic(MinecraftServer server, String name, String requesterId,
      String requesterName, String target, int targetAmount, int unitReward, long expiresAt,
      String description) {
    ServerLevel level = server.overworld();
    Forge1201CommissionSavedData data = data(level);
    PublicCommission commission = PublicCommission.create(UUID.randomUUID(), name, requesterId,
        requesterName, target, targetAmount, unitReward, now(), expiresAt, description);
    publicService(level, data).create(commission);
    return commission;
  }

  public static List<PublicCommission> listPublic(MinecraftServer server) {
    ServerLevel level = server.overworld();
    return publicService(level, data(level)).list(now());
  }

  public static java.util.Optional<PublicCommission> findPublic(MinecraftServer server, UUID id) {
    return listPublic(server).stream().filter(c -> c.commissionId().equals(id)).findFirst();
  }

  public static boolean cancelPublic(MinecraftServer server, UUID id) {
    ServerLevel level = server.overworld();
    return publicService(level, data(level)).cancel(id);
  }

  public static void removePublic(MinecraftServer server, UUID id) {
    data(server.overworld()).removePublic(id);
  }

  public static PublicCommissionService.SubmitResult submitPublicItem(ServerPlayer player,
      UUID id, int requestedAmount) {
    return submitPublicItem(player, id, UUID.randomUUID(), requestedAmount);
  }

  public static PublicCommissionService.SubmitResult submitPublicItem(ServerPlayer player,
      UUID id, UUID submissionId, int requestedAmount) {
    if (requestedAmount <= 0) throw new IllegalArgumentException("提交数量必须大于 0");
    java.util.Objects.requireNonNull(submissionId, "submissionId");
    ServerLevel level = player.serverLevel();
    Forge1201CommissionSavedData data = data(level);
    PublicCommissionService publicService = publicService(level, data);
    PublicCommission commission = publicService.list(now()).stream()
        .filter(c -> c.commissionId().equals(id)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("找不到公共委托"));
    if (commission.status() != PublicCommissionStatus.AVAILABLE) {
      return publicService.submit(player.getUUID(), id, submissionId, requestedAmount, now());
    }
    int amount = Math.min(requestedAmount, commission.remainingAmount());
    ResourceLocation targetId = ResourceLocation.tryParse(commission.targetSnapshot());
    if (targetId == null || !BuiltInRegistries.ITEM.containsKey(targetId)) {
      throw new IllegalArgumentException("目标物品不可用");
    }
    Item target = BuiltInRegistries.ITEM.get(targetId);
    Map<Integer, ItemStack> originals = matchingStacks(player, target, amount);
    if (originals == null) throw new IllegalArgumentException("背包中没有足够的目标物品");
    consume(player, originals, amount);
    try {
      PublicCommissionService.SubmitResult result = publicService.submit(
          player.getUUID(), id, submissionId, amount, now());
      if (result.acceptedAmount() != amount) {
        restore(player, originals);
        consume(player, originals, result.acceptedAmount());
      }
      player.containerMenu.broadcastChanges();
      return result;
    } catch (RuntimeException failure) {
      restore(player, originals);
      throw failure;
    }
  }

  /** Synchronizes the durable commission reward record after the mailbox ledger claims it. */
  public static void markRewardClaimed(ServerPlayer player, UUID rewardRecordId) {
    if (rewardRecordId == null) return;
    try {
      Forge1201CommissionSavedData data = data(player.serverLevel());
      data.find(rewardRecordId).ifPresent(record -> {
        if (record.status() != com.mo.economy_system.common.commission.CommissionRewardStatus.CLAIMED) {
          data.save(record.claimed(now()));
        }
      });
    } catch (RuntimeException failure) {
      // The mailbox marker is already the duplicate-claim guard. Keep the currency in the account
      // and let a later reconciliation retry this audit marker; never debit a successful claim.
      LOGGER.error("Commission reward claim marker reconciliation failed reward={}",
          rewardRecordId, failure);
    }
  }

  private static synchronized CommissionService service(ServerLevel level,
      Forge1201CommissionSavedData data) {
    MinecraftServer server = level.getServer();
    return SERVICES.computeIfAbsent(server, ignored -> new CommissionService(
        new CommissionGenerator(activeCatalog, random()), data, data,
        new Forge1201CommissionMailBridge(level, data)));
  }

  private static synchronized PublicCommissionService publicService(ServerLevel level,
      Forge1201CommissionSavedData data) {
    MinecraftServer server = level.getServer();
    return PUBLIC_SERVICES.computeIfAbsent(server, ignored -> {
      com.mo.economy_system.common.commission.PublicCommissionRepository publicRepository =
          new com.mo.economy_system.common.commission.PublicCommissionRepository() {
            @Override public java.util.Optional<PublicCommission> find(UUID id) { return data.findPublic(id); }
            @Override public java.util.List<PublicCommission> list() { return data.listPublic(); }
            @Override public void save(PublicCommission value) { data.savePublic(value); }
            @Override public void remove(UUID id) { data.removePublic(id); }
          };
      return new PublicCommissionService(publicRepository, data,
          new Forge1201CommissionMailBridge(level, data));
    });
  }

  private static Forge1201CommissionSavedData data(ServerLevel level) {
    return Forge1201CommissionSavedData.get(level);
  }

  private static CommissionCatalog loadCatalog(MinecraftServer server) {
    Path path = server.getServerDirectory().toPath()
        .resolve("config").resolve("economysystem").resolve("commissions").resolve("catalog.json");
    CommissionCatalog catalog = CommissionCatalogConfigLoader.loadOrCreate(path);
    validateCatalog(catalog, path);
    LOGGER.info("Loaded personal commission catalog {} ({} templates)", path, catalog.templates().size());
    return catalog;
  }

  /**
   * Resolves target-pool IDs against the Forge registries before a catalog becomes active.
   *
   * <p>Common catalog parsing deliberately stays loader-neutral, so this is the target-side
   * boundary where Minecraft item/entity availability is checked.  Invalid administrator data
   * must fail startup/reload rather than silently producing an unusable commission.
   */
  private static void validateCatalog(CommissionCatalog catalog, Path path) {
    for (CommissionTemplate template : catalog.templates()) {
      if (template.type() != CommissionType.ITEM_DELIVERY
          && template.type() != CommissionType.ENTITY_KILL) {
        continue;
      }
      CommissionTargetPool pool = catalog.targetPool(template.targetPool()).orElseThrow(() ->
          catalogError(path, template, template.targetPool(), "<missing>",
              "target pool is missing"));
      for (CommissionTargetPool.Target target : pool.targets()) {
        ResourceLocation id = ResourceLocation.tryParse(target.id());
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

  private static IllegalStateException catalogError(Path path, CommissionTemplate template,
      String poolId, String targetId, String reason) {
    return new IllegalStateException("Invalid Forge 1.20.1 commission catalog at " + path
        + " [target=Forge 1.20.1, template=" + template.id() + ", pool=" + poolId
        + ", targetId=" + targetId + ", type=" + template.type() + "]: " + reason);
  }

  private static CommissionRandom random() {
    Random random = ThreadLocalRandom.current();
    return new CommissionRandom() {
      @Override
      public double nextDouble() {
        return random.nextDouble();
      }

      @Override
      public int nextInt(int bound) {
        return random.nextInt(bound);
      }
    };
  }

  private static long now() {
    return Math.max(1L, System.currentTimeMillis());
  }

  private static Map<Integer, ItemStack> matchingStacks(
      ServerPlayer player, Item target, int required) {
    int available = 0;
    ItemStack template = new ItemStack(target);
    Map<Integer, ItemStack> originals = new LinkedHashMap<>();
    for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
      ItemStack stack = player.getInventory().items.get(slot);
      if (stack.isEmpty() || !Forge1201Platform.nativeItemStacks().sameItemAndData(stack, template)) continue;
      available += stack.getCount();
      originals.put(slot, stack.copy());
      if (available >= required) return originals;
    }
    return null;
  }

  private static void consume(ServerPlayer player, Map<Integer, ItemStack> originals, int amount) {
    int remaining = amount;
    for (Map.Entry<Integer, ItemStack> entry : originals.entrySet()) {
      if (remaining <= 0) break;
      ItemStack current = player.getInventory().items.get(entry.getKey());
      int removed = Math.min(remaining, current.getCount());
      current.shrink(removed);
      remaining -= removed;
    }
    if (remaining != 0) throw new IllegalStateException("inventory changed during commission submission");
  }

  private static void restore(ServerPlayer player, Map<Integer, ItemStack> originals) {
    originals.forEach((slot, stack) -> player.getInventory().items.set(slot, stack.copy()));
    try {
      player.containerMenu.broadcastChanges();
    } catch (RuntimeException ignored) {
      // The authoritative inventory was restored; a UI refresh is best effort.
    }
  }

  private static String formatCommission(CommissionInstance value) {
    String status = value.status().name();
    return value.commissionId() + " | " + value.type().id() + " | " + value.targetSnapshot()
        + " | " + value.progress() + "/" + value.requiredAmount()
        + " | reward=" + value.rewardSnapshot().amount() + " | " + status;
  }

  private static String formatDuration(long millis) {
    long seconds = millis / 1000L;
    long hours = seconds / 3600L;
    long minutes = (seconds % 3600L) / 60L;
    return hours + "h " + minutes + "m " + (seconds % 60L) + "s";
  }

  private static String statusMessage(CommissionStatus status) {
    return switch (status) {
      case COMPLETED -> "该委托已经完成，奖励请前往邮箱领取。";
      case EXPIRED -> "该委托已经过期。";
      case DISABLED -> "该委托已被禁用。";
      case AVAILABLE, ACTIVE, LOCKED -> "该委托当前不可提交。";
    };
  }

  private static String outcomeMessage(CommissionService.SubmitResult result) {
    return switch (result.outcome()) {
      case PROGRESSED -> "已提交 " + (result.commission().progress()) + "/"
          + result.commission().requiredAmount() + "。";
      case COMPLETED, REWARD_PENDING_MAIL -> "委托完成，奖励已发送至邮箱。";
      case REWARD_DELIVERY_RETRY -> "委托完成，但奖励邮件暂未投递成功，将自动重试。";
      case ALREADY_COMPLETED -> "该委托已经完成，奖励请前往邮箱领取。";
      case EXPIRED -> "该委托已经过期。";
      case DISABLED -> "该委托已被禁用。";
      case NOT_FOUND -> "找不到该委托。";
      case INVALID_AMOUNT -> "提交数量必须大于 0。";
      case DUPLICATE_SUBMISSION -> "该提交已处理。";
    };
  }

  private static String safeMessage(RuntimeException failure) {
    return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
  }

  public record SubmitFeedback(boolean accepted, String message) {
    public SubmitFeedback {
      message = message == null ? "" : message;
    }

    static SubmitFeedback success(String message) {
      return new SubmitFeedback(true, message);
    }

    static SubmitFeedback failure(String message) {
      return new SubmitFeedback(false, message);
    }
  }
}
