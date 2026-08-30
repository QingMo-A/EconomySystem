package com.mo.economy_system.target.neoforge1211.api;

import com.mo.economy_system.api.EconomyApiCapabilities;
import com.mo.economy_system.api.EconomyApiSession;
import com.mo.economy_system.api.account.EconomyAccountApi;
import com.mo.economy_system.api.mailbox.EconomyMailboxApi;
import com.mo.economy_system.api.market.EconomyMarketApi;
import com.mo.economy_system.api.territory.EconomyTerritoryApi;
import com.mo.economy_system.common.api.EconomyApiMappings;
import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.mail.MailRecord;
import com.mo.economy_system.common.mail.MailType;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.BalanceLogEntry;
import com.mo.economy_system.core.economy_system.EconomyLedger;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.delivery_box.DeliveryBoxSavedData;
import com.mo.economy_system.core.economy_system.mailbox.MailboxSavedData;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.core.territory_system.PlayerInfo;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.target.neoforge1211.NeoForge1211Platform;
import com.mo.economy_system.target.neoforge1211.protocol.NeoForge1211MailboxHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** NeoForge 1.21.1 implementation behind the stable public EconomySystem API. */
public final class NeoForge1211EconomyApiSession implements EconomyApiSession {
  private final ServerLevel level;
  private final EconomyAccountApi accounts = new Accounts();
  private final EconomyMailboxApi mailbox = new Mailbox();
  private final EconomyMarketApi market = new Market();
  private final EconomyTerritoryApi territories = new Territories();

  public NeoForge1211EconomyApiSession(ServerLevel level) {
    this.level = Objects.requireNonNull(level, "level");
  }

  @Override public EconomyApiCapabilities capabilities() { return EconomyApiCapabilities.V1; }
  @Override public EconomyAccountApi accounts() { return accounts; }
  @Override public EconomyMailboxApi mailbox() { return mailbox; }
  @Override public EconomyMarketApi market() { return market; }
  @Override public EconomyTerritoryApi territories() { return territories; }

  private void requireServerThread() {
    if (!level.getServer().isSameThread()) {
      throw new IllegalStateException("EconomySystem API must be called from the Minecraft server thread");
    }
  }

  private final class Accounts implements EconomyAccountApi {
    private EconomySavedData data() { return EconomySavedData.getInstance(level); }

    @Override public int maxBalance() { return EconomyLedger.MAX_BALANCE; }

    @Override public int balance(UUID playerId) {
      requireServerThread();
      return data().getBalance(Objects.requireNonNull(playerId, "playerId"));
    }

    @Override public boolean hasAtLeast(UUID playerId, int amount) {
      requireServerThread();
      return amount > 0 && data().getBalance(Objects.requireNonNull(playerId, "playerId")) >= amount;
    }

    @Override public MutationStatus previewCredit(UUID playerId, int amount) {
      requireServerThread();
      return EconomyApiMappings.mutation(data().previewCreditExact(
          Objects.requireNonNull(playerId, "playerId"), amount));
    }

    @Override public MutationStatus credit(UUID playerId, int amount, TransactionNote note) {
      requireServerThread();
      Objects.requireNonNull(note, "note");
      return EconomyApiMappings.mutation(data().creditExact(
          Objects.requireNonNull(playerId, "playerId"), amount, note.source(), note.reason()));
    }

    @Override public MutationStatus debit(UUID playerId, int amount, TransactionNote note) {
      requireServerThread();
      Objects.requireNonNull(note, "note");
      return EconomyApiMappings.mutation(data().debitExact(
          Objects.requireNonNull(playerId, "playerId"), amount, note.source(), note.reason()));
    }

    @Override public TransferStatus previewTransfer(UUID senderId, UUID recipientId, int amount) {
      requireServerThread();
      return EconomyApiMappings.transfer(data().previewTransferExact(
          Objects.requireNonNull(senderId, "senderId"),
          Objects.requireNonNull(recipientId, "recipientId"), amount));
    }

