package com.mo.economy_system.core.economy_system;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;
import java.util.stream.Collectors;

public class EconomySavedData extends SavedData {
    private static final String DATA_NAME = "economy_data";
    public static final int MAX_BALANCE = Integer.MAX_VALUE;
    public static final int MAX_LOGS_PER_PLAYER = 1000;
    private final Map<UUID, Integer> accounts = new HashMap<>();
    private final Map<UUID, List<String>> offlineMessages = new HashMap<>(); // 用于存储离线消息
    private final Map<UUID, Deque<BalanceLogEntry>> balanceLogs = new HashMap<>();

    // 获取玩家余额
    public int getBalance(UUID playerUUID) {
        return accounts.getOrDefault(playerUUID, 0);
    }

    // 设置玩家余额
    public void setBalance(UUID playerUUID, int amount) {
        setBalance(playerUUID, amount, "系统", "余额设置");
    }

    public void setBalance(UUID playerUUID, int amount, String category, String reason) {
        int before = getBalance(playerUUID);
        int after = Math.max(0, amount);
        accounts.put(playerUUID, after);
        recordBalanceLog(playerUUID, category, reason, after - before, before, after);
        this.setDirty(); // 标记数据已更改，确保保存到文件
    }

    // 增加余额
    public boolean addBalance(UUID playerUUID, int amount) {
        return addBalance(playerUUID, amount, "系统", "余额增加");
    }

    public boolean addBalance(UUID playerUUID, int amount, String category, String reason) {
        if (amount <= 0) {
            return false;
        }
        int balance = getBalance(playerUUID);
        if (balance > MAX_BALANCE - amount) {
            setBalance(playerUUID, MAX_BALANCE, category, reason);
            return true;
        }
        setBalance(playerUUID, balance + amount, category, reason);
        return true;
    }

    // 减少余额
    public boolean minBalance(UUID playerUUID, int amount) {
        return minBalance(playerUUID, amount, "系统", "余额减少");
    }

    public boolean minBalance(UUID playerUUID, int amount, String category, String reason) {
        if (amount <= 0) {
            return false;
        }
        int balance = getBalance(playerUUID);
        if (balance >= amount) {
            setBalance(playerUUID, balance - amount, category, reason);
            return true;
        }
        return false; // 余额不足
    }

    private void recordBalanceLog(UUID playerUUID, String category, String reason, int delta, int before, int after) {
        if (delta == 0) {
            return;
        }
        Deque<BalanceLogEntry> logs = balanceLogs.computeIfAbsent(playerUUID, key -> new ArrayDeque<>());
        logs.addFirst(new BalanceLogEntry(System.currentTimeMillis(), category, reason, delta, before, after));
        while (logs.size() > MAX_LOGS_PER_PLAYER) {
            logs.removeLast();
        }
    }

    public List<BalanceLogEntry> getBalanceLogs(UUID playerUUID) {
        return new ArrayList<>(balanceLogs.getOrDefault(playerUUID, new ArrayDeque<>()));
    }

    public BalanceLogPage getBalanceLogs(UUID playerUUID, String category, int offset, int limit) {
        String normalizedCategory = category == null ? "全部" : category;
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(1, Math.min(100, limit));
        List<BalanceLogEntry> filtered = getBalanceLogs(playerUUID).stream()
                .filter(entry -> "全部".equals(normalizedCategory) || normalizedCategory.equals(entry.category()))
                .toList();
        int total = filtered.size();
        int fromIndex = Math.min(safeOffset, total);
        int toIndex = Math.min(fromIndex + safeLimit, total);
        return new BalanceLogPage(filtered.subList(fromIndex, toIndex), normalizedCategory, safeOffset, safeLimit, total);
    }

    // 检查是否有足够余额
    public boolean hasEnoughBalance(UUID playerUUID, int amount) {
        if (amount <= 0) {
            return false;
        }
        return getBalance(playerUUID) >= amount;
    }

    // 存储离线消息
    public void storeOfflineMessage(UUID playerUUID, String message) {
        offlineMessages.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(message);
        this.setDirty(); // 标记数据已更改
    }

    // 获取并清除离线消息
    public List<String> getOfflineMessages(UUID playerUUID) {
        List<String> messages = offlineMessages.remove(playerUUID);
        this.setDirty(); // 标记数据已更改
        return messages != null ? messages : new ArrayList<>();
    }

