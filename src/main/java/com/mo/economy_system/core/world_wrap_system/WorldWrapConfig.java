package com.mo.economy_system.core.world_wrap_system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.init.Init;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class WorldWrapConfig {
    private static final String CONFIG_FILE_NAME = "world_wrap.json";
    private static final String DEFAULT_DIMENSION = "minecraft:overworld";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private static final File CONFIG_FILE = new File(Init.CONFIG_FOLDER_PATH, CONFIG_FILE_NAME);

    private static WorldWrapConfigData config = WorldWrapConfigData.createDefault();

    public static void init() {
        ensureFile();
        load();
    }

    public static void load() {
        ensureFile();
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            WorldWrapConfigData loaded = GSON.fromJson(reader, WorldWrapConfigData.class);
            config = loaded == null ? WorldWrapConfigData.createDefault() : loaded.normalize();
            save();
            EconomySystem.LOGGER.info("世界环绕配置加载完成：enabled={}, dimension={}, width={}, height={}, preview={}",
                    config.isEnabled(), config.getDimension(), config.getWidth(), config.getHeight(),
                    config.isClientChunkMirrorEnabled());
        } catch (IOException e) {
            config = WorldWrapConfigData.createDefault();
            EconomySystem.LOGGER.error("加载世界环绕配置失败", e);
        }
    }

    public static void save() {
        ensureFile();
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            EconomySystem.LOGGER.error("保存世界环绕配置失败", e);
        }
    }

    public static WorldWrapConfigData getConfig() {
        return config;
    }

    public static void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
        save();
    }

    public static void setClientChunkMirrorEnabled(boolean enabled) {
        config.setClientChunkMirrorEnabled(enabled);
        save();
    }

    public static void setEntityMirrorEnabled(boolean enabled) {
        config.setEntityMirrorEnabled(enabled);
        save();
    }

    private static void ensureFile() {
        try {
            File parent = CONFIG_FILE.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!CONFIG_FILE.exists()) {
                config = WorldWrapConfigData.createDefault();
                try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                    GSON.toJson(config, writer);
                }
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("初始化世界环绕配置文件失败", e);
        }
    }

    public static class WorldWrapConfigData {
        private boolean enabled = false;
        private String dimension = DEFAULT_DIMENSION;
        private double centerX = 0.0D;
        private double centerZ = 0.0D;
        private double width = 8000.0D;
        private double height = 8000.0D;
        private int cooldownTicks = 20;
        private boolean clientChunkMirrorEnabled = true;
        private int clientChunkMirrorRadius = 12;
        private int clientChunkMirrorSendBudgetPerTick = 96;
        private int preloadChunkRadius = 1;
        private int teleportPrefillRadius = 6;
        private int teleportPrefillSendLimit = 160;
        private boolean entityMirrorEnabled = true;
        private double boundaryWarningDistance = 50.0D;
        private boolean worldgenSmoothingEnabled = true;
        private double worldgenBlendDistance = 512.0D;

        public static WorldWrapConfigData createDefault() {
            return new WorldWrapConfigData();
        }

        public WorldWrapConfigData normalize() {
            if (dimension == null || dimension.isEmpty()) {
                dimension = DEFAULT_DIMENSION;
            }
            width = Math.max(16.0D, width);
            height = Math.max(16.0D, height);
            cooldownTicks = Math.max(1, cooldownTicks);
            clientChunkMirrorRadius = Math.max(1, Math.min(clientChunkMirrorRadius, 32));
            clientChunkMirrorSendBudgetPerTick = Math.max(1, Math.min(clientChunkMirrorSendBudgetPerTick, 256));
            preloadChunkRadius = Math.max(0, Math.min(preloadChunkRadius, 2));
            teleportPrefillRadius = Math.max(0, Math.min(teleportPrefillRadius, 16));
            teleportPrefillSendLimit = Math.max(0, Math.min(teleportPrefillSendLimit, 512));
            boundaryWarningDistance = Math.max(0.0D, boundaryWarningDistance);
            worldgenBlendDistance = Math.max(0.0D, worldgenBlendDistance);
            return this;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDimension() {
            return dimension;
        }

        public double getCenterX() {
            return centerX;
        }

        public double getCenterZ() {
            return centerZ;
        }

        public double getWidth() {
            return width;
        }

        public double getHeight() {
            return height;
        }

        public int getCooldownTicks() {
            return cooldownTicks;
        }

        public boolean isClientChunkMirrorEnabled() {
            return clientChunkMirrorEnabled;
        }

        public void setClientChunkMirrorEnabled(boolean clientChunkMirrorEnabled) {
            this.clientChunkMirrorEnabled = clientChunkMirrorEnabled;
        }

        public int getClientChunkMirrorRadius() {
            return clientChunkMirrorRadius;
        }

        public int getClientChunkMirrorSendBudgetPerTick() {
            return clientChunkMirrorSendBudgetPerTick;
        }

        public int getPreloadChunkRadius() {
            return preloadChunkRadius;
        }

        public int getTeleportPrefillRadius() {
            return teleportPrefillRadius;
        }

        public int getTeleportPrefillSendLimit() {
            return teleportPrefillSendLimit;
        }

        public boolean isEntityMirrorEnabled() {
            return entityMirrorEnabled;
        }

        public void setEntityMirrorEnabled(boolean entityMirrorEnabled) {
            this.entityMirrorEnabled = entityMirrorEnabled;
        }

        public double getBoundaryWarningDistance() {
            return boundaryWarningDistance;
        }

        public boolean isWorldgenSmoothingEnabled() {
            return worldgenSmoothingEnabled;
        }

        public double getWorldgenBlendDistance() {
            return worldgenBlendDistance;
        }

        public double getMinX() {
            return centerX - width / 2.0D;
        }

        public double getMaxX() {
            return centerX + width / 2.0D;
        }

        public double getMinZ() {
            return centerZ - height / 2.0D;
        }

        public double getMaxZ() {
            return centerZ + height / 2.0D;
        }
    }
}
