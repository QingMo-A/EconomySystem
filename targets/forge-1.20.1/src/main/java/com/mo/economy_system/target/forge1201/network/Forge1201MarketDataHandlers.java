package com.mo.economy_system.target.forge1201.network;
import com.mo.economy_system.common.client.ClientMarketState;
import com.mo.economy_system.common.market.MarketDataQueryService;
import com.mo.economy_system.common.network.*;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
final class Forge1201MarketDataHandlers {
 static void request(MarketDataRequestMessage m,Supplier<NetworkEvent.Context> s){NetworkEvent.Context c=s.get();ServerPlayer p=c.getSender();if(p!=null){var data=MarketSavedData.getInstance(p.serverLevel());Forge1201NetworkChannel.sendToPlayer(p,MarketDataQueryService.query(data.getView(),p.getUUID(),m));}c.setPacketHandled(true);}
 static void response(MarketDataResponseMessage m,Supplier<NetworkEvent.Context> s){ClientMarketState.apply(m);s.get().setPacketHandled(true);}
}
