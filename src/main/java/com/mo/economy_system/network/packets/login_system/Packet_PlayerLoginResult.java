package com.mo.economy_system.network.packets.login_system;

import com.mo.economy_system.core.login_system.Screen_LoginUI;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;
import com.mo.economy_system.compat.network.NetworkEvent;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Supplier;

/**
 * 登录结果包（S→C）
 * 服务端返回登录/注册结果给客户端
 */
public class Packet_PlayerLoginResult {
    private final boolean success;
    private final String message;

    public Packet_PlayerLoginResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public static void encode(@UnknownNullability Packet_PlayerLoginResult msg, FriendlyByteBuf buffer) {
        buffer.writeBoolean(msg.success);
        buffer.writeUtf(msg.message);
    }

    public static Packet_PlayerLoginResult decode(FriendlyByteBuf buffer) {
        boolean success = buffer.readBoolean();
        String message = buffer.readUtf();
        return new Packet_PlayerLoginResult(success, message);
    }

    public static void handle(Packet_PlayerLoginResult msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        // 只在客户端执行UI逻辑
        if (FMLLoader.getDist().isClient()) {
            context.enqueueWork(() -> {
                handleClient(msg);
            });
        }

        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(Packet_PlayerLoginResult msg) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof Screen_LoginUI loginScreen) {
            if (msg.isSuccess()) {
                minecraft.setScreen(null);
            } else {
                loginScreen.setStatusMessage(msg.getMessage(), true);
            }
        }
    }
}
