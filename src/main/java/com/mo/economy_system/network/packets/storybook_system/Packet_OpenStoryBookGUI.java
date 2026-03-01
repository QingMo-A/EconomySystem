package com.mo.economy_system.network.packets.storybook_system;

import com.mo.economy_system.core.storybook_system.StoryBookEntryViewData;
import com.mo.economy_system.screen.storybook_system.Screen_StoryBookCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Packet_OpenStoryBookGUI {
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

    public static void handle(Packet_OpenStoryBookGUI packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (FMLLoader.getDist().isClient()) {
            context.enqueueWork(() -> handleClient(packet));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(Packet_OpenStoryBookGUI packet) {
        Minecraft.getInstance().setScreen(new Screen_StoryBookCatalog(packet.entries));
    }
}
