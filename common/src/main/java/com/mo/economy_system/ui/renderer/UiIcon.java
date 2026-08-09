package com.mo.economy_system.ui.renderer;

/** Loader-neutral semantic icon mapping for the shared 64x64 texture authority. */
public enum UiIcon {
    TERRITORY("territory"),
    HOME("home"),
    SHOP("shop"),
    MARKET("market"),
    DELIVERY("delivery"),
    ABOUT("about"),
    TRADE("trade"),
    LEADERBOARD("leaderboard"),
    BALANCE("balance"),
    MEMBER("authorized"),
    OWNER("owner"),
    MANAGE("manage"),
    ARROW_LEFT("arrow_left"),
    ARROW_RIGHT("arrow_right"),
    RETRY("manage"),
    BACK("arrow_left"),
    BUFF("manage"),
    OVERWORLD("overworld"),
    NETHER("nether"),
    END("end"),
    AUTHORIZED("authorized"),
    KEY("key"),
    TELEPORT("teleport");

    public static final int SOURCE_WIDTH = 64;
    public static final int SOURCE_HEIGHT = 64;

    private final String fileName;

    UiIcon(String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }

    public String resourcePath() {
        return "economy_system:textures/gui/icons/" + fileName + ".png";
    }

    public int sourceWidth() {
        return SOURCE_WIDTH;
    }

    public int sourceHeight() {
        return SOURCE_HEIGHT;
    }
}
