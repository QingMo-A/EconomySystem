package com.mo.economy_system.core.login_system;

import com.mo.economy_system.EconomySystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class PlayerLoginData {
    private UUID playerUUID;
    private String passwordAfterHash;  //哈希后的密码
    private String registerIP;   //注册时的ip
    private String lastLoginIP;  //上次登录后的ip
    private String lastLoginTime;  //上次登录后的时间
    private GameType lastGameMode;
    private long lastLogoutTime;  //上次退出时间戳（毫秒）- 用于快速登录判断
    private boolean hasCompletedNewPlayerGuidence;  // 是否已完成新手教程
    private boolean loginSessionCompleted;  // 当前会话是否已完成登录验证（用于区分登录验证状态和正常游戏状态）

    //无参构造
    public PlayerLoginData() {
    }

    //带参构造
    public PlayerLoginData(UUID playerUUID, String passwordAfterHash, String registerIP, String lastLoginIP, String lastLoginTime, GameType lastGameMode) {
        this.playerUUID = playerUUID;
        this.passwordAfterHash = passwordAfterHash;
        this.registerIP = registerIP;
        this.lastLoginIP = lastLoginIP;
        this.lastLoginTime = lastLoginTime;
        this.lastGameMode = lastGameMode;
        this.lastLogoutTime = 0L;  // 默认为0，表示未记录
    }

    /**
     * 设置密码（SHA-256哈希 + UUID盐值）
     * @param plainPassword 明文密码
     */
    public void setPassword(String plainPassword) {
        EconomySystem.LOGGER.info("setPassword方法开始执行，明文密码长度={}", plainPassword.length());

        try {
            // 使用 UUID 作为盐值，确保每个玩家的哈希值不同
            String salt = playerUUID.toString();
            String saltedPassword = plainPassword + salt;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            this.passwordAfterHash = hexString.toString();
            EconomySystem.LOGGER.info("密码哈希完成，SHA-256哈希值={}", passwordAfterHash);
        } catch (NoSuchAlgorithmException e) {
            EconomySystem.LOGGER.error("SHA-256算法不可用", e);
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 验证密码（接受明文）
     * @param plainPassword 明文密码
     */
    public boolean verifyPassword(String plainPassword) {
        if (passwordAfterHash == null || plainPassword == null) {
            return false;
        }

        try {
            // 使用相同的盐值（UUID）重新计算哈希
            String salt = playerUUID.toString();
            String saltedPassword = plainPassword + salt;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String computedHash = hexString.toString();
            boolean matches = computedHash.equals(passwordAfterHash);

            EconomySystem.LOGGER.info("验证密码: 输入密码哈希={}, 存储密码哈希={}, 匹配={}",
                computedHash, passwordAfterHash, matches);

            return matches;
        } catch (NoSuchAlgorithmException e) {
            EconomySystem.LOGGER.error("SHA-256算法不可用", e);
            return false;
        }
    }

    /**
     * 玩家修改密码
     * @param targetPlayer 申请修改密码的玩家
     * @param oldPassword 老密码
     * @param newPassword 新密码
     */
    public boolean changePassword(ServerPlayer targetPlayer, String oldPassword, String newPassword) {
        if (targetPlayer == null || oldPassword == null || newPassword == null) {
            return false;
        }

        if (this.verifyPassword(oldPassword)) {
            this.setPassword(newPassword);
            targetPlayer.sendSystemMessage(Component.literal("§a您已成功修改了您的密码"), true);
            return true;
        } else {
            targetPlayer.sendSystemMessage(Component.literal("§c您输入的旧密码有误，无法修改密码"), true);
            return false;
        }
    }

    /**
     * 管理员强制修改密码
     * @param adminPlayer 管理员玩家
     * @param targetPlayer 需要修改密码的目标玩家
     * @param newPassword 新密码
     */
    public boolean forgeChangePassword(ServerPlayer adminPlayer, ServerPlayer targetPlayer, String newPassword) {
        if (adminPlayer == null || targetPlayer == null || newPassword == null) {
            return false;
        }

        //不是管理员还想强行修改密码！休想！！
        if (adminPlayer.hasPermissions(2)) {
            this.setPassword(newPassword);
            adminPlayer.sendSystemMessage(Component.literal("§a您已成功修改玩家§e" + targetPlayer.getName().getString() + "§a的密码"), true);
            targetPlayer.sendSystemMessage(Component.literal("§a您的登录密码已成功被管理员§e" + adminPlayer.getName().getString() + "§a修改"), true);
            return true;
        } else {
            adminPlayer.sendSystemMessage(Component.literal("§c不是管理员还想强行修改密码！休想！！"), true);
            return false;
        }
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public String getPasswordAfterHash() {
        return passwordAfterHash;
    }

    public void setPasswordAfterHash(String passwordAfterHash) {
        this.passwordAfterHash = passwordAfterHash;
    }

    public String getRegisterIP() {
        return registerIP;
    }

    public void setRegisterIP(String registerIP) {
        this.registerIP = registerIP;
    }

    public String getLastLoginIP() {
        return lastLoginIP;
    }

    public void setLastLoginIP(String lastLoginIP) {
        this.lastLoginIP = lastLoginIP;
    }

    public String getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(String lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public GameType getLastGameMode() {
        return lastGameMode;
    }

    public void setGameMode(GameType gameMode) {
        this.lastGameMode = gameMode;
    }

    public long getLastLogoutTime() {
        return lastLogoutTime;
    }

    public void setLastLogoutTime(long lastLogoutTime) {
        this.lastLogoutTime = lastLogoutTime;
    }

    /**
     * 检查是否可以快速登录（同IP且5分钟内退出）
     * @param currentIp 当前玩家IP
     * @return true表示可以快速登录
     */
    public boolean canQuickLogin(String currentIp) {
        if (lastLogoutTime == 0L) {
//            EconomySystem.LOGGER.info("快速登录检查: 没有退出时间记录");
            return false;
        }

        long timeSinceLogout = System.currentTimeMillis() - lastLogoutTime;
        boolean ipMatches = currentIp.equals(lastLoginIP);
        boolean withinTimeLimit = timeSinceLogout <= 300000;  // 5分钟 = 300000毫秒

//        EconomySystem.LOGGER.info("快速登录检查详情:");
//        EconomySystem.LOGGER.info("  当前IP: " + currentIp);
//        EconomySystem.LOGGER.info("  上次IP: " + lastLoginIP);
//        EconomySystem.LOGGER.info("  IP匹配: " + ipMatches);
//        EconomySystem.LOGGER.info("  退出时间: " + lastLogoutTime);
//        EconomySystem.LOGGER.info("  距离退出: " + (timeSinceLogout / 1000) + "秒");
//        EconomySystem.LOGGER.info("  时间检查(<=300秒): " + withinTimeLimit);

        return ipMatches && withinTimeLimit;
    }

    public boolean gethasCompletedNewPlayerGuidence() {
        return hasCompletedNewPlayerGuidence;
    }

    public void setHasCompletedNewPlayerGuidence(boolean hasCompletedNewPlayerGuidence) {
        this.hasCompletedNewPlayerGuidence = hasCompletedNewPlayerGuidence;
    }

    public boolean isLoginSessionCompleted() {
        return loginSessionCompleted;
    }

    public void setLoginSessionCompleted(boolean loginSessionCompleted) {
        this.loginSessionCompleted = loginSessionCompleted;
    }
}