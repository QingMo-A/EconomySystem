package com.mo.economy_system.core.economy_system;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Forge 1.20.1 persistence shell for the loader-neutral {@link EconomyLedger}.
 */
public class EconomySavedData extends SavedData {
    private static final String DATA_NAME = "economy_data";
    public static final int MAX_BALANCE = EconomyLedger.MAX_BALANCE;
    public static final int MAX_LOGS_PER_PLAYER = EconomyLedger.MAX_LOGS_PER_PLAYER;

    private final EconomyLedger ledger = new EconomyLedger(this::setDirty);

    public int getBalance(UUID playerUUID) {
        return ledger.getBalance(playerUUID);
    }

    public void setBalance(UUID playerUUID, int amount) {
        ledger.setBalance(playerUUID, amount);
    }

    public void setBalance(UUID playerUUID, int amount, String category, String reason) {
        ledger.setBalance(playerUUID, amount, category, reason);
    }

    public boolean addBalance(UUID playerUUID, int amount) {
        return ledger.addBalance(playerUUID, amount);
    }

    public boolean addBalance(UUID playerUUID, int amount, String category, String reason) {
        return ledger.addBalance(playerUUID, amount, category, reason);
    }

    public boolean minBalance(UUID playerUUID, int amount) {
        return ledger.minBalance(playerUUID, amount);
    }

    public boolean minBalance(UUID playerUUID, int amount, String category, String reason) {
        return ledger.minBalance(playerUUID, amount, category, reason);
    }

    public List<BalanceLogEntry> getBalanceLogs(UUID playerUUID) {
        return ledger.getBalanceLogs(playerUUID);
    }

    public BalanceLogPage getBalanceLogs(UUID playerUUID, String category, int offset, int limit) {
        return ledger.getBalanceLogs(playerUUID, category, offset, limit);
    }

    public boolean hasEnoughBalance(UUID playerUUID, int amount) {
        return ledger.hasEnoughBalance(playerUUID, amount);
    }

    public boolean canCreditExact(UUID playerUUID, int amount) { return ledger.canCreditExact(playerUUID, amount); }
    public BalanceMutationResult creditExact(UUID playerUUID, int amount, String category, String reason) {
        return ledger.creditExact(playerUUID, amount, category, reason);
    }
    public BalanceMutationResult debitExact(UUID playerUUID, int amount, String category, String reason) {
        return ledger.debitExact(playerUUID, amount, category, reason);
    }

    public BalanceTransferResult transferBalance(
            UUID senderUUID,
            UUID recipientUUID,
            int amount,
            String category,
            String senderReason,
            String recipientReason
    ) {
        return ledger.transferBalance(
                senderUUID,
                recipientUUID,
                amount,
                category,
                senderReason,
                recipientReason
        );
    }

    public BalanceTransferResult previewTransferExact(UUID senderUUID, UUID recipientUUID, int amount) {
        return ledger.previewTransferExact(senderUUID, recipientUUID, amount);
    }

    public BalanceTransferResult transferExact(UUID senderUUID, UUID recipientUUID, int amount, String category,
                                               String senderReason, String recipientReason) {
        return ledger.transferExact(senderUUID, recipientUUID, amount, category, senderReason, recipientReason);
    }

    public void storeOfflineMessage(UUID playerUUID, String message) {
        ledger.storeOfflineMessage(playerUUID, message);
    }

    public List<String> getOfflineMessages(UUID playerUUID) {
        return ledger.getOfflineMessages(playerUUID);
    }

    public List<Map.Entry<UUID, Integer>> getAllAccounts() {
        return ledger.getAllAccounts();
    }

