//package com.mo.economy_system.core.auth_system;
//
//import com.mo.economy_system.EconomySystem;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.saveddata.SavedData;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//
//
//// DEPRECATED: Old login system
//// public class AuthSavedData extends SavedData {
//    private static final String DATA_NAME = "auth_data";
//    private final Map<UUID, String> passwords = new HashMap<>();
//    private final Map<UUID, Boolean> loggedIn = new HashMap<>();
//    // 记录玩家退出时间和IP地址
//    private final Map<UUID, LogoutInfo> logoutInfo = new HashMap<>();
//    // 记录是否已完成新手教程
//    private final Map<UUID, Boolean> completedGuide = new HashMap<>();
//
//    // 退出信息记录类
//    public static class LogoutInfo {
//        private final String ipAddress;
//        private final long logoutTime;
//
//        public LogoutInfo(String ipAddress, long logoutTime) {
//            this.ipAddress = ipAddress;
//            this.logoutTime = logoutTime;
//        }
//
//        public String getIpAddress() {
//            return ipAddress;
//        }
//
//        public long getLogoutTime() {
//            return logoutTime;
//        }
//    }
//
//    public boolean isRegistered(UUID playerUUID) {
//        return passwords.containsKey(playerUUID);
//    }
//
//    public boolean register(UUID playerUUID, String password) {
//        if (isRegistered(playerUUID)) {
//            return false;
//        }
//        passwords.put(playerUUID, password);
//        loggedIn.put(playerUUID, false);
//        this.setDirty();
//        return true;
//    }
//
//    public boolean registerImmediate(UUID playerUUID, String password, ServerLevel level) {
//        if (register(playerUUID, password)) {
//            // 立即保存到磁盘 - 使用更可靠的保存方式
//            try {
//                // 先标记为脏数据
//                this.setDirty();
//                // 强制保存世界数据
//                level.save(null, true, false);
//            } catch (Exception e) {
//                // 保存失败不影响注册流程，但打印错误日志
//                e.printStackTrace();
//            }
//            return true;
//        }
//        return false;
//    }
//
//    public boolean login(UUID playerUUID, String password) {
//        if (!isRegistered(playerUUID)) {
//            return false;
//        }
//        String storedPassword = passwords.get(playerUUID);
//        if (storedPassword.equals(password)) {
//            loggedIn.put(playerUUID, true);
//            this.setDirty();
//            return true;
//        }
//        return false;
//    }
//
//    public boolean loginImmediate(UUID playerUUID, String password, ServerLevel level) {
//        if (login(playerUUID, password)) {
//            // 立即保存到磁盘 - 使用更可靠的保存方式
//            try {
//                // 先标记为脏数据
//                this.setDirty();
//                // 强制保存世界数据
//                level.save(null, true, false);
//            } catch (Exception e) {
//                // 保存失败不影响登录流程，但打印错误日志
//                e.printStackTrace();
//            }
//            return true;
//        }
//        return false;
//    }
//
//    public boolean isLoggedIn(UUID playerUUID) {
//        return loggedIn.getOrDefault(playerUUID, false);
//    }
//
//    public void logout(UUID playerUUID) {
//        loggedIn.put(playerUUID, false);
//        this.setDirty();
//    }
//
//    public void playerLogout(UUID playerUUID) {
//        loggedIn.put(playerUUID, false);
//        this.setDirty();
//    }
//
//    /**
//     * 记录玩家退出信息（IP地址和退出时间）
//     */
//    public void recordLogoutInfo(UUID playerUUID, String ipAddress) {
//        logoutInfo.put(playerUUID, new LogoutInfo(ipAddress, System.currentTimeMillis()));
//        this.setDirty();
//        EconomySystem.LOGGER.info("记录玩家退出信息 - UUID: " + playerUUID + ", IP: " + ipAddress);
//    }
//
//    /**
//     * 检查是否可以快速登录（同IP且5分钟内退出）
//     */
//    public boolean canQuickLogin(UUID playerUUID, String currentIp) {
//        LogoutInfo info = logoutInfo.get(playerUUID);
//        if (info == null) {
//            EconomySystem.LOGGER.info("快速登录检查: 没有找到退出信息记录");
//            return false;
//        }
//
//        String savedIp = info.getIpAddress();
//        long logoutTime = info.getLogoutTime();
//        long timeSinceLogout = System.currentTimeMillis() - logoutTime;
//
//        EconomySystem.LOGGER.info("快速登录检查详情:");
//        EconomySystem.LOGGER.info("  当前IP: " + currentIp);
//        EconomySystem.LOGGER.info("  保存IP: " + savedIp);
//        EconomySystem.LOGGER.info("  IP匹配: " + savedIp.equals(currentIp));
//        EconomySystem.LOGGER.info("  退出时间: " + logoutTime);
//        EconomySystem.LOGGER.info("  距离退出: " + (timeSinceLogout / 1000) + "秒");
//        EconomySystem.LOGGER.info("  时间检查(<=300秒): " + (timeSinceLogout <= 300000));
//
//        // 5分钟 = 300000毫秒
//        return savedIp.equals(currentIp) && timeSinceLogout <= 300000;
//    }
//
//    /**
//     * 快速登录
//     */
//    public void quickLogin(UUID playerUUID) {
//        loggedIn.put(playerUUID, true);
//        this.setDirty();
//    }
//
//    /**
//     * 快速登录并立即保存到磁盘
//     */
//    public void quickLoginImmediate(UUID playerUUID, ServerLevel level) {
//        quickLogin(playerUUID);
//        saveImmediately(level);
//    }
//
//    /**
//     * 检查是否已完成新手教程
//     */
//    public boolean hasCompletedGuide(UUID playerUUID) {
//        return completedGuide.getOrDefault(playerUUID, false);
//    }
//
//    /**
//     * 标记新手教程已完成
//     */
//    public void markGuideCompleted(UUID playerUUID) {
//        completedGuide.put(playerUUID, true);
//        this.setDirty();
//    }
//
//    /**
//     * 标记新手教程已完成并立即保存
//     */
//    public void markGuideCompletedImmediate(UUID playerUUID, ServerLevel level) {
//        markGuideCompleted(playerUUID);
//        saveImmediately(level);
//    }
//
//    /**
//     * 立即保存数据到磁盘
//     */
//    private void saveImmediately(ServerLevel level) {
//        try {
//            this.setDirty();
//            level.save(null, true, false);
//            EconomySystem.LOGGER.info("认证数据已立即保存到磁盘");
//        } catch (Exception e) {
//            EconomySystem.LOGGER.error("保存认证数据失败", e);
//        }
//    }
//
//    @Override
//    public CompoundTag save(CompoundTag tag) {
//        CompoundTag passwordsTag = new CompoundTag();
//        passwords.forEach((uuid, password) -> passwordsTag.putString(uuid.toString(), password));
//        tag.put("passwords", passwordsTag);
//
//        CompoundTag loggedInTag = new CompoundTag();
//        loggedIn.forEach((uuid, status) -> loggedInTag.putBoolean(uuid.toString(), status));
//        tag.put("loggedIn", loggedInTag);
//
//        CompoundTag logoutInfoTag = new CompoundTag();
//        logoutInfo.forEach((uuid, info) -> {
//            CompoundTag infoTag = new CompoundTag();
//            infoTag.putString("ip", info.getIpAddress());
//            infoTag.putLong("time", info.getLogoutTime());
//            logoutInfoTag.put(uuid.toString(), infoTag);
//        });
//        tag.put("logoutInfo", logoutInfoTag);
//
//        CompoundTag completedGuideTag = new CompoundTag();
//        completedGuide.forEach((uuid, completed) -> completedGuideTag.putBoolean(uuid.toString(), completed));
//        tag.put("completedGuide", completedGuideTag);
//
//        return tag;
//    }
//
//    public static AuthSavedData load(CompoundTag tag) {
//        AuthSavedData data = new AuthSavedData();
//
//        if (tag.contains("passwords")) {
//            CompoundTag passwordsTag = tag.getCompound("passwords");
//            for (String key : passwordsTag.getAllKeys()) {
//                UUID uuid = UUID.fromString(key);
//                String password = passwordsTag.getString(key);
//                data.passwords.put(uuid, password);
//            }
//        }
//
//        if (tag.contains("loggedIn")) {
//            CompoundTag loggedInTag = tag.getCompound("loggedIn");
//            for (String key : loggedInTag.getAllKeys()) {
//                UUID uuid = UUID.fromString(key);
//                boolean status = loggedInTag.getBoolean(key);
//                data.loggedIn.put(uuid, status);
//            }
//        }
//
//        if (tag.contains("logoutInfo")) {
//            CompoundTag logoutInfoTag = tag.getCompound("logoutInfo");
//            for (String key : logoutInfoTag.getAllKeys()) {
//                UUID uuid = UUID.fromString(key);
//                CompoundTag infoTag = logoutInfoTag.getCompound(key);
//                String ip = infoTag.getString("ip");
//                long time = infoTag.getLong("time");
//                data.logoutInfo.put(uuid, new LogoutInfo(ip, time));
//            }
//        }
//
//        if (tag.contains("completedGuide")) {
//            CompoundTag completedGuideTag = tag.getCompound("completedGuide");
//            for (String key : completedGuideTag.getAllKeys()) {
//                UUID uuid = UUID.fromString(key);
//                boolean completed = completedGuideTag.getBoolean(key);
//                data.completedGuide.put(uuid, completed);
//            }
//        }
//
//        return data;
//    }
//
//    public static AuthSavedData getInstance(ServerLevel level) {
//        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
//        if (overworld == null) {
//            throw new IllegalStateException("Overworld is not loaded!");
//        }
//        return overworld.getDataStorage().computeIfAbsent(AuthSavedData::load, AuthSavedData::new, DATA_NAME);
//    }
//}
