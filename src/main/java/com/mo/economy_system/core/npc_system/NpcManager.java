package com.mo.economy_system.core.npc_system;

import net.minecraft.core.registries.BuiltInRegistries;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.npc_system.Packet_OpenNpcDialogueGUI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class NpcManager {
    public static final String ENTITY_NPC_ID_TAG = "EconomySystemNpcId";
    private static final File NPC_DATA_FILE = new File("config/economy_system/npc_data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private static final Type NPC_MAP_TYPE = new TypeToken<Map<Integer, NpcData>>() {}.getType();

    private static Map<Integer, NpcData> npcCache = new ConcurrentHashMap<>();

    public static void init() {
        ensureFile();
        load();
        NpcRelationManager.init();
    }

    public static void load() {
        ensureFile();
        try (FileReader reader = new FileReader(NPC_DATA_FILE)) {
            Map<Integer, NpcData> loaded = GSON.fromJson(reader, NPC_MAP_TYPE);
            npcCache = loaded == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(loaded);
            if (npcCache.isEmpty()) {
                createDefaultNpc();
                save();
            }
            EconomySystem.LOGGER.info("NPC数据加载完成，共 {} 个NPC", npcCache.size());
        } catch (IOException e) {
            npcCache = new ConcurrentHashMap<>();
            EconomySystem.LOGGER.error("加载NPC数据失败", e);
        }
    }

    public static void save() {
        ensureFile();
        try (FileWriter writer = new FileWriter(NPC_DATA_FILE)) {
            GSON.toJson(npcCache, writer);
        } catch (IOException e) {
            EconomySystem.LOGGER.error("保存NPC数据失败", e);
        }
    }

    public static Optional<NpcData> getNpc(int npcId) {
        return Optional.ofNullable(npcCache.get(npcId));
    }

    public static List<NpcData> getAllNpcs() {
        List<NpcData> npcs = new ArrayList<>(npcCache.values());
        npcs.sort(Comparator.comparingInt(NpcData::getNpcId));
        return npcs;
    }

    public static boolean openNpcDialogue(ServerPlayer player, int npcId) {
        return openNpcDialogue(player, npcId, -1);
    }

    public static boolean openNpcDialogue(ServerPlayer player, int npcId, int entityId) {
        Optional<NpcData> npc = getNpc(npcId);
        if (npc.isEmpty()) {
            return false;
        }
        EconomySystem_NetworkManager.sendToClient(new Packet_OpenNpcDialogueGUI(createViewData(player, npc.get(), entityId)), player);
        return true;
    }

    public static void handleInteraction(ServerPlayer player, int npcId, int entityId, NpcInteractionType interactionType) {
        Optional<NpcData> optionalNpc = getNpc(npcId);
        if (optionalNpc.isEmpty()) {
            return;
        }

        NpcData npc = optionalNpc.get();
        int requiredFavorability = npc.getActionFavorabilityRequirements().getOrDefault(interactionType.name(), 0);
        if (!NpcRelationManager.canUseAction(npcId, player.getUUID(), interactionType, requiredFavorability)) {
            openNpcDialogue(player, npcId, entityId);
            return;
        }

        if (interactionType == NpcInteractionType.DIALOGUE) {
            NpcRelationManager.addFavorability(npcId, player.getUUID(), 1);
        } else if (interactionType == NpcInteractionType.GIFT_ITEM) {
            handleGift(player, npc);
        }

        openNpcDialogue(player, npcId, entityId);
    }

    public static NpcDialogueViewData createViewData(ServerPlayer player, NpcData npc) {
        return createViewData(player, npc, -1);
    }

    public static NpcDialogueViewData createViewData(ServerPlayer player, NpcData npc, int entityId) {
        NpcRelationData relation = NpcRelationManager.getRelation(npc.getNpcId(), player.getUUID());
        NpcThoughtData thought = npc.getCurrentThought();
        return new NpcDialogueViewData(
                npc.getNpcId(),
                entityId,
                npc.getNpcName(),
                npc.getNpcIntroduction(),
                npc.getNpcGender(),
                npc.getNpcProfession(),
                npc.getStoryStageId(),
                npc.getDialogues(),
                thought == null ? "" : thought.getThoughtText(),
                thought == null ? "" : thought.getWantedItemId(),
                relation.getFavorability(),
                relation.getRelationType().getDisplayName(),
                getAvailableActionNames(player, npc)
        );
    }

    private static void handleGift(ServerPlayer player, NpcData npc) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        NpcThoughtData thought = npc.getCurrentThought();
        int favorability = 2;
        if (thought != null && itemId.equals(thought.getWantedItemId())) {
            favorability = thought.getFavorabilityReward();
        }
        stack.shrink(1);
        NpcRelationManager.addFavorability(npc.getNpcId(), player.getUUID(), favorability);
    }

    private static List<String> getAvailableActionNames(ServerPlayer player, NpcData npc) {
        List<String> actions = new ArrayList<>();
        for (NpcInteractionType type : NpcInteractionType.values()) {
            int required = npc.getActionFavorabilityRequirements().getOrDefault(type.name(), 0);
            if (NpcRelationManager.canUseAction(npc.getNpcId(), player.getUUID(), type, required)) {
                actions.add(type.name());
            }
        }
        return actions;
    }

    private static void createDefaultNpc() {
        NpcData npc = new NpcData(1, "剧情记录员", "他记录着服务器共同推进的故事，也会记住每位玩家与他的交情。", "未知", "记录员");
        List<String> dialogues = new ArrayList<>();
        dialogues.add("欢迎回来。这个世界的故事不是一个人写完的。");
        dialogues.add("如果你带来了我正在寻找的东西，我会记住这份帮助。");
        npc.setDialogues(dialogues);
        npc.setStoryStageId(1);
        npc.setCurrentThought(new NpcThoughtData("我现在想要一个苹果，用来确认赠礼系统是否正常。", "minecraft:apple", "", 20, 0.0D));
        Map<String, Integer> requirements = new HashMap<>();
        requirements.put(NpcInteractionType.DIALOGUE.name(), 0);
        requirements.put(NpcInteractionType.GIFT_ITEM.name(), 0);
        requirements.put(NpcInteractionType.FOLLOW.name(), 300);
        requirements.put(NpcInteractionType.SET_HOME.name(), 300);
        requirements.put(NpcInteractionType.VIEW_BACKPACK.name(), 600);
        requirements.put(NpcInteractionType.ASSIGN_TASK.name(), 100);
        requirements.put(NpcInteractionType.WARNING_RULES.name(), 600);
        npc.setActionFavorabilityRequirements(requirements);
        npcCache.put(npc.getNpcId(), npc);
    }

    private static void ensureFile() {
        try {
            File parent = NPC_DATA_FILE.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!NPC_DATA_FILE.exists()) {
                NPC_DATA_FILE.createNewFile();
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("初始化NPC数据文件失败", e);
        }
    }
}
