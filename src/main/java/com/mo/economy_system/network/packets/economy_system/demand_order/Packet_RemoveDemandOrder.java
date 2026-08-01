package com.mo.economy_system.network.packets.economy_system.demand_order;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.market.*;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.MarketInvalidationBroadcaster;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.utils.Util_Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** Legacy NeoForge protocol 16 payload retained until formal migration. */
public class Packet_RemoveDemandOrder implements net.minecraft.network.protocol.common.custom.CustomPacketPayload, EconomyNetworkMessage {
    public static final Type<Packet_RemoveDemandOrder> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID,"economy_system/demand_order/packet_remove_demand_order"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf,Packet_RemoveDemandOrder> STREAM_CODEC=net.minecraft.network.codec.StreamCodec.of((buf,packet)->encode(packet,buf),Packet_RemoveDemandOrder::decode);
    private final UUID itemId;
    public Packet_RemoveDemandOrder(UUID itemId){this.itemId=itemId;}
    @Override public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type(){return TYPE;}
    public static void encode(Packet_RemoveDemandOrder msg,FriendlyByteBuf buf){buf.writeUUID(msg.itemId);}
    public static Packet_RemoveDemandOrder decode(FriendlyByteBuf buf){return new Packet_RemoveDemandOrder(buf.readUUID());}

    public static void handle(Packet_RemoveDemandOrder msg,IPayloadContext context){context.enqueueWork(()->{
        if(!(context.player() instanceof ServerPlayer player))return;
        EconomySavedData accounts=EconomySavedData.getInstance(player.serverLevel());
        MarketSavedData market=MarketSavedData.getInstance(player.serverLevel());
        CancelDemandOrderResult result=CancelDemandOrderService.execute(msg.itemId,new CancelDemandOrderService.Context(
                player.getUUID(),Util_Player.isOP(player),new AccountAdapter(accounts),new RepositoryAdapter(market),reporter(player)));
        player.sendSystemMessage(Component.translatable(CancelDemandOrderFeedback.messageKey(result)));
        if(result==CancelDemandOrderResult.SUCCESS)MarketInvalidationBroadcaster.broadcast(player);
    });}

    private static CancelDemandOrderService.FailureReporter reporter(ServerPlayer player){return(id,result,refund,restore,cause)->
            EconomySystem.LOGGER.error("Demand cancellation failed actor={} order={} result={} refund={} restore={}",player.getUUID(),id,result,refund,restore,cause);}
    private record AccountAdapter(EconomySavedData data) implements CancelDemandOrderService.Account{
        public boolean canCreditExact(UUID owner,int amount){return data.canCreditExact(owner,amount);}
        public BalanceMutationResult creditExact(UUID owner,int amount){return data.creditExact(owner,amount,"市场","取消求购单退款");}}
    private record RepositoryAdapter(MarketSavedData data) implements CancelDemandOrderService.Repository{
        public MarketOrder find(UUID id){return data.getOrder(id);}
        public DemandOrderRemovalResult removeUndeliveredDemand(UUID id){return data.removeUndeliveredDemand(id);}}
}
