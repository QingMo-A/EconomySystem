package com.mo.economy_system.network.packets.npc_system;

import com.mo.economy_system.core.npc_system.NpcDialogueViewData;
import com.mo.economy_system.screen.npc_system.Screen_NpcDialogue;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Packet_OpenNpcDialogueGUI {
    private final NpcDialogueViewData viewData;

    public Packet_OpenNpcDialogueGUI(NpcDialogueViewData viewData) {
        this.viewData = viewData;
    }

    public static void encode(Packet_OpenNpcDialogueGUI packet, FriendlyByteBuf buf) {
        NpcDialogueViewData data = packet.viewData;
        buf.writeVarInt(data.getNpcId());
        buf.writeVarInt(data.getEntityId());
        buf.writeUtf(data.getNpcName(), 256);
        buf.writeUtf(data.getNpcIntroduction(), Short.MAX_VALUE);
        buf.writeUtf(data.getNpcGender(), 128);
        buf.writeUtf(data.getNpcProfession(), 128);
        buf.writeVarInt(data.getStoryStageId());
        buf.writeVarInt(data.getDialogues().size());
        for (String dialogue : data.getDialogues()) {
            buf.writeUtf(dialogue, Short.MAX_VALUE);
        }
        buf.writeUtf(data.getThoughtText(), Short.MAX_VALUE);
        buf.writeUtf(data.getWantedItemId(), 256);
        buf.writeVarInt(data.getFavorability());
        buf.writeUtf(data.getRelationName(), 128);
        buf.writeVarInt(data.getAvailableActions().size());
        for (String action : data.getAvailableActions()) {
            buf.writeUtf(action, 128);
        }
    }

    public static Packet_OpenNpcDialogueGUI decode(FriendlyByteBuf buf) {
        int npcId = buf.readVarInt();
        int entityId = buf.readVarInt();
        String npcName = buf.readUtf(256);
        String npcIntroduction = buf.readUtf(Short.MAX_VALUE);
        String npcGender = buf.readUtf(128);
        String npcProfession = buf.readUtf(128);
        int storyStageId = buf.readVarInt();
        int dialogueSize = buf.readVarInt();
        List<String> dialogues = new ArrayList<>();
        for (int i = 0; i < dialogueSize; i++) {
            dialogues.add(buf.readUtf(Short.MAX_VALUE));
        }
        String thoughtText = buf.readUtf(Short.MAX_VALUE);
        String wantedItemId = buf.readUtf(256);
        int favorability = buf.readVarInt();
        String relationName = buf.readUtf(128);
        int actionSize = buf.readVarInt();
        List<String> actions = new ArrayList<>();
        for (int i = 0; i < actionSize; i++) {
            actions.add(buf.readUtf(128));
        }

        return new Packet_OpenNpcDialogueGUI(new NpcDialogueViewData(
                npcId,
                entityId,
                npcName,
                npcIntroduction,
                npcGender,
                npcProfession,
                storyStageId,
                dialogues,
                thoughtText,
                wantedItemId,
                favorability,
                relationName,
                actions
        ));
    }

    public static void handle(Packet_OpenNpcDialogueGUI packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (FMLLoader.getDist().isClient()) {
            context.enqueueWork(() -> handleClient(packet));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(Packet_OpenNpcDialogueGUI packet) {
        Minecraft.getInstance().setScreen(new Screen_NpcDialogue(packet.viewData));
    }
}
