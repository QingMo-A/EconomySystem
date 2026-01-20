package com.mo.economy_system.network.packets.playerattribute_system.death_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 死亡屏幕数据包
 * 服务端 → 客户端
 * 发送当前复活点数、消耗信息和死亡位置，用于显示自定义死亡屏幕
 */
public class Packet_DeathScreenData {

    private final float respawnPoint;
    private final float normalCost;
    private final float keepInventoryCost;
    private final boolean isInfected;
    private final Component deathMessage;
    private final double deathX;
    private final double deathY;
    private final double deathZ;
    private final String dimension;

    public Packet_DeathScreenData(float respawnPoint, float normalCost, float keepInventoryCost,
                                  boolean isInfected, Component deathMessage,
                                  double deathX, double deathY, double deathZ, String dimension) {
        this.respawnPoint = respawnPoint;
        this.normalCost = normalCost;
        this.keepInventoryCost = keepInventoryCost;
        this.isInfected = isInfected;
        this.deathMessage = deathMessage;
        this.deathX = deathX;
        this.deathY = deathY;
        this.deathZ = deathZ;
        this.dimension = dimension;
    }

    /**
     * 编码
     */
    public static void encode(Packet_DeathScreenData packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.respawnPoint);
        buf.writeFloat(packet.normalCost);
        buf.writeFloat(packet.keepInventoryCost);
        buf.writeBoolean(packet.isInfected);
        buf.writeComponent(packet.deathMessage);
        buf.writeDouble(packet.deathX);
        buf.writeDouble(packet.deathY);
        buf.writeDouble(packet.deathZ);
        buf.writeUtf(packet.dimension);
    }

    /**
     * 解码
     */
    public static Packet_DeathScreenData decode(FriendlyByteBuf buf) {
        float respawnPoint = buf.readFloat();
        float normalCost = buf.readFloat();
        float keepInventoryCost = buf.readFloat();
        boolean isInfected = buf.readBoolean();
        Component deathMessage = buf.readComponent();
        double deathX = buf.readDouble();
        double deathY = buf.readDouble();
        double deathZ = buf.readDouble();
        String dimension = buf.readUtf();
        return new Packet_DeathScreenData(respawnPoint, normalCost, keepInventoryCost, isInfected, deathMessage, deathX, deathY, deathZ, dimension);
    }

    /**
     * 处理（客户端）
     */
    public static void handle(Packet_DeathScreenData packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet));
        });
        context.setPacketHandled(true);
    }

    /**
     * 客户端处理器 - 只有客户端会加载这个内部类
     */
    @OnlyIn(Dist.CLIENT)
    private static class ClientHandler {
        static void handle(Packet_DeathScreenData packet) {
            EconomySystem.LOGGER.info("客户端收到死亡屏幕数据: 复活点={}, 正常={}, 保留={}",
                packet.respawnPoint, packet.normalCost, packet.keepInventoryCost);

            // 存储死亡屏幕数据，供 DeathScreenMixin 使用
            com.mo.economy_system.core.playerattributes_system.death.DeathScreenDataStorage.setData(
                packet.respawnPoint,
                packet.normalCost,
                packet.keepInventoryCost,
                packet.isInfected,
                packet.deathMessage,
                packet.deathX,
                packet.deathY,
                packet.deathZ,
                packet.dimension
            );

            EconomySystem.LOGGER.info("死亡屏幕数据已存储");
        }
    }

    public float getRespawnPoint() {
        return respawnPoint;
    }

    public float getNormalCost() {
        return normalCost;
    }

    public float getKeepInventoryCost() {
        return keepInventoryCost;
    }

    public boolean isInfected() {
        return isInfected;
    }
}
