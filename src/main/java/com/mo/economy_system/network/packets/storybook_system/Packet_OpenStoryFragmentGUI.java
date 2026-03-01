package com.mo.economy_system.network.packets.storybook_system;

import com.mo.economy_system.core.storybook_system.FragmentData;
import com.mo.economy_system.screen.storybook_system.Screen_StoryFragment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class Packet_OpenStoryFragmentGUI {

    private final int fragmentId;
    private final int stageId;
    private final int chapterId;
    private final String title;
    private final String content;
    private final String time;
    private final String authorName;

    public Packet_OpenStoryFragmentGUI(FragmentData fragmentData) {
        this(
                fragmentData.getId(),
                fragmentData.getStageId(),
                fragmentData.getChapterId(),
                fragmentData.getTitle(),
                fragmentData.getContent(),
                fragmentData.getTime(),
                fragmentData.getAuthorName()
        );
    }

    public Packet_OpenStoryFragmentGUI(int fragmentId, int stageId, int chapterId, String title, String content, String time, String authorName) {
        this.fragmentId = fragmentId;
        this.stageId = stageId;
        this.chapterId = chapterId;
        this.title = title;
        this.content = content;
        this.time = time;
        this.authorName = authorName;
    }

    public static void encode(Packet_OpenStoryFragmentGUI packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.fragmentId);
        buf.writeVarInt(packet.stageId);
        buf.writeVarInt(packet.chapterId);
        buf.writeUtf(packet.title, Short.MAX_VALUE);
        buf.writeUtf(packet.content, Short.MAX_VALUE);
        buf.writeUtf(packet.time == null ? "" : packet.time, 256);
        buf.writeUtf(packet.authorName == null ? "" : packet.authorName, 256);
    }

    public static Packet_OpenStoryFragmentGUI decode(FriendlyByteBuf buf) {
        return new Packet_OpenStoryFragmentGUI(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(Short.MAX_VALUE),
                buf.readUtf(Short.MAX_VALUE),
                buf.readUtf(256),
                buf.readUtf(256)
        );
    }

    public static void handle(Packet_OpenStoryFragmentGUI packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (FMLLoader.getDist().isClient()) {
            context.enqueueWork(() -> handleClient(packet));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(Packet_OpenStoryFragmentGUI packet) {
        Minecraft.getInstance().setScreen(new Screen_StoryFragment(
                packet.fragmentId,
                packet.stageId,
                packet.chapterId,
                packet.title,
                packet.content,
                packet.time,
                packet.authorName
        ));
    }
}
