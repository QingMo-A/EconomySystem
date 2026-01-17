package com.mo.economy_system.network.packets.blueprint_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.blueprint_system.BlueprintConfig;
import com.mo.economy_system.core.blueprint_system.BlueprintConfigManager;
import com.mo.economy_system.core.blueprint_system.PlayerBlueprintData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 蓝图配置同步数据包
 * 从服务端同步蓝图配置到客户端
 */
public class Packet_SyncBlueprintConfig {
    private final List<String> defaultUnlockedItems;
    private final List<String> excludedKeywords;
    private final String serverIdentifier;

    // 服务端构造器
    public Packet_SyncBlueprintConfig(BlueprintConfig config, String serverIdentifier) {
        this.defaultUnlockedItems = new ArrayList<>(config.getDefaultUnlockedItems());
        this.excludedKeywords = new ArrayList<>(config.getExcludedKeywords());
        this.serverIdentifier = serverIdentifier;
    }

    // 编码
    public static void encode(Packet_SyncBlueprintConfig packet, FriendlyByteBuf buf) {
        // 写入默认解锁物品列表
        buf.writeInt(packet.defaultUnlockedItems.size());
        for (String item : packet.defaultUnlockedItems) {
            buf.writeUtf(item);
        }

        // 写入排除关键字列表
        buf.writeInt(packet.excludedKeywords.size());
        for (String keyword : packet.excludedKeywords) {
            buf.writeUtf(keyword);
        }

        // 写入服务端标识
        buf.writeUtf(packet.serverIdentifier != null ? packet.serverIdentifier : "");
    }

    // 解码
    public static Packet_SyncBlueprintConfig decode(FriendlyByteBuf buf) {
        List<String> defaultUnlockedItems = new ArrayList<>();
        int defaultCount = buf.readInt();
        for (int i = 0; i < defaultCount; i++) {
            defaultUnlockedItems.add(buf.readUtf());
        }

        List<String> excludedKeywords = new ArrayList<>();
        int excludedCount = buf.readInt();
        for (int i = 0; i < excludedCount; i++) {
            excludedKeywords.add(buf.readUtf());
        }

        String serverIdentifier = buf.readUtf();
        if (serverIdentifier.isEmpty()) {
            serverIdentifier = null;
        }

        return new Packet_SyncBlueprintConfig(createConfigFromLists(defaultUnlockedItems, excludedKeywords), serverIdentifier);
    }

    // 处理逻辑
    public static void handle(Packet_SyncBlueprintConfig packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.setPacketHandled(true);

        // 传递不可变参数
        final List<String> safeDefaultItems = new ArrayList<>(packet.defaultUnlockedItems);
        final List<String> safeExcludedKeywords = new ArrayList<>(packet.excludedKeywords);
        final String safeServerIdentifier = packet.serverIdentifier;

        context.enqueueWork(() -> processOnMainThread(safeDefaultItems, safeExcludedKeywords, safeServerIdentifier));
    }

    // 分发方法
    private static void processOnMainThread(List<String> defaultUnlockedItems, List<String> excludedKeywords, String serverIdentifier) {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> new ClientSyncRunnable(defaultUnlockedItems, excludedKeywords, serverIdentifier));
    }

    // 纯客户端逻辑
    @OnlyIn(Dist.CLIENT)
    private static class ClientSyncRunnable implements DistExecutor.SafeRunnable {
        private final List<String> defaultUnlockedItems;
        private final List<String> excludedKeywords;
        private final String serverIdentifier;

        public ClientSyncRunnable(List<String> defaultUnlockedItems, List<String> excludedKeywords, String serverIdentifier) {
            this.defaultUnlockedItems = defaultUnlockedItems;
            this.excludedKeywords = excludedKeywords;
            this.serverIdentifier = serverIdentifier;
        }

        @Override
        public void run() {
            syncBlueprintConfigOnClient(defaultUnlockedItems, excludedKeywords, serverIdentifier);
        }
    }

    // 客户端方法
    @OnlyIn(Dist.CLIENT)
    private static void syncBlueprintConfigOnClient(List<String> defaultUnlockedItems, List<String> excludedKeywords, String serverIdentifier) {
        try {
            BlueprintConfigManager manager = BlueprintConfigManager.getInstance();

            // 设置服务端标识
            if (serverIdentifier != null && !serverIdentifier.isEmpty()) {
                manager.setServerIdentifier(serverIdentifier);
            } else {
                manager.setSinglePlayerMode();
            }

            // 创建并设置配置
            BlueprintConfig config = createConfigFromLists(defaultUnlockedItems, excludedKeywords);

            // 保存配置到客户端本地文件
            manager.saveConfig();

            EconomySystem.LOGGER.info("客户端成功同步蓝图配置，服务端标识: {}", serverIdentifier);

        } catch (Exception e) {
            EconomySystem.LOGGER.error("客户端同步蓝图配置失败", e);
        }
    }

    // 辅助方法：从列表创建配置
    private static BlueprintConfig createConfigFromLists(List<String> defaultUnlockedItems, List<String> excludedKeywords) {
        BlueprintConfig config = new BlueprintConfig();
        for (String item : defaultUnlockedItems) {
            config.addDefaultUnlockedItem(item);
        }
        for (String keyword : excludedKeywords) {
            config.addExcludedKeyword(keyword);
        }
        return config;
    }
}