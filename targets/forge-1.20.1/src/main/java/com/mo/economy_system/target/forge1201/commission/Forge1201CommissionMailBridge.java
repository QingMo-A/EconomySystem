package com.mo.economy_system.target.forge1201.commission;

import com.mo.economy_system.common.commission.CommissionRewardDeliveryPort;
import com.mo.economy_system.common.commission.CommissionRewardRecord;
import com.mo.economy_system.common.commission.CommissionRewardStatus;
import com.mo.economy_system.common.mail.MailRecord;
import com.mo.economy_system.common.mail.MailType;
import com.mo.economy_system.common.mail.MailboxCapacityPolicy;
import com.mo.economy_system.common.mail.MailboxLedger;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge mailbox bridge for commission rewards.
 *
 * <p>The existing mailbox SavedData is intentionally target-network scoped and package-private.
 * This bridge keeps that visibility boundary intact while exposing only the operations needed by
 * the commission adapter.  Reflection is isolated here so no commission code can accidentally
 * use the legacy immediate {@code moneyAmount} field.  A future mailbox API can replace this
 * class without changing the common commission service.
 */
final class Forge1201CommissionMailBridge implements CommissionRewardDeliveryPort {
  private static final Logger LOGGER = LoggerFactory.getLogger(Forge1201CommissionMailBridge.class);
  private static final String MAILBOX_DATA_CLASS =
      "com.mo.economy_system.target.forge1201.network.Forge1201MailboxSavedData";
  private static final String MAILBOX_HANDLER_CLASS =
      "com.mo.economy_system.target.forge1201.network.Forge1201MailboxHandlers";

  private final ServerLevel level;
  private final Forge1201CommissionSavedData commissions;

  Forge1201CommissionMailBridge(ServerLevel level, Forge1201CommissionSavedData commissions) {
    this.level = level;
    this.commissions = commissions;
  }

  @Override
  public DeliveryResult deliver(CommissionRewardRecord record) {
    if (record == null) return DeliveryResult.STATE_UNKNOWN;
    UUID mailId = deterministicMailId(record.rewardRecordId());
    try {
      Object mailbox = mailboxSavedData(level);
      MailboxLedger ledger = mailboxLedger(mailbox);
      MailRecord existing = ledger.findPersonal(record.playerId(), mailId);
      if (existing != null) {
        if (!matches(existing, record)) {
          LOGGER.error("Commission reward mail identity collision reward={} mail={}",
              record.rewardRecordId(), mailId);
          return DeliveryResult.STATE_UNKNOWN;
        }
        if (record.status() != CommissionRewardStatus.MAIL_CREATED
            && record.status() != CommissionRewardStatus.CLAIMED) {
          commissions.save(record.mailCreated(mailId));
        }
        return DeliveryResult.ALREADY_DELIVERED;
      }
      if (record.status() == CommissionRewardStatus.CLAIMED
          || (record.status() == CommissionRewardStatus.MAIL_CREATED
              && !mailId.equals(record.mailId()))) {
        return DeliveryResult.STATE_UNKNOWN;
      }
      if (!MailboxCapacityPolicy.canAddPersonal(
          MailType.SYSTEM, ledger.listPersonal(record.playerId()).size())) {
        return DeliveryResult.RETRYABLE_FAILURE;
      }

      long now = Math.max(System.currentTimeMillis(), Math.max(1L, record.createdAt()));
      String requester = record.requesterId().isBlank() ? "系统" : record.requesterId();
      MailRecord mail = new MailRecord(
          mailId,
          MailType.SYSTEM,
          null,
          requester,
          "委托奖励结算",
          "委托已完成，报酬已随信附上。请领取货币附件。\n奖励记录: "
              + record.rewardRecordId(),
          "commission.reward",
          now,
          0,
          List.of(),
          record.rewardRecordId(),
          record.currencyRewardAmount(),
          false,
          false,
          true);
      ledger.addPersonal(record.playerId(), mail, () -> markDirty(mailbox));
      try {
        commissions.save(record.mailCreated(mailId));
      } catch (RuntimeException saveFailure) {
        // The mailbox metadata is durable and has a deterministic identity.  Leaving the reward
        // pending makes the next tick reconcile the already-created mail without duplicating it.
        LOGGER.error("Commission reward record save failed after mail creation reward={} mail={}",
            record.rewardRecordId(), mailId, saveFailure);
        return DeliveryResult.RETRYABLE_FAILURE;
      }
      notifyNewMail(record.playerId(), mail.subject());
      return DeliveryResult.CREATED;
    } catch (IllegalStateException full) {
      return DeliveryResult.RETRYABLE_FAILURE;
    } catch (RuntimeException failure) {
      LOGGER.error("Commission reward mail delivery failed reward={} player={}",
          record.rewardRecordId(), record.playerId(), failure);
      return DeliveryResult.STATE_UNKNOWN;
    }
  }

