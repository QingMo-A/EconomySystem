package com.mo.economy_system.network.packets.playerattribute_system.strength_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import com.mo.economy_system.core.playerattributes_system.strength.PlayerStrengthManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

import static com.mo.economy_system.core.playerattributes_system.strength.PlayerStrengthManager.MIN_RESPRINT_STRENGTH;

public class Packet_SprintKeyPress {
    public Packet_SprintKeyPress() {}

    public static void encode(Packet_SprintKeyPress packet, FriendlyByteBuf buf) {}

    public static Packet_SprintKeyPress decode(FriendlyByteBuf buf) {
        return new Packet_SprintKeyPress();
    }

    public static void handle(Packet_SprintKeyPress packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.isAlive()) {
                context.setPacketHandled(true);
                return;
            }

            UUID uuid = player.getUUID();
            PlayerAttributesData attrData = PlayerAttributesDataManager.getPlayerAttributesData(uuid);
            if (attrData == null) {
                context.setPacketHandled(true);
                return;
            }

            int currentStrength = attrData.getCurrentStrength();
            boolean isExhausted = PlayerStrengthManager.IS_STRENGTH_EXHAUSTED.getOrDefault(uuid, false);

            if (isExhausted && currentStrength < MIN_RESPRINT_STRENGTH) {
                player.setSprinting(false);

                net.minecraft.network.Connection realConnection = player.connection.connection;
                EconomySystem_NetworkManager.INSTANCE.sendTo(
                        new Packet_CantRun(),
                        realConnection,
                        NetworkDirection.PLAY_TO_CLIENT
                );
            } else {
                player.setSprinting(true);
            }
        });
        context.setPacketHandled(true);
    }
}