    @Override public TransferStatus transfer(
        UUID senderId, UUID recipientId, int amount,
        TransactionNote senderNote, TransactionNote recipientNote) {
      requireServerThread();
      Objects.requireNonNull(senderNote, "senderNote");
      Objects.requireNonNull(recipientNote, "recipientNote");
      if (!senderNote.source().equals(recipientNote.source())) {
        throw new IllegalArgumentException("one transfer must use the same source for both account logs");
      }
      return EconomyApiMappings.transfer(data().transferExact(
          Objects.requireNonNull(senderId, "senderId"),
          Objects.requireNonNull(recipientId, "recipientId"), amount,
          senderNote.source(), senderNote.reason(), recipientNote.reason()));
    }

    @Override public LogPage history(UUID playerId, String sourceFilter, int offset, int limit) {
      requireServerThread();
      Objects.requireNonNull(playerId, "playerId");
      String filter = sourceFilter == null ? "" : sourceFilter.trim();
      int safeOffset = Math.max(0, offset);
      int safeLimit = Math.max(1, Math.min(100, limit));
      List<BalanceLogEntry> filtered = data().getBalanceLogs(playerId).stream()
          .filter(entry -> filter.isEmpty() || filter.equals(entry.category()))
          .toList();
      int total = filtered.size();
      int from = Math.min(safeOffset, total);
      int to = Math.min(total, from + safeLimit);
      List<LogEntry> entries = filtered.subList(from, to).stream()
          .map(entry -> new LogEntry(entry.timeMillis(), entry.category(), entry.reason(),
              entry.delta(), entry.beforeBalance(), entry.afterBalance()))
          .toList();
      return new LogPage(entries, filter, safeOffset, safeLimit, total);
    }
  }

  private final class Mailbox implements EconomyMailboxApi {
    @Override public DeliveryStatus sendNotice(UUID recipientId, MailDraft draft) {
      requireServerThread();
      Objects.requireNonNull(recipientId, "recipientId");
      Objects.requireNonNull(draft, "draft");
      MailboxSavedData data = MailboxSavedData.getInstance(level);
      if (data.ledger().listPersonal(recipientId).size() >= EconomyNetworkLimits.MAX_MAILS_PER_PLAYER) {
        return DeliveryStatus.MAILBOX_FULL;
      }
      int moneyAmount = draft.moneyAmount();
      EconomySavedData economy = moneyAmount > 0 ? EconomySavedData.getInstance(level) : null;
      if (economy != null && economy.previewCreditExact(recipientId, moneyAmount)
          != BalanceMutationResult.SUCCESS) {
        return DeliveryStatus.BALANCE_LIMIT;
      }
      boolean moneyCredited = false;
      try {
        if (economy != null) {
          BalanceMutationResult credited = economy.creditExact(
              recipientId, moneyAmount, "邮件", "系统邮件发放");
          if (credited != BalanceMutationResult.SUCCESS) {
            return credited == BalanceMutationResult.BALANCE_LIMIT
                ? DeliveryStatus.BALANCE_LIMIT : DeliveryStatus.PERSIST_FAILED;
          }
          moneyCredited = true;
        }
        long now = System.currentTimeMillis();
        data.ledger().addPersonal(recipientId,
            new MailRecord(UUID.randomUUID(), MailType.SYSTEM, null, "", draft.subject(), draft.body(),
                draft.source(), now, 0, List.of(), moneyAmount, false, true), data::markDirty);
      } catch (IllegalArgumentException failure) {
        if (moneyCredited && economy != null) rollbackCredit(economy, recipientId, moneyAmount);
        return DeliveryStatus.INVALID_INPUT;
      } catch (IllegalStateException failure) {
        if (moneyCredited && economy != null) rollbackCredit(economy, recipientId, moneyAmount);
        return DeliveryStatus.MAILBOX_FULL;
      } catch (RuntimeException failure) {
        if (moneyCredited && economy != null) rollbackCredit(economy, recipientId, moneyAmount);
        return DeliveryStatus.PERSIST_FAILED;
      }
      NeoForge1211MailboxHandlers.notifyNewMail(
          level.getServer().getPlayerList().getPlayer(recipientId), MailType.SYSTEM, "", draft.subject());
      return DeliveryStatus.SUCCESS;
    }

