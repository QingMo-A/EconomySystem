package com.mo.economy_system.server.chattitle;

public class Title {
    private final int TitleID;
    private final String TitleName;

    public Title(int TitleID, String TitleName) {
        this.TitleID = TitleID;
        this.TitleName = TitleName;
    }
    public int getTitleID() {
        return TitleID;
    }
    public String getTitleName() {
        return TitleName;
    }
}