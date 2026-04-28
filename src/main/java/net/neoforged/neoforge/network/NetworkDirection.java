package net.neoforged.neoforge.network;

import net.neoforged.fml.LogicalSide;

public enum NetworkDirection {
    PLAY_TO_CLIENT,
    PLAY_TO_SERVER;

    public LogicalSide getReceptionSide() {
        return this == PLAY_TO_CLIENT ? LogicalSide.CLIENT : LogicalSide.SERVER;
    }
}
