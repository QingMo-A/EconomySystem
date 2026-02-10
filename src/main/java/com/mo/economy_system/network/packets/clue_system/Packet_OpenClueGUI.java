package com.mo.economy_system.network.packets.clue_system;

import com.mo.economy_system.core.clue_system.Screen_Clue;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 打开线索 GUI 数据包
 * 服务端发送到客户端，携带线索数据
 */
public class Packet_OpenClueGUI {

    private final String title;
    private final String content;
    private final String time;
    private final String author;
    private final int stage;

    public Packet_OpenClueGUI(String title, String content, String time, String author, int stage) {
        this.title = title;
        this.content = content;
        this.time = time;
        this.author = author;
        this.stage = stage;
    }

    public Packet_OpenClueGUI(com.mo.economy_system.core.clue_system.ClueData clueData) {
        this(clueData.getClueTitle(), clueData.getClueContent(), clueData.getClueTime(), clueData.getClueAuthor(), clueData.getClueStage());
    }

    public static void encode(Packet_OpenClueGUI packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.title, Short.MAX_VALUE);
        buf.writeUtf(packet.content, Short.MAX_VALUE);
        buf.writeUtf(packet.time, 256);
        buf.writeUtf(packet.author, 256);
        buf.writeVarInt(packet.stage);
    }

    public static Packet_OpenClueGUI decode(FriendlyByteBuf buf) {
        String title = buf.readUtf(Short.MAX_VALUE);
        String content = buf.readUtf(Short.MAX_VALUE);
        String time = buf.readUtf(256);
        String author = buf.readUtf(256);
        int stage = buf.readVarInt();
        return new Packet_OpenClueGUI(title, content, time, author, stage);
    }

    public static void handle(Packet_OpenClueGUI packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        if (FMLLoader.getDist().isClient()) {
            context.enqueueWork(() -> {
                handleClient(packet);
            });
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(Packet_OpenClueGUI packet) {
        Minecraft.getInstance().setScreen(new Screen_Clue(
                packet.title,
                packet.content,
                packet.time,
                packet.author,
                packet.stage
        ));
    }
}
