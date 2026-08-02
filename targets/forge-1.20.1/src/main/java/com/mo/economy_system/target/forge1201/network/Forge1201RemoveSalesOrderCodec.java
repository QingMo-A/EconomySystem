package com.mo.economy_system.target.forge1201.network;
import com.mo.economy_system.common.network.RemoveSalesOrderMessage;
import net.minecraft.network.FriendlyByteBuf;
final class Forge1201RemoveSalesOrderCodec {static void encode(RemoveSalesOrderMessage m,FriendlyByteBuf b){b.writeUUID(m.tradeId());}static RemoveSalesOrderMessage decode(FriendlyByteBuf b){return new RemoveSalesOrderMessage(b.readUUID());}}
