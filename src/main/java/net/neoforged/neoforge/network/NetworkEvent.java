package net.neoforged.neoforge.network;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class NetworkEvent {
    private NetworkEvent() {}

    public static class Context {
        @Nullable
        private final ServerPlayer sender;

        public Context(@Nullable ServerPlayer sender) {
            this.sender = sender;
        }

        public void enqueueWork(Runnable runnable) {
            runnable.run();
        }

        @Nullable
        public ServerPlayer getSender() {
            return sender;
        }

        public NetworkDirection getDirection() {
            return sender == null ? NetworkDirection.PLAY_TO_CLIENT : NetworkDirection.PLAY_TO_SERVER;
        }

        public void setPacketHandled(boolean handled) {
        }
    }
}
