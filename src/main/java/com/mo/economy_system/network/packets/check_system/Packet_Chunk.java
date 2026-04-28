package com.mo.economy_system.network.packets.check_system;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * ChunkPacket 用于分块传输大文件或大字符串。
 * 其中包含：
 * - fileId: 用于标识同一个文件/传输的唯一 ID
 * - chunkIndex: 当前是第几块
 * - totalChunks: 总块数
 * - chunkData: 当前块的 Base64 文本或其他字符串数据
 */
public class Packet_Chunk {
    /**
     * 用于缓存所有正在接收的分块。
     * key = fileId (String)
     * value = 一个临时对象，用来存储所有 chunkData，直到接收完成。
     */
    public static final Map<String, ChunkAccumulator> ACCUMULATOR_MAP = new ConcurrentHashMap<>();

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final String playerName;
    private final String playerUUID;
    private final String senderName;
    private final String senderUUID;
    private final String actionType;
    private final String fileName;

    private final String fileId;
    private final int chunkIndex;
    private final int totalChunks;
    private final String chunkData;

    public Packet_Chunk(String playerName, String playerUUID, String senderName, String senderUUID, String actionType, String fileName, String fileId, int chunkIndex, int totalChunks, String chunkData) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.senderName = senderName;
        this.senderUUID = senderUUID;
        this.actionType = actionType;
        this.fileName = fileName;
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.chunkData = chunkData;
    }

    // 编码
    public static void encode(Packet_Chunk msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.playerName);
        buf.writeUtf(msg.playerUUID);
        buf.writeUtf(msg.senderName);
        buf.writeUtf(msg.senderUUID);
        buf.writeUtf(msg.actionType);
        buf.writeUtf(msg.fileName);
        buf.writeUtf(msg.fileId);
        buf.writeInt(msg.chunkIndex);
        buf.writeInt(msg.totalChunks);
        buf.writeUtf(msg.chunkData);
    }

    // 解码
    public static Packet_Chunk decode(FriendlyByteBuf buf) {
        String playerName = buf.readUtf();
        String playerUUID = buf.readUtf();
        String senderName = buf.readUtf();
        String senderUUID = buf.readUtf();
        String actionType = buf.readUtf();
        String fileName = buf.readUtf();
        String fileId = buf.readUtf();
        int idx = buf.readInt();
        int total = buf.readInt();
        String data = buf.readUtf();
        return new Packet_Chunk(playerName, playerUUID, senderName, senderUUID, actionType, fileName, fileId, idx, total, data);
    }

    // 处理
    public static void handle(Packet_Chunk msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            MinecraftServer server = player.server;
            ServerPlayer target = server.getPlayerList().getPlayer(UUID.fromString(msg.senderUUID));
            // 在服务器或者客户端都可执行分块接收逻辑。
            // 这里以服务器端接收为例，如果要客户端接收，也可类似实现。

            // 1. 找到/创建一个 ChunkAccumulator
            ChunkAccumulator accumulator = ACCUMULATOR_MAP.computeIfAbsent(
                    msg.fileId,
                    key -> new ChunkAccumulator(msg.totalChunks)
            );

            // 2. 存储当前 chunkData
            accumulator.addChunk(msg.chunkIndex, msg.chunkData);

            // 3. 检查是否全部接收完
            if (accumulator.isComplete()) {
                // 3a. 拼接所有分块
                String fullData = accumulator.buildFullData();

                int chunkSize = 30000;
                String uuid = UUID.randomUUID().toString();

                int totalChunks = (fullData.length() + chunkSize - 1) / chunkSize;
                for (int i = 0; i < totalChunks; i++) {
                    int start = i * chunkSize;
                    int end = Math.min(start + chunkSize, fullData.length());
                    String chunkData = fullData.substring(start, end);

                    // 发送 ChunkPacket
                    EconomySystem_NetworkManager.INSTANCE.send(target, new Packet_ChunkResponse(msg.playerName, msg.playerUUID, msg.senderName, msg.senderUUID, msg.actionType, msg.fileName, uuid, i, totalChunks, chunkData));
                }

                ACCUMULATOR_MAP.remove(msg.fileId);
            }
        });
        context.setPacketHandled(true);
    }

    /**
     * ChunkAccumulator 用于暂存多个分块。
     */
    public static class ChunkAccumulator {
        private final String[] chunks;
        private final int totalChunks;
        private int receivedCount = 0;

        public ChunkAccumulator(int totalChunks) {
            this.totalChunks = totalChunks;
            this.chunks = new String[totalChunks];
        }

        public void addChunk(int idx, String data) {
            // 注意判断 idx 是否越界
            if (idx >= 0 && idx < totalChunks) {
                // 如果该分块尚未收到
                if (chunks[idx] == null) {
                    chunks[idx] = data;
                    receivedCount++;
                }
            }
        }

        public boolean isComplete() {
            return receivedCount >= totalChunks;
        }

        public String buildFullData() {
            // 将所有分块按顺序拼接
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < totalChunks; i++) {
                if (chunks[i] != null) {
                    sb.append(chunks[i]);
                }
            }
            return sb.toString();
        }
    }
}
