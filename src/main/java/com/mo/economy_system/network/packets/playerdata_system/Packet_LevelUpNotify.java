package com.mo.economy_system.network.packets.playerdata_system;

import com.mo.economy_system.screen.server_screen.tips.TipDisplayManager;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * 等级升级提示网络包（服务端→客户端）
 * 携带升级后的新等级，用于客户端渲染提示
 */
public class Packet_LevelUpNotify implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_LevelUpNotify> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "playerdata_system/packet_level_up_notify"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_LevelUpNotify> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_LevelUpNotify.encode(packet, buf), Packet_LevelUpNotify::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final int newLevel; // 升级后的新等级

    // 构造方法
    public Packet_LevelUpNotify(int newLevel) {
        this.newLevel = newLevel;
    }

    // 编码（序列化：将数据写入字节流）
    public static void encode(Packet_LevelUpNotify packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.newLevel);
    }

    // 解码（反序列化：从字节流读取数据）
    public static Packet_LevelUpNotify decode(FriendlyByteBuf buf) {
        int newLevel = buf.readInt();
        return new Packet_LevelUpNotify(newLevel);
    }

    // 处理包（客户端执行：收到通知后，显示升级提示）
    public static void handle(Packet_LevelUpNotify packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 仅在客户端执行，调用提示管理器显示文字
            // 自定义提示文本，可携带新等级
            String tipText = "§6您的等级提升了！\n" + "§b当前等级：" + packet.newLevel + "，您的属性增加了";
            TipDisplayManager.addMessage(tipText, 8000); // 调用你已有的提示管理器
        });
    }

    // Getter（可选，若客户端需要获取新等级）
    public int getNewLevel() {
        return newLevel;
    }
}
