package com.mo.economy_system.init;

import com.mo.economy_system.EconomySystem;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;

public class Init {
    public static final String CONFIG_FOLDER_PATH = FMLPaths.CONFIGDIR.get().toFile() + File.separator + EconomySystem.MODID;

    public Init() {
        File dir = new File(CONFIG_FOLDER_PATH);

        // 创建目录（包括所有不存在的父目录）
        dir.mkdirs();
    }
}
