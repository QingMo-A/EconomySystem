package com.mo.economy_system.core.npc_system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mo.economy_system.EconomySystem;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NpcRelationManager {
    private static final File RELATION_DATA_FILE = new File("config/economy_system/data/npc_relation_data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private static final Type RELATION_MAP_TYPE = new TypeToken<Map<String, NpcRelationData>>() {}.getType();

    private static Map<String, NpcRelationData> relationCache = new ConcurrentHashMap<>();

    public static void init() {
        ensureFile();
        load();
    }

    public static NpcRelationData getRelation(int npcId, UUID playerUUID) {
        String key = makeKey(npcId, playerUUID);
        return relationCache.computeIfAbsent(key, ignored -> new NpcRelationData(npcId, playerUUID));
    }

    public static void addFavorability(int npcId, UUID playerUUID, int amount) {
        NpcRelationData relation = getRelation(npcId, playerUUID);
        relation.addFavorability(amount);
        save();
    }

    public static boolean canUseAction(int npcId, UUID playerUUID, NpcInteractionType interactionType, int requiredFavorability) {
        return getRelation(npcId, playerUUID).getFavorability() >= requiredFavorability;
    }

    public static void load() {
        ensureFile();
        try (FileReader reader = new FileReader(RELATION_DATA_FILE)) {
            Map<String, NpcRelationData> loaded = GSON.fromJson(reader, RELATION_MAP_TYPE);
            relationCache = loaded == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(loaded);
            relationCache.values().forEach(NpcRelationData::refreshRelationType);
            EconomySystem.LOGGER.info("NPC关系数据加载完成，共 {} 条", relationCache.size());
        } catch (IOException e) {
            relationCache = new ConcurrentHashMap<>();
            EconomySystem.LOGGER.error("加载NPC关系数据失败", e);
        }
    }

    public static void save() {
        ensureFile();
        try (FileWriter writer = new FileWriter(RELATION_DATA_FILE)) {
            GSON.toJson(relationCache, writer);
        } catch (IOException e) {
            EconomySystem.LOGGER.error("保存NPC关系数据失败", e);
        }
    }

    private static String makeKey(int npcId, UUID playerUUID) {
        return npcId + ":" + playerUUID;
    }

    private static void ensureFile() {
        try {
            File parent = RELATION_DATA_FILE.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!RELATION_DATA_FILE.exists()) {
                RELATION_DATA_FILE.createNewFile();
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("初始化NPC关系数据文件失败", e);
        }
    }
}
