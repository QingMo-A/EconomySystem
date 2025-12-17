package com.mo.economy_system.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mo.economy_system.EconomySystem;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TaskDataManager {
    private static final File TASK_DATA_FILE = new File("config/economy_system/task_data.json");
    private static final Map<UUID, TaskData> TASK_DATA_CACHE = new ConcurrentHashMap<>();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();  //创建带缩进的配置json文件

    static {
        try {
            if (!TASK_DATA_FILE.getParentFile().exists()) {
                boolean dirCreated = TASK_DATA_FILE.getParentFile().mkdirs();
                if (dirCreated) {
                    EconomySystem.LOGGER.info("任务数据目录创建成功：{}", TASK_DATA_FILE.getParentFile().getPath());
                } else {
                    EconomySystem.LOGGER.info("任务数据目录创建失败：{}", TASK_DATA_FILE.getParentFile().getPath());
                }
            }
            if (!TASK_DATA_FILE.exists()) {
                boolean fileCreated = TASK_DATA_FILE.createNewFile();
                if (fileCreated) {
                    EconomySystem.LOGGER.info("任务数据文件创建成功：{}", TASK_DATA_FILE.getPath());
                } else {
                    EconomySystem.LOGGER.error("任务数据文件创建失败：{}", TASK_DATA_FILE.getPath());
                }
            } else {
                EconomySystem.LOGGER.info("任务数据文件已存在：{}", TASK_DATA_FILE.getPath());
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("初始化任务数据文件失败", e);
        }
    }
}
