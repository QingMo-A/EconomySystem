package com.mo.economy_system.core.auth_system;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthSavedData extends SavedData {
    private static final String DATA_NAME = "auth_data";
    private final Map<UUID, String> passwords = new HashMap<>();
    private final Map<UUID, Boolean> loggedIn = new HashMap<>();

    public boolean isRegistered(UUID playerUUID) {
        return passwords.containsKey(playerUUID);
    }

    public boolean register(UUID playerUUID, String password) {
        if (isRegistered(playerUUID)) {
            return false;
        }
        passwords.put(playerUUID, password);
        loggedIn.put(playerUUID, false);
        this.setDirty();
        return true;
    }

    public boolean registerImmediate(UUID playerUUID, String password, ServerLevel level) {
        if (register(playerUUID, password)) {
            // 立即保存到磁盘 - 使用更可靠的保存方式
            try {
                // 先标记为脏数据
                this.setDirty();
                // 强制保存世界数据
                level.save(null, true, false);
            } catch (Exception e) {
                // 保存失败不影响注册流程，但打印错误日志
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public boolean login(UUID playerUUID, String password) {
        if (!isRegistered(playerUUID)) {
            return false;
        }
        String storedPassword = passwords.get(playerUUID);
        if (storedPassword.equals(password)) {
            loggedIn.put(playerUUID, true);
            this.setDirty();
            return true;
        }
        return false;
    }

    public boolean loginImmediate(UUID playerUUID, String password, ServerLevel level) {
        if (login(playerUUID, password)) {
            // 立即保存到磁盘 - 使用更可靠的保存方式
            try {
                // 先标记为脏数据
                this.setDirty();
                // 强制保存世界数据
                level.save(null, true, false);
            } catch (Exception e) {
                // 保存失败不影响登录流程，但打印错误日志
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public boolean isLoggedIn(UUID playerUUID) {
        return loggedIn.getOrDefault(playerUUID, false);
    }

    public void logout(UUID playerUUID) {
        loggedIn.put(playerUUID, false);
        this.setDirty();
    }

    public void playerLogout(UUID playerUUID) {
        loggedIn.put(playerUUID, false);
        this.setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag passwordsTag = new CompoundTag();
        passwords.forEach((uuid, password) -> passwordsTag.putString(uuid.toString(), password));
        tag.put("passwords", passwordsTag);

        CompoundTag loggedInTag = new CompoundTag();
        loggedIn.forEach((uuid, status) -> loggedInTag.putBoolean(uuid.toString(), status));
        tag.put("loggedIn", loggedInTag);

        return tag;
    }

    public static AuthSavedData load(CompoundTag tag) {
        AuthSavedData data = new AuthSavedData();

        if (tag.contains("passwords")) {
            CompoundTag passwordsTag = tag.getCompound("passwords");
            for (String key : passwordsTag.getAllKeys()) {
                UUID uuid = UUID.fromString(key);
                String password = passwordsTag.getString(key);
                data.passwords.put(uuid, password);
            }
        }

        if (tag.contains("loggedIn")) {
            CompoundTag loggedInTag = tag.getCompound("loggedIn");
            for (String key : loggedInTag.getAllKeys()) {
                UUID uuid = UUID.fromString(key);
                boolean status = loggedInTag.getBoolean(key);
                data.loggedIn.put(uuid, status);
            }
        }

        return data;
    }

    public static AuthSavedData getInstance(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded!");
        }
        return overworld.getDataStorage().computeIfAbsent(AuthSavedData::load, AuthSavedData::new, DATA_NAME);
    }
}
