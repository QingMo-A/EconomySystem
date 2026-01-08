package com.mo.economy_system.network.packets.auth_system;

import com.mo.economy_system.core.auth_system.network.ClientAuthHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端发送给客户端的认证挑战包
 * 要求客户端进行登录
 */
public class Packet_AuthChallenge {
    private final boolean requireRegistration;

    public Packet_AuthChallenge(boolean requireRegistration) {
        this.requireRegistration = requireRegistration;
    }

    public static Packet_AuthChallenge decode(FriendlyByteBuf buffer) {
        return new Packet_AuthChallenge(buffer.readBoolean());
    }

    public static void encode(Packet_AuthChallenge packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.requireRegistration);
    }

    public static void handle(Packet_AuthChallenge packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 添加日志
            com.mo.economy_system.EconomySystem.LOGGER.info("客户端收到认证挑战，需要注册: " + packet.requireRegistration);

            // 在客户端处理认证挑战
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> ClientAuthHandler.handleAuthChallenge(packet.requireRegistration)
            );
        });
        context.setPacketHandled(true);
    }

    public boolean isRequireRegistration() {
        return requireRegistration;
    }
}
