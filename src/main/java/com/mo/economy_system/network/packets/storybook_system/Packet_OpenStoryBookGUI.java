package com.mo.economy_system.network.packets.storybook_system;

import com.mo.economy_system.core.storybook_system.StoryBookEntryViewData;
import com.mo.economy_system.screen.storybook_system.Screen_StoryBookCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class Packet_OpenStoryBookGUI implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_OpenStoryBookGUI> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "storybook_system/packet_open_story_book_gui"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_OpenStoryBookGUI> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_OpenStoryBookGUI.encode(packet, buf), Packet_OpenStoryBookGUI::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final List<StoryBookEntryViewData> entries;

    public Packet_OpenStoryBookGUI(List<StoryBookEntryViewData> entries) {
        this.entries = entries;
    }

    public static void encode(Packet_OpenStoryBookGUI packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entries.size());
        for (StoryBookEntryViewData entry : packet.entries) {
            buf.writeVarInt(entry.getFragmentId());
            buf.writeVarInt(entry.getStageId());
            buf.writeVarInt(entry.getChapterId());
            buf.writeUtf(entry.getTitle(), Short.MAX_VALUE);
            buf.writeUtf(entry.getContent(), Short.MAX_VALUE);
            buf.writeUtf(entry.getTime(), 256);
            buf.writeUtf(entry.getAuthorName(), 256);
            buf.writeBoolean(entry.isRead());
        }
    }

    public static Packet_OpenStoryBookGUI decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<StoryBookEntryViewData> entries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            entries.add(new StoryBookEntryViewData(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readUtf(Short.MAX_VALUE),
                    buf.readUtf(Short.MAX_VALUE),
                    buf.readUtf(256),
                    buf.readUtf(256),
                    buf.readBoolean()
            ));
        }
        return new Packet_OpenStoryBookGUI(entries);
    }

    public static void handle(Packet_OpenStoryBookGUI packet, IPayloadContext context) {
        if (FMLLoader.getDist().isClient()) {
            context.enqueueWork(() -> handleClient(packet));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(Packet_OpenStoryBookGUI packet) {
        Minecraft.getInstance().setScreen(new Screen_StoryBookCatalog(packet.entries));
    }
}
