package com.mo.economy_system.core.territory_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.utils.Util_Message;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TerritoryManager {

    private static TerritorySavedData savedData;
    private static boolean initialized = false;
    public static final File CONFIG_FILE = new File(FMLPaths.CONFIGDIR.get().toFile() + File.separator + EconomySystem.MODID, "territory_buffs.json");

    private static final Map<UUID, Territory> territoryByID = Collections.synchronizedMap(new HashMap<>());
    private static final Map<UUID, List<Territory>> territoriesByOwner = new ConcurrentHashMap<>();

    // 初始化一个有限边界的四叉树
    public static QuadTree quadTree = new QuadTree(0, new Bounds(-30000, -30000, 60000, 60000));

    // 自动保存间隔（60秒）
    private static final long AUTO_SAVE_INTERVAL = 60000L;
    private static long lastSaveTime = System.currentTimeMillis();

    // 初始化领地管理器
    public static void initialize(ServerLevel level) {
        if (initialized) return;

        TerritoryBuffManager.initConfig(); // 初始化 Buff 配置

        quadTree.clear(); // 清空四叉树
        savedData = TerritorySavedData.getInstance(level);

        // ServerMessageUtil.log("Initializing TerritoryManager...");

        for (Territory territory : savedData.getAllTerritories()) {

            addTerritory(territory);

        }

        initialized = true;
        // ServerMessageUtil.log("TerritoryManager initialized with " + territoryByID.size() + " territories.");
    }

    // 添加领地
    public static void addTerritory(Territory territory) {
        if (!territoryByID.containsKey(territory.getTerritoryID())) {
            territoryByID.put(territory.getTerritoryID(), territory);
            territoriesByOwner.computeIfAbsent(territory.getOwnerUUID(), k -> new ArrayList<>()).add(territory);

            // 动态扩展四叉树边界
            if (!quadTree.getBounds().contains(territory.getBounds())) {
                expandQuadTreeBounds(territory.getBounds());
            }

            quadTree.insert(territory); // 插入四叉树
            if (savedData != null) {
                savedData.addTerritory(territory);
            }

            updateTerritoryBuffs(territory);

            autoSave(); // 自动保存
            // ServerMessageUtil.log("Territory added: " + territory.getName());
        }
    }

    private static void updateTerritoryBuffs(Territory territory) {
        System.out.println("------init territory Buff-------");

        // 🔹 读取配置文件中的 Buff ID
        Set<String> configBuffs = TerritoryBuffManager.getAllBuffIDs();

        // 🔹 获取当前领地已有的 Buff ID（从 List 生成 Set）
        Set<String> existingBuffs = new HashSet<>();
        for (TerritoryBuff buff : territory.getTerritoryBuffs()) {
            existingBuffs.add(buff.getId());
        }

        // **1. 添加新 Buff（配置文件有，领地没有）**
        for (String buffId : configBuffs) {
            if (!existingBuffs.contains(buffId)) {
                System.out.println("add Buff: " + buffId);
                TerritoryBuffConfig buffConfig = TerritoryBuffManager.getBuffConfig(buffId);
                if (buffConfig != null) {
                    TerritoryBuff newBuff = new TerritoryBuff(buffConfig.getId(),
                            buffConfig.getDisplayText(),
                            buffConfig.getEffectId(),
                            buffConfig.isInitialUnlockState(),
                            buffConfig.getInitialLevel(),
                            buffConfig.getSingleUpgradeLevel(),
                            buffConfig.getMaxLevel(),
                            buffConfig.getUpgradeCost());
                    territory.addBuffs(newBuff);
                }
            }
        }

        // **2. 删除已移除 Buff（配置文件没有，领地有）**
        Iterator<TerritoryBuff> iterator = territory.getTerritoryBuffs().iterator();
        while (iterator.hasNext()) {
            TerritoryBuff buff = iterator.next();
            if (!configBuffs.contains(buff.getId())) {
                System.out.println("remove Buff: " + buff.getId());
                iterator.remove(); // 移除 Buff
            }
        }

        // **3. 更新已存在 Buff（同步配置信息）**
        for (TerritoryBuff buff : territory.getTerritoryBuffs()) {
            if (configBuffs.contains(buff.getId())) {
                System.out.println("Update Buff: " + buff.getId());
                TerritoryBuffConfig buffConfig = TerritoryBuffManager.getBuffConfig(buff.getId());

                if (buffConfig != null) {
                    // **更新 Buff 的属性**
                    boolean changed = false;

                    if (!buff.getDisplayText().equals(buffConfig.getDisplayText())) {
                        buff.setDisplayText(buffConfig.getDisplayText());
                        changed = true;
                    }

                    if (!buff.getEffectId().equals(buffConfig.getEffectId())) {
                        buff.setEffectId(buffConfig.getEffectId());
                        changed = true;
                    }

                    if (buff.getSingleUpgradeLevel() != buffConfig.getSingleUpgradeLevel()) {
                        buff.setSingleUpgradeLevel(buffConfig.getSingleUpgradeLevel());
                        changed = true;
                    }

                    if (buff.getMaxLevel() != buffConfig.getMaxLevel()) {
                        buff.setMaxLevel(buffConfig.getMaxLevel());
                        changed = true;
                    }

                    if (!buff.getUpgradeCost().equals(buffConfig.getUpgradeCost())) {
                        buff.setUpgradeCost(buffConfig.getUpgradeCost());
                        changed = true;
                    }

                    // 确保当前等级不超过最大等级
                    if (buff.getLevel() > buffConfig.getMaxLevel()) {
                        buff.setLevel(buffConfig.getMaxLevel());
                        changed = true;
                    }

                    // **如果有任何变化，则打印日志**
                    if (changed) {
                        System.out.println("Buff " + buff.getId() + " 更新完成！");
                    }
                }
            }
        }


        autoSave(); // 自动保存
    }

    // **🔹 解锁 Buff**
    public static boolean unlockBuff(UUID territoryID, String buffID) {
        Territory territory = getTerritoryByID(territoryID);
        if (territory == null) {
            return false; // 领地不存在
        }

        TerritoryBuff buff = territory.getBuff(buffID);
        if (buff == null) {
            return false; // Buff 不存在
        }

        if (!buff.isUnlocked()) {
            buff.setUnlocked(true); // 直接解锁
            markDirty();
            return true; // 成功解锁
        }

        return false; // 已解锁，返回 false
    }

    // **🔹 升级 Buff**
    public static boolean upgradeBuff(UUID territoryID, String buffID) {
        Territory territory = getTerritoryByID(territoryID);
        if (territory == null) {
            return false; // 领地不存在
        }

        TerritoryBuff buff = territory.getBuff(buffID);
        if (buff == null) {
            return false; // Buff 不存在
        }

        TerritoryBuffConfig config = TerritoryBuffManager.getBuffConfig(buffID);
        if (config == null) {
            return false; // 配置不存在
        }

        if (buff.getLevel() < config.getMaxLevel()) {
            buff.setLevel(Math.min(config.getMaxLevel(), buff.getLevel() + config.getSingleUpgradeLevel())); // 直接升级
            markDirty();
            return true; // 成功升级
        }

        return false; // 已达最大等级，返回 false
    }

    // 移除领地
    public static void removeTerritory(UUID territoryID) {
        Territory territory = territoryByID.remove(territoryID);
        if (territory != null) {
            territoriesByOwner.getOrDefault(territory.getOwnerUUID(), new ArrayList<>()).remove(territory);

            quadTree.remove(territory); // 从四叉树中移除
            if (savedData != null) {
                savedData.removeTerritory(territoryID);
            }

            autoSave(); // 自动保存
            // ServerMessageUtil.log("Territory removed: " + territory.getName());
        }
    }

    // 查询指定位置的领地（X 和 Z 轴）
    public static Territory getTerritoryAtIgnoreY(int x, int z) {
        List<Territory> candidates = quadTree.query(x, z);
        return candidates.stream()
                .filter(territory -> territory.isWithinBoundsIgnoreY(x, z))
                .findFirst()
                .orElse(null);
    }

    // 查询指定维度和 X/Z 位置的领地。领地保护必须带维度判断，否则不同维度同坐标会互相误判。
    public static Territory getTerritoryAtIgnoreY(ResourceKey<Level> dimension, int x, int z) {
        List<Territory> candidates = quadTree.query(x, z);
        return candidates.stream()
                .filter(territory -> territory.getDimension().equals(dimension))
                .filter(territory -> territory.isWithinBoundsIgnoreY(x, z))
                .findFirst()
                .orElse(null);
    }

    // 获取玩家拥有的所有领地
    public static List<Territory> getTerritoriesByOwner(UUID ownerUUID) {
        return territoriesByOwner.getOrDefault(ownerUUID, new ArrayList<>());
    }

    // 获取玩家有权限的领地（排除自己拥有的领地）
    public static List<Territory> getAuthorizedTerritories(UUID playerUUID) {
        List<Territory> authorizedTerritories = new ArrayList<>();
        synchronized (territoryByID) {
            for (Territory territory : territoryByID.values()) {
                if (!territory.isOwner(playerUUID) && territory.hasPermission(playerUUID)) {
                    authorizedTerritories.add(territory);
                }
            }
        }
        return authorizedTerritories;
    }

    // 根据 ID 获取领地
    public static Territory getTerritoryByID(UUID territoryID) {
        return territoryByID.get(territoryID);
    }

    // 获取所有领地
    public static List<Territory> getAllTerritories() {
        return new ArrayList<>(territoryByID.values());
    }

    // 检查玩家是否有权限
    public static boolean isPlayerAuthorized(UUID territoryID, UUID playerUUID) {
        Territory territory = getTerritoryByID(territoryID);
        return territory != null && territory.hasPermission(playerUUID);
    }

    // 扩展四叉树边界
    private static void expandQuadTreeBounds(Bounds newBounds) {
        Bounds currentBounds = quadTree.getBounds();
        int newMinX = Math.min(currentBounds.x, newBounds.x);
        int newMinZ = Math.min(currentBounds.z, newBounds.z);
        int newMaxX = Math.max(currentBounds.x + currentBounds.width, newBounds.x + newBounds.width);
        int newMaxZ = Math.max(currentBounds.z + currentBounds.height, newBounds.z + newBounds.height);

        Bounds expandedBounds = new Bounds(newMinX, newMinZ, newMaxX - newMinX, newMaxZ - newMinZ);

        QuadTree newQuadTree = new QuadTree(0, expandedBounds);
        synchronized (territoryByID) {
            for (Territory territory : territoryByID.values()) {
                newQuadTree.insert(territory);
            }
        }
        quadTree.clear();
        quadTree.copyFrom(newQuadTree);
        // ServerMessageUtil.log("QuadTree bounds expanded: " + expandedBounds);
    }

    // 自动保存
    private static void autoSave() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSaveTime >= AUTO_SAVE_INTERVAL) {
            markDirty();
            lastSaveTime = currentTime;
            // ServerMessageUtil.log("Territory data saved.");
        }
    }

    public static void markDirty() {
        if (savedData != null) {
            savedData.setDirty();
        }
    }

    // 清空领地管理器
    public static void reset() {
        savedData = null;
        initialized = false;
        territoryByID.clear();
        territoriesByOwner.clear();
        quadTree.clear();
    }
}