    @Override public DeliveryStatus sendCompensation(
        UUID recipientId, MailDraft draft, List<MailItemGrant> items) {
      requireServerThread();
      Objects.requireNonNull(recipientId, "recipientId");
      Objects.requireNonNull(draft, "draft");
      if (items == null || items.isEmpty()) return DeliveryStatus.INVALID_INPUT;

      Materialization materialized = materialize(items);
      if (materialized.error() != null) return materialized.error();
      List<ItemStack> stacks = materialized.stacks();

      List<DeliveryBoxEntrySnapshot> attachments = new ArrayList<>(stacks.size());
      try {
        for (ItemStack stack : stacks) {
          var snapshot = NeoForge1211Platform.nativeItemStacks()
              .captureSnapshot(stack.copy(), level.registryAccess());
          if (!snapshot.isSuccess()) return DeliveryStatus.STATE_UNKNOWN;
          attachments.add(new DeliveryBoxEntrySnapshot(UUID.randomUUID(), snapshot.orElseThrow(), draft.source()));
        }
      } catch (RuntimeException failure) {
        return DeliveryStatus.STATE_UNKNOWN;
      }

      DeliveryBoxSavedData delivery = DeliveryBoxSavedData.getInstance(level);
      MailboxSavedData mailboxData = MailboxSavedData.getInstance(level);
      if (mailboxData.ledger().listPersonal(recipientId).size() >= EconomyNetworkLimits.MAX_MAILS_PER_PLAYER) {
        return DeliveryStatus.MAILBOX_FULL;
      }
      if (delivery.ledger().list(recipientId).size() + attachments.size()
          > EconomyNetworkLimits.MAX_DELIVERY_BOX_ENTRIES) {
        return DeliveryStatus.ATTACHMENT_STORAGE_FULL;
      }
      int moneyAmount = draft.moneyAmount();
      EconomySavedData economy = moneyAmount > 0 ? EconomySavedData.getInstance(level) : null;
      if (economy != null && economy.previewCreditExact(recipientId, moneyAmount)
          != BalanceMutationResult.SUCCESS) {
        return DeliveryStatus.BALANCE_LIMIT;
      }

      boolean deliveryAdded = false;
      boolean moneyCredited = false;
      List<UUID> attachmentIds = attachments.stream().map(DeliveryBoxEntrySnapshot::entryId).toList();
      try {
        delivery.ledger().addAll(recipientId, attachments, delivery::markDirty);
        deliveryAdded = true;
        if (economy != null) {
          BalanceMutationResult credited = economy.creditExact(
              recipientId, moneyAmount, "邮件", "补偿邮件发放");
          if (credited != BalanceMutationResult.SUCCESS) {
            if (!rollbackDelivery(delivery, recipientId, attachmentIds)) {
              return DeliveryStatus.STATE_UNKNOWN;
            }
            return credited == BalanceMutationResult.BALANCE_LIMIT
                ? DeliveryStatus.BALANCE_LIMIT : DeliveryStatus.PERSIST_FAILED;
          }
          moneyCredited = true;
        }
        long now = System.currentTimeMillis();
        mailboxData.ledger().addPersonal(recipientId,
            new MailRecord(UUID.randomUUID(), MailType.COMPENSATION, null, "", draft.subject(), draft.body(),
                draft.source(), now, 0, attachmentIds, moneyAmount, false, true), mailboxData::markDirty);
      } catch (IllegalStateException failure) {
        if (deliveryAdded && !rollbackDelivery(delivery, recipientId, attachmentIds)) {
          return DeliveryStatus.STATE_UNKNOWN;
        }
        if (moneyCredited && economy != null && !rollbackCredit(economy, recipientId, moneyAmount)) {
          return DeliveryStatus.STATE_UNKNOWN;
        }
        return DeliveryStatus.MAILBOX_FULL;
      } catch (RuntimeException failure) {
        if (deliveryAdded && !rollbackDelivery(delivery, recipientId, attachmentIds)) {
          return DeliveryStatus.STATE_UNKNOWN;
        }
        if (moneyCredited && economy != null && !rollbackCredit(economy, recipientId, moneyAmount)) {
          return DeliveryStatus.STATE_UNKNOWN;
        }
        return DeliveryStatus.PERSIST_FAILED;
      }

      NeoForge1211MailboxHandlers.notifyNewMail(
          level.getServer().getPlayerList().getPlayer(recipientId), MailType.COMPENSATION, "", draft.subject());
      return DeliveryStatus.SUCCESS;
    }

