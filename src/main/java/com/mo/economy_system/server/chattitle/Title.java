package com.mo.economy_system.server.chattitle;

public class Title {
    private int TitleID;
    private String TitleName;
    private int color; // 称号颜色（RGB格式，如0xFFFFFF）

    // 无参构造函数（Gson反序列化需要）
    public Title() {
    }

    public Title(int TitleID, String TitleName, int color) {
        this.TitleID = TitleID;
        this.TitleName = TitleName;
        this.color = color;
    }
    public int getTitleID() {
        return TitleID;
    }
    public String getTitleName() {
        return TitleName;
    }
    public int getColor() {
        return color;
    }
}