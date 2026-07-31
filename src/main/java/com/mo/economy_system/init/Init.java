package com.mo.economy_system.init;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.platform.EconomyServices;

import java.io.File;

public class Init {
    public static final String CONFIG_FOLDER_PATH = EconomyServices.platform()
            .configDirectory()
            .resolve(EconomySystem.MODID)
            .toString();

    public Init() {
        File dir = new File(CONFIG_FOLDER_PATH);

        // 创建目录（包括所有不存在的父目录）
        dir.mkdirs();
    }
}