    public List<Map.Entry<UUID, Integer>> getAllPlayers() {
        return ledger.getAllPlayers();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        EconomyLedger.Snapshot snapshot = ledger.snapshot();

        CompoundTag accountsTag = new CompoundTag();
        snapshot.accounts().forEach((uuid, balance) -> accountsTag.putInt(uuid.toString(), balance));
        tag.put("accounts", accountsTag);

        CompoundTag offlineMessagesTag = new CompoundTag();
        snapshot.offlineMessages().forEach((uuid, messages) -> {
            CompoundTag playerTag = new CompoundTag();
            for (int index = 0; index < messages.size(); index++) {
                playerTag.putString("message" + index, messages.get(index));
            }
            offlineMessagesTag.put(uuid.toString(), playerTag);
        });
        tag.put("offlineMessages", offlineMessagesTag);

        CompoundTag balanceLogsTag = new CompoundTag();
        snapshot.balanceLogs().forEach((uuid, logs) -> {
            ListTag listTag = new ListTag();
            for (BalanceLogEntry entry : logs) {
                listTag.add(writeBalanceLog(entry));
            }
            balanceLogsTag.put(uuid.toString(), listTag);
        });
        tag.put("balanceLogs", balanceLogsTag);

        return tag;
    }

    public static EconomySavedData load(CompoundTag tag) {
        EconomySavedData data = new EconomySavedData();
        data.restoreLedger(tag);
        return data;
    }

    public static EconomySavedData getInstance(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded!");
        }
        return overworld.getDataStorage().computeIfAbsent(
                EconomySavedData::load,
                EconomySavedData::new,
                DATA_NAME
        );
    }

    private void restoreLedger(CompoundTag tag) {
        Map<UUID, Integer> accounts = new HashMap<>();
        Map<UUID, List<String>> offlineMessages = new HashMap<>();
        Map<UUID, List<BalanceLogEntry>> balanceLogs = new HashMap<>();

        if (tag.contains("accounts")) {
            CompoundTag accountsTag = tag.getCompound("accounts");
            for (String key : accountsTag.getAllKeys()) {
                accounts.put(UUID.fromString(key), accountsTag.getInt(key));
            }
        }

        if (tag.contains("offlineMessages")) {
            CompoundTag offlineMessagesTag = tag.getCompound("offlineMessages");
            for (String key : offlineMessagesTag.getAllKeys()) {
                CompoundTag playerTag = offlineMessagesTag.getCompound(key);
                List<String> messages = new ArrayList<>();
                for (String messageKey : playerTag.getAllKeys()) {
                    messages.add(playerTag.getString(messageKey));
                }
                offlineMessages.put(UUID.fromString(key), messages);
            }
        }

        if (tag.contains("balanceLogs")) {
            CompoundTag balanceLogsTag = tag.getCompound("balanceLogs");
            for (String key : balanceLogsTag.getAllKeys()) {
                ListTag listTag = balanceLogsTag.getList(key, Tag.TAG_COMPOUND);
                List<BalanceLogEntry> logs = new ArrayList<>();
                for (int index = 0; index < listTag.size() && index < MAX_LOGS_PER_PLAYER; index++) {
                    logs.add(readBalanceLog(listTag.getCompound(index)));
                }
                balanceLogs.put(UUID.fromString(key), logs);
            }
        }

        ledger.restore(new EconomyLedger.Snapshot(accounts, offlineMessages, balanceLogs));
    }

    private static CompoundTag writeBalanceLog(BalanceLogEntry entry) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("timeMillis", entry.timeMillis());
        tag.putString("category", entry.category() == null ? "系统" : entry.category());
        tag.putString("reason", entry.reason() == null ? "" : entry.reason());
        tag.putInt("delta", entry.delta());
        tag.putInt("beforeBalance", entry.beforeBalance());
        tag.putInt("afterBalance", entry.afterBalance());
        return tag;
    }

    private static BalanceLogEntry readBalanceLog(CompoundTag tag) {
        return new BalanceLogEntry(
                tag.getLong("timeMillis"),
                tag.getString("category"),
                tag.getString("reason"),
                tag.getInt("delta"),
                tag.getInt("beforeBalance"),
                tag.getInt("afterBalance")
        );
    }
}