  @Override
  public ClaimResult claim(UUID rewardRecordId, UUID playerId, long nowMillis) {
    CommissionRewardDeliveryPort.validateClaim(rewardRecordId, playerId, nowMillis);
    CommissionRewardRecord record = commissions.find(rewardRecordId).orElse(null);
    if (record == null) return ClaimResult.NOT_FOUND;
    if (!record.playerId().equals(playerId)) return ClaimResult.WRONG_PLAYER;
    if (record.status() == CommissionRewardStatus.CLAIMED) return ClaimResult.ALREADY_CLAIMED;
    if (record.status() != CommissionRewardStatus.MAIL_CREATED || record.mailId() == null) {
      return ClaimResult.STATE_UNKNOWN;
    }

    try {
      Object mailbox = mailboxSavedData(level);
      MailboxLedger ledger = mailboxLedger(mailbox);
      MailRecord mail = ledger.findPersonal(playerId, record.mailId());
      if (mail == null || !mail.hasCurrencyReward()) return ClaimResult.NOT_FOUND;
      // Mailbox handlers may have completed the claim before the reward repository was updated.
      // Reconcile that durable marker without crediting a second time.
      if (mail.currencyRewardClaimed()) {
        try {
          commissions.save(record.claimed(nowMillis));
          return ClaimResult.ALREADY_CLAIMED;
        } catch (RuntimeException persistFailure) {
          LOGGER.error("Commission reward claim reconciliation failed reward={}",
              rewardRecordId, persistFailure);
          return ClaimResult.PERSIST_FAILED;
        }
      }

      EconomySavedData economy = EconomySavedData.getInstance(level);
      int amount = record.currencyRewardAmount();
      if (economy.previewCreditExact(playerId, amount) != BalanceMutationResult.SUCCESS) {
        return ClaimResult.BALANCE_LIMIT;
      }
      BalanceMutationResult credited = economy.creditExact(
          playerId, amount, "委托奖励", "领取委托货币奖励");
      if (credited != BalanceMutationResult.SUCCESS) {
        return credited == BalanceMutationResult.BALANCE_LIMIT
            ? ClaimResult.BALANCE_LIMIT : ClaimResult.STATE_UNKNOWN;
      }
      try {
        MailboxLedger.MutationResult marked = ledger.markCurrencyRewardClaimed(
            playerId, record.mailId(), () -> markDirty(mailbox));
        if (marked != MailboxLedger.MutationResult.UPDATED
            && marked != MailboxLedger.MutationResult.NO_CHANGE) {
          return ClaimResult.PERSIST_FAILED;
        }
        commissions.save(record.claimed(nowMillis));
        return ClaimResult.CLAIMED;
      } catch (RuntimeException persistFailure) {
        // If the mailbox marker reached disk, a retry will reconcile the reward record and will
        // not credit again.  Do not debit here: a rollback after a successful marker could lose a
        // legitimately claimed reward.
        LOGGER.error("Commission reward claim persistence failed reward={} player={}",
            rewardRecordId, playerId, persistFailure);
        return ClaimResult.PERSIST_FAILED;
      }
    } catch (RuntimeException failure) {
      LOGGER.error("Commission reward claim failed reward={} player={}",
          rewardRecordId, playerId, failure);
      return ClaimResult.STATE_UNKNOWN;
    }
  }

  private static UUID deterministicMailId(UUID rewardRecordId) {
    return UUID.nameUUIDFromBytes(("economysystem:commission-mail:" + rewardRecordId)
        .getBytes(StandardCharsets.UTF_8));
  }

  private static boolean matches(MailRecord mail, CommissionRewardRecord record) {
    return mail.rewardRecordId() != null
        && mail.rewardRecordId().equals(record.rewardRecordId())
        && mail.currencyRewardAmount() == record.currencyRewardAmount();
  }

  private void notifyNewMail(UUID playerId, String subject) {
    try {
      Method method = Class.forName(MAILBOX_HANDLER_CLASS)
          .getDeclaredMethod("notifyNewMail", net.minecraft.server.level.ServerPlayer.class,
              MailType.class, String.class, String.class);
      method.setAccessible(true);
      net.minecraft.server.level.ServerPlayer player =
          level.getServer().getPlayerList().getPlayer(playerId);
      if (player != null) method.invoke(null, player, MailType.SYSTEM, "系统", subject);
    } catch (ReflectiveOperationException | RuntimeException ignored) {
      // Notification is a best-effort side effect; the durable mail is already committed.
    }
  }

  private static Object mailboxSavedData(ServerLevel level) {
    try {
      Class<?> type = Class.forName(MAILBOX_DATA_CLASS);
      Method get = type.getDeclaredMethod("get", ServerLevel.class);
      get.setAccessible(true);
      return get.invoke(null, level);
    } catch (ReflectiveOperationException failure) {
      throw reflectionFailure("mailbox SavedData lookup failed", failure);
    }
  }

  private static MailboxLedger mailboxLedger(Object mailbox) {
    try {
      Method ledger = mailbox.getClass().getDeclaredMethod("ledger");
      ledger.setAccessible(true);
      return (MailboxLedger) ledger.invoke(mailbox);
    } catch (ReflectiveOperationException failure) {
      throw reflectionFailure("mailbox ledger lookup failed", failure);
    }
  }

  private static void markDirty(Object mailbox) {
    try {
      Method markDirty = mailbox.getClass().getDeclaredMethod("markDirty");
      markDirty.setAccessible(true);
      markDirty.invoke(mailbox);
    } catch (ReflectiveOperationException failure) {
      throw reflectionFailure("mailbox dirty marker failed", failure);
    }
  }

  private static RuntimeException reflectionFailure(String message, ReflectiveOperationException failure) {
    Throwable cause = failure;
    if (failure instanceof InvocationTargetException target && target.getCause() != null) {
      cause = target.getCause();
    }
    return new IllegalStateException(message, cause);
  }
}