    @Override public DeliveryStatus publishAnnouncement(MailDraft draft, long expiresAtEpochMillis) {
      requireServerThread();
      Objects.requireNonNull(draft, "draft");
      if (expiresAtEpochMillis < 0) return DeliveryStatus.INVALID_INPUT;
      if (draft.moneyAmount() > 0) return DeliveryStatus.INVALID_INPUT;
      MailboxSavedData data = MailboxSavedData.getInstance(level);
      if (data.ledger().snapshot().announcements().size() >= EconomyNetworkLimits.MAX_MAIL_ANNOUNCEMENTS) {
        return DeliveryStatus.MAILBOX_FULL;
      }
      try {
        long now = System.currentTimeMillis();
        data.ledger().addAnnouncement(
            new MailRecord(UUID.randomUUID(), MailType.ANNOUNCEMENT, null, "", draft.subject(), draft.body(),
                draft.source(), now, expiresAtEpochMillis, List.of(), false, true), data::markDirty);
      } catch (IllegalArgumentException failure) {
        return DeliveryStatus.INVALID_INPUT;
      } catch (IllegalStateException failure) {
        return DeliveryStatus.MAILBOX_FULL;
      } catch (RuntimeException failure) {
        return DeliveryStatus.PERSIST_FAILED;
      }
      for (var player : level.getServer().getPlayerList().getPlayers()) {
        NeoForge1211MailboxHandlers.notifyNewMail(player, MailType.ANNOUNCEMENT, "", draft.subject());
      }
      return DeliveryStatus.SUCCESS;
    }

    private Materialization materialize(List<MailItemGrant> grants) {
      List<ItemStack> result = new ArrayList<>();
      for (MailItemGrant grant : grants) {
        if (grant == null) return new Materialization(List.of(), DeliveryStatus.INVALID_INPUT);
        ResourceLocation id = ResourceLocation.tryParse(grant.itemId());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
          return new Materialization(List.of(), DeliveryStatus.UNKNOWN_ITEM);
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null || item == Items.AIR) {
          return new Materialization(List.of(), DeliveryStatus.UNKNOWN_ITEM);
        }
        int max = Math.max(1, item.getDefaultInstance().getMaxStackSize());
        int remaining = grant.count();
        while (remaining > 0) {
          if (result.size() >= EconomyNetworkLimits.MAX_MAIL_ATTACHMENTS) {
            return new Materialization(List.of(), DeliveryStatus.TOO_MANY_ATTACHMENTS);
          }
          int count = Math.min(max, remaining);
          result.add(new ItemStack(item, count));
          remaining -= count;
        }
      }
      return new Materialization(List.copyOf(result), null);
    }

    private record Materialization(List<ItemStack> stacks, DeliveryStatus error) {}

