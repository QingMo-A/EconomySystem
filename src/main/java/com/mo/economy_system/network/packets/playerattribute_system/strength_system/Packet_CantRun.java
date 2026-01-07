package com.mo.economy_system.network.packets.playerattribute_system.strength_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.strength.PlayerStrengthManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class Packet_CantRun {
    public Packet_CantRun() {}
    public static void encode(Packet_CantRun packet, FriendlyByteBuf buf) {}
    public static Packet_CantRun decode(FriendlyByteBuf buf) {
        return new Packet_CantRun();
    }

    public static void handle(Packet_CantRun packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {

            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return;

                // 强制停止客户端疾跑
                mc.player.setSprinting(false);

                // 设置客户端耗尽标记
                PlayerStrengthManager.IS_STRENGTH_EXHAUSTED_CLIENT.put(mc.player.getUUID(), true);

                // 显示提示消息
                mc.player.displayClientMessage(
                        Component.literal("§c老己~，跑不动啦歇会儿吧，休息就能恢复体力啦❤"),
                        true
                );
            });
        });
        context.setPacketHandled(true);
    }
}