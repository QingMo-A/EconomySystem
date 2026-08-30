package com.mo.economy_system.target.neoforge1211.commission;

import com.mo.economy_system.common.commission.*;
import com.mo.economy_system.common.mail.MailRecord;
import com.mo.economy_system.common.mail.MailType;
import com.mo.economy_system.core.economy_system.mailbox.MailboxSavedData;
import com.mo.economy_system.target.neoforge1211.NeoForge1211Platform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.*;

/** NeoForge lifecycle bridge for the common personal commission service. */
public final class NeoForge1211CommissionRuntime {
  private static final Map<MinecraftServer, CommissionService> SERVICES = new WeakHashMap<>();
  private static final Map<MinecraftServer, PublicCommissionService> PUBLIC_SERVICES = new WeakHashMap<>();
  private NeoForge1211CommissionRuntime() {}

  public static synchronized CommissionService service(MinecraftServer server) {
    return SERVICES.computeIfAbsent(server, s -> {
      NeoForge1211CommissionSavedData data = NeoForge1211CommissionSavedData.getInstance(s.overworld());
      return new CommissionService(new CommissionGenerator(defaultCatalog(), new CommissionRandom() {
            private final java.util.Random random = new java.util.Random();
            public double nextDouble() { return random.nextDouble(); }
            public int nextInt(int bound) { return random.nextInt(bound); }
          }),
          new CommissionRepository() { public CommissionPlayerState load(UUID id) { return data.loadState(id); } public void save(CommissionPlayerState state) { data.saveState(state); } },
          new CommissionRewardRepository() { public Optional<CommissionRewardRecord> find(UUID id) { return data.findReward(id); } public Optional<CommissionRewardRecord> findByIdempotencyKey(String key) { return data.findRewardByKey(key); } public CommissionRewardRecord createIfAbsent(CommissionRewardRecord candidate) { return data.createReward(candidate); } public void save(CommissionRewardRecord r) { data.saveReward(r); } public List<CommissionRewardRecord> listForPlayer(UUID id) { return data.rewardsFor(id); } },
          new Delivery(data, s));
    });
  }