    // 返回一个只读的账户视图（防止外部修改原始数据）
    public List<Map.Entry<UUID, Integer>> getAllAccounts() {
        List<Map.Entry<UUID, Integer>> allAccounts = accounts.entrySet()
                .stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue())) // 按值降序排序
                .collect(Collectors.toList());
        return allAccounts;
    }

    // 返回一个只读的账户视图（防止外部修改原始数据）
    public List<Map.Entry<UUID, Integer>> getAllPlayers() {
        List<Map.Entry<UUID, Integer>> allPlayers = new ArrayList<>(accounts.entrySet());
        return allPlayers;
    }

    // 保存数据到 NBT
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        // 保存账户数据
        CompoundTag accountsTag = new CompoundTag();
        accounts.forEach((uuid, balance) -> accountsTag.putInt(uuid.toString(), balance));
        tag.put("accounts", accountsTag);

        // 保存离线消息
        CompoundTag offlineMessagesTag = new CompoundTag();
        offlineMessages.forEach((uuid, messages) -> {
            CompoundTag playerTag = new CompoundTag();
            for (int i = 0; i < messages.size(); i++) {
                playerTag.putString("message" + i, messages.get(i));
            }
            offlineMessagesTag.put(uuid.toString(), playerTag);
        });
        tag.put("offlineMessages", offlineMessagesTag);

        CompoundTag balanceLogsTag = new CompoundTag();
        balanceLogs.forEach((uuid, logs) -> {
            ListTag listTag = new ListTag();
            for (BalanceLogEntry entry : logs) {
                listTag.add(entry.toNBT());
            }
            balanceLogsTag.put(uuid.toString(), listTag);
        });
        tag.put("balanceLogs", balanceLogsTag);

        return tag;
    }

    // 从 NBT 加载数据
    public static EconomySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        EconomySavedData data = new EconomySavedData();

        // 加载账户数据
        if (tag.contains("accounts")) {
            CompoundTag accountsTag = tag.getCompound("accounts");
            for (String key : accountsTag.getAllKeys()) {
                UUID uuid = UUID.fromString(key);
                int balance = accountsTag.getInt(key);
                data.accounts.put(uuid, balance);
            }
        }

        // 加载离线消息
        if (tag.contains("offlineMessages")) {
            CompoundTag offlineMessagesTag = tag.getCompound("offlineMessages");
            for (String key : offlineMessagesTag.getAllKeys()) {
                UUID uuid = UUID.fromString(key);
                CompoundTag playerTag = offlineMessagesTag.getCompound(key);
                List<String> messages = new ArrayList<>();
                for (String messageKey : playerTag.getAllKeys()) {
                    messages.add(playerTag.getString(messageKey));
                }
                data.offlineMessages.put(uuid, messages);
            }
        }

        if (tag.contains("balanceLogs")) {
            CompoundTag balanceLogsTag = tag.getCompound("balanceLogs");
            for (String key : balanceLogsTag.getAllKeys()) {
                UUID uuid = UUID.fromString(key);
                ListTag listTag = balanceLogsTag.getList(key, Tag.TAG_COMPOUND);
                Deque<BalanceLogEntry> logs = new ArrayDeque<>();
                for (int i = 0; i < listTag.size() && i < MAX_LOGS_PER_PLAYER; i++) {
                    logs.add(BalanceLogEntry.fromNBT(listTag.getCompound(i)));
                }
                data.balanceLogs.put(uuid, logs);
            }
        }

        return data;
    }

    public static EconomySavedData getInstance(ServerLevel level) {
        // 获取主世界（Overworld）的保存数据
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded!");
        }
        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(EconomySavedData::new, EconomySavedData::load), DATA_NAME);
    }

    public record BalanceLogEntry(long timeMillis, String category, String reason, int delta, int beforeBalance, int afterBalance) {
        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("timeMillis", timeMillis);
            tag.putString("category", category == null ? "系统" : category);
            tag.putString("reason", reason == null ? "" : reason);
            tag.putInt("delta", delta);
            tag.putInt("beforeBalance", beforeBalance);
            tag.putInt("afterBalance", afterBalance);
            return tag;
        }

        public static BalanceLogEntry fromNBT(CompoundTag tag) {
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

    public record BalanceLogPage(List<BalanceLogEntry> logs, String category, int offset, int limit, int total) {
    }

}
