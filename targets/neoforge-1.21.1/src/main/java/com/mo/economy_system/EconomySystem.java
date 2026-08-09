package com.mo.economy_system;

import com.mo.economy_system.enchant.EconomySystem_Enchants;
import com.mo.economy_system.init.Init;
import com.mo.economy_system.item.EconomySystem_CreativeTabs;
import com.mo.economy_system.item.EconomySystem_Items;
import com.mo.economy_system.core.economy_system.shop.ShopConfigWatcher;
import com.mo.economy_system.core.economy_system.shop.ShopManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.sound.EconomySystem_Sounds;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.common.settings.EconomySettings;
import com.mo.economy_system.target.neoforge1211.NeoForge1211Platform;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(EconomySystem.MODID)
public class EconomySystem {
    public static final boolean isDev = true;
    public static final String MODID = EconomyConstants.MOD_ID;
    public static final Logger LOGGER = LogUtils.getLogger();

    static {
        EconomyServices.init(new NeoForge1211Platform());
    }

    public static final ShopManager SHOP_MANAGER = new ShopManager();

    public EconomySystem(IEventBus modEventBus, ModContainer modContainer) {
        EconomySettings.initialize();
        // 注册物品
        EconomySystem_Items.register(modEventBus);
        // 注册声音
        EconomySystem_Sounds.SOUND_EVENTS.register(modEventBus);
        // 注册附魔
        EconomySystem_Enchants.register(modEventBus);
        // 注册网络包
        EconomySystem_NetworkManager.register(modEventBus);
        // 注册创造物品栏
        EconomySystem_CreativeTabs.CREATIVE_TABS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        new Init();
        // 启动文件监听器
        new ShopConfigWatcher(SHOP_MANAGER).watchConfigFile();
        // 日志信息
        LOGGER.info("Economy System Mod Initialized!");
    }
}