  public static CommissionService.RefreshView refresh(ServerPlayer player) { return service(player.server).refresh(player.getUUID(), System.currentTimeMillis()); }
  public static CommissionPlayerState state(ServerPlayer player) { return service(player.server).refresh(player.getUUID(), System.currentTimeMillis()).state(); }

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
    long now = Math.max(1L, System.currentTimeMillis());
    PublicCommission commission = PublicCommission.create(UUID.randomUUID(), name, requesterId,
        requesterName, target, targetAmount, unitReward, now, expiresAt, description);
    publicService(server).create(commission);
    return commission;
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
    if (requested <= 0) throw new IllegalArgumentException("提交数量必须大于 0");
    PublicCommissionService service = publicService(player.server);
    PublicCommission commission = findPublic(player.server, id).orElseThrow(() -> new IllegalArgumentException("找不到公共委托"));
    if (commission.status() != PublicCommissionStatus.AVAILABLE) {
      return service.submit(player.getUUID(), id, UUID.randomUUID(), requested, System.currentTimeMillis());
    }
    int amount = Math.min(requested, commission.remainingAmount());
    var itemId = net.minecraft.resources.ResourceLocation.parse(commission.targetSnapshot());
    var target = BuiltInRegistries.ITEM.get(itemId);
    if (target == null || target == net.minecraft.world.item.Items.AIR) throw new IllegalArgumentException("目标物品不可用");
    int available = 0;
    for (ItemStack stack : player.getInventory().items) if (stack.getItem() == target) available += stack.getCount();
    if (available < amount) throw new IllegalArgumentException("背包中没有足够的目标物品");
    List<ItemStack> originals = new ArrayList<>();
    for (ItemStack stack : player.getInventory().items) originals.add(stack.copy());
    int remaining = amount;
    for (ItemStack stack : player.getInventory().items) {
      if (remaining == 0) break;
      if (stack.getItem() != target) continue;
      int take = Math.min(remaining, stack.getCount()); stack.shrink(take); remaining -= take;
    }
    try {
      PublicCommissionService.SubmitResult result = service.submit(player.getUUID(), id, UUID.randomUUID(), amount, System.currentTimeMillis());
      if (result.acceptedAmount() == 0) {
        for (int i = 0; i < originals.size(); i++) player.getInventory().items.set(i, originals.get(i));
      }
      player.containerMenu.broadcastChanges();
      return result;
    } catch (RuntimeException failure) {
      for (int i = 0; i < originals.size(); i++) player.getInventory().items.set(i, originals.get(i));
      player.containerMenu.broadcastChanges();
      throw failure;
    }
  }

  public static CommissionService.SubmitResult submitItem(ServerPlayer player, UUID id, int amount) {
    CommissionPlayerState state = state(player);
    CommissionInstance commission = state.commissions().stream().filter(c -> c.commissionId().equals(id)).findFirst().orElse(null);
    if (commission == null || commission.type() != CommissionType.ITEM_DELIVERY) return service(player.server).submitProgress(player.getUUID(), id, amount, System.currentTimeMillis());
    int available = 0;
    for (ItemStack stack : player.getInventory().items) if (stack.getItem() == BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(commission.targetSnapshot()))) available += stack.getCount();
    if (available < amount) throw new IllegalStateException("背包中没有足够的目标物品");
    int remaining = amount;
    for (ItemStack stack : player.getInventory().items) { if (remaining == 0) break; if (stack.getItem() != BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(commission.targetSnapshot()))) continue; int take=Math.min(remaining, stack.getCount()); stack.shrink(take); remaining-=take; }
    CommissionService.SubmitResult result = service(player.server).submitProgress(player.getUUID(), id, amount, System.currentTimeMillis());
    if (!result.accepted() && result.outcome() != CommissionService.SubmitOutcome.REWARD_PENDING_MAIL) player.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(commission.targetSnapshot())), amount));
    return result;
  }

  public static void onKill(ServerPlayer player, LivingEntity entity) {
    String target = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    CommissionPlayerState state = state(player);
    for (CommissionInstance c : state.commissions()) {
      if (c.type() == CommissionType.ENTITY_KILL && c.targetSnapshot().equals(target)
          && c.status().countsAsActive()) {
        service(player.server).submitProgress(player.getUUID(), c.commissionId(), 1,
            System.currentTimeMillis());
      }
    }
  }

  public static void clear(MinecraftServer server) { SERVICES.remove(server); PUBLIC_SERVICES.remove(server); }

  private static CommissionRewardRepository rewardRepository(NeoForge1211CommissionSavedData data) {
    return new CommissionRewardRepository() {
      public Optional<CommissionRewardRecord> find(UUID id) { return data.findReward(id); }
      public Optional<CommissionRewardRecord> findByIdempotencyKey(String key) { return data.findRewardByKey(key); }
      public CommissionRewardRecord createIfAbsent(CommissionRewardRecord candidate) { return data.createReward(candidate); }
      public void save(CommissionRewardRecord reward) { data.saveReward(reward); }
      public List<CommissionRewardRecord> listForPlayer(UUID id) { return data.rewardsFor(id); }
    };
  }

  private static CommissionCatalog defaultCatalog() {
    CommissionRequester requester = new CommissionRequester("town", "城镇供应处");
    Map<String,List<CommissionRequester>> requesters = Map.of("default", List.of(requester));
    Map<String,CommissionTargetPool> targets = Map.of("items", CommissionTargetPool.unweighted("items", List.of("minecraft:cobblestone", "minecraft:kelp")), "mobs", CommissionTargetPool.unweighted("mobs", List.of("minecraft:zombie", "minecraft:skeleton")));
    long h=60L*60L*1000L;
    List<CommissionTemplate> templates = List.of(new CommissionTemplate("materials", CommissionType.ITEM_DELIVERY, "default", "items", 16, 64, 16, 2, 1, "material", h*2, h*4), new CommissionTemplate("hunt", CommissionType.ENTITY_KILL, "default", "mobs", 5, 15, 5, 20, 1, "combat", h*2, h*4));
    return new CommissionCatalog(templates, requesters, targets);
  }

  private static final class Delivery implements CommissionRewardDeliveryPort {
    private final NeoForge1211CommissionSavedData data; private final MinecraftServer server;
    Delivery(NeoForge1211CommissionSavedData d, MinecraftServer s) { data=d; server=s; }
    public DeliveryResult deliver(CommissionRewardRecord r) { var mailbox=MailboxSavedData.getInstance(server.overworld()); for (var m:mailbox.ledger().listPersonal(r.playerId())) if (r.rewardRecordId().equals(m.rewardRecordId())) return DeliveryResult.ALREADY_DELIVERED; UUID mail=UUID.randomUUID(); mailbox.ledger().addPersonal(r.playerId(), new MailRecord(mail, MailType.SYSTEM, null, "", "委托奖励", r.rewardSnapshot().description(), "commission.reward", r.createdAt(), 0, List.of(), r.rewardRecordId(), r.rewardSnapshot().amount(), false, false, true), mailbox::markDirty); data.saveReward(r.mailCreated(mail)); return DeliveryResult.CREATED; }
    public ClaimResult claim(UUID rewardId, UUID playerId, long now) { CommissionRewardRecord r=data.findReward(rewardId).orElse(null); if(r==null)return ClaimResult.NOT_FOUND; if(!r.playerId().equals(playerId))return ClaimResult.WRONG_PLAYER; return r.status()==CommissionRewardStatus.CLAIMED?ClaimResult.ALREADY_CLAIMED:ClaimResult.STATE_UNKNOWN; }
  }
}
