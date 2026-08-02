package com.mo.economy_system.target.forge1201.network;
import com.mo.economy_system.common.network.RemoveDemandOrderMessage;
import net.minecraft.network.FriendlyByteBuf;
final class Forge1201RemoveDemandOrderCodec { private Forge1201RemoveDemandOrderCodec(){} static void encode(RemoveDemandOrderMessage m,FriendlyByteBuf b){b.writeUUID(m.tradeId());} static RemoveDemandOrderMessage decode(FriendlyByteBuf b){return new RemoveDemandOrderMessage(b.readUUID());} }
