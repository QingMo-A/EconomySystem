package com.mo.economy_system.core.territory_system;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InviteManager {
    private static final Map<UUID, Invite> invites = new HashMap<>();

    public static void sendInvite(UUID senderUUID, UUID receiverUUID, UUID territoryID) {
        invites.put(receiverUUID, new Invite(senderUUID, receiverUUID, territoryID));
    }

    public static Invite getInvite(UUID receiverUUID) {
        return invites.get(receiverUUID);
    }

    public static void removeInvite(UUID receiverUUID) {
        invites.remove(receiverUUID);
    }

    public static class Invite {
        private final UUID senderUUID;
        private final UUID receiverUUID;
        private final UUID territoryID;

        public Invite(UUID senderUUID, UUID receiverUUID, UUID territoryID) {
            this.senderUUID = senderUUID;
            this.receiverUUID = receiverUUID;
            this.territoryID = territoryID;
        }

        public UUID getSenderUUID() {
            return senderUUID;
        }

        public UUID getReceiverUUID() {
            return receiverUUID;
        }

        public UUID getTerritoryID() {
            return territoryID;
        }
    }
}