    private static boolean rollbackCredit(
        EconomySavedData economy, UUID recipientId, int amount) {
      try {
        return economy.debitExact(recipientId, amount, "邮件回滚", "邮件发送失败退款")
            == BalanceMutationResult.SUCCESS;
      } catch (RuntimeException failure) {
        return false;
      }
    }

    private static boolean rollbackDelivery(
        DeliveryBoxSavedData delivery, UUID recipientId, List<UUID> attachmentIds) {
      try {
        return delivery.ledger().removeUnclaimedBatch(
            recipientId, Set.copyOf(attachmentIds), delivery::markDirty);
      } catch (RuntimeException failure) {
        return false;
      }
    }
  }

  private final class Market implements EconomyMarketApi {
    private List<OrderView> snapshot() {
      requireServerThread();
      return MarketSavedData.getInstance(level).getOrders().stream()
          .map(EconomyApiMappings::marketOrder)
          .toList();
    }

    @Override public List<OrderView> orders() { return snapshot(); }

    @Override public Optional<OrderView> order(UUID tradeId) {
      Objects.requireNonNull(tradeId, "tradeId");
      return snapshot().stream().filter(order -> order.tradeId().equals(tradeId)).findFirst();
    }

    @Override public List<OrderView> ordersByOwner(UUID ownerId) {
      Objects.requireNonNull(ownerId, "ownerId");
      return snapshot().stream().filter(order -> order.ownerId().equals(ownerId)).toList();
    }

    @Override public List<OrderView> ordersByType(OrderType type) {
      Objects.requireNonNull(type, "type");
      return snapshot().stream().filter(order -> order.type() == type).toList();
    }
  }

  private final class Territories implements EconomyTerritoryApi {
    private void ensureInitialized() {
      requireServerThread();
      ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
      if (overworld == null) throw new IllegalStateException("Overworld is not loaded");
      TerritoryManager.initialize(overworld);
    }

    @Override public List<TerritoryView> territories() {
      ensureInitialized();
      return TerritoryManager.getAllTerritories().stream().map(Territories::view).toList();
    }

    @Override public Optional<TerritoryView> territory(UUID territoryId) {
      ensureInitialized();
      Territory territory = TerritoryManager.getTerritoryByID(Objects.requireNonNull(territoryId, "territoryId"));
      return Optional.ofNullable(territory).map(Territories::view);
    }

    @Override public List<TerritoryView> territoriesByOwner(UUID ownerId) {
      ensureInitialized();
      return TerritoryManager.getTerritoriesByOwner(Objects.requireNonNull(ownerId, "ownerId")).stream()
          .map(Territories::view).toList();
    }

    @Override public Optional<TerritoryView> territoryAt(int x, int y, int z) {
      ensureInitialized();
      return TerritoryManager.getAllTerritories().stream()
          .filter(territory -> territory.getDimension().equals(level.dimension()))
          .filter(territory -> territory.isWithinBounds(x, y, z))
          .findFirst().map(Territories::view);
    }

    @Override public Relationship relationship(UUID territoryId, UUID playerId) {
      ensureInitialized();
      Territory territory = TerritoryManager.getTerritoryByID(Objects.requireNonNull(territoryId, "territoryId"));
      Objects.requireNonNull(playerId, "playerId");
      if (territory == null) return Relationship.NONE;
      if (territory.isOwner(playerId)) return Relationship.OWNER;
      return territory.hasPermission(playerId) ? Relationship.MEMBER : Relationship.NONE;
    }

    private static TerritoryView view(Territory territory) {
      var pos1 = territory.getPos1();
      var pos2 = territory.getPos2();
      return new TerritoryView(
          territory.getTerritoryID(), territory.getOwnerUUID(), territory.getOwnerName(), territory.getName(),
          new Position(pos1.getX(), pos1.getY(), pos1.getZ()),
          new Position(pos2.getX(), pos2.getY(), pos2.getZ()),
          territory.getDimension().location().toString(),
          territory.getAuthorizedPlayers().stream().map(PlayerInfo::getUuid).toList());
    }
  }
}
