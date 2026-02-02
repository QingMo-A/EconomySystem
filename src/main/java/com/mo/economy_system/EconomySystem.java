package com.mo.economy_system;

import com.mo.economy_system.armor.ArmorTickHandler;
import com.mo.economy_system.armor.armors.CustomHitboxRenderer;
import com.mo.economy_system.enchant.EconomySystem_Enchants;
import com.mo.economy_system.entity.EconomySystem_Entities;
import com.mo.economy_system.init.Init;
import com.mo.economy_system.item.EconomySystem_CreativeTabs;
import com.mo.economy_system.item.EconomySystem_Items;
import com.mo.economy_system.core.economy_system.reward.RewardConfigWatcher;
import com.mo.economy_system.core.economy_system.reward.RewardManager;
import com.mo.economy_system.core.economy_system.shop.ShopConfigWatcher;
import com.mo.economy_system.core.economy_system.shop.ShopManager;
import com.mo.economy_system.core.playerattributes_system.limb_health_system.LimbDamageConfig;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.server.notice.NoticeManager;
import com.mo.economy_system.server.notice.PlayerNoticeDataManager;
import com.mo.economy_system.sound.EconomySystem_Sounds;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(EconomySystem.MODID)
public class EconomySystem {
    public static final boolean isDev = true;
    public static final String MODID = "economy_system";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final ShopManager SHOP_MANAGER = new ShopManager();
    public static final RewardManager REWARD_MANAGER = new RewardManager();

    public EconomySystem(FMLJavaModLoadingContext context) {
        // 获取 mod 事件总线
        IEventBus modEventBus = context.getModEventBus();
        // 注册客户端事件
        modEventBus.addListener(this::onClientSetup);
        // 注册物品
        EconomySystem_Items.register(modEventBus);
        // 注册声音
        EconomySystem_Sounds.SOUND_EVENTS.register(modEventBus);
        // 注册附魔
        EconomySystem_Enchants.register(modEventBus);
        // 注册网络包
        EconomySystem_NetworkManager.register();
        // 注册创造物品栏
        EconomySystem_CreativeTabs.CREATIVE_TABS.register(modEventBus);
        // 注册实体
        EconomySystem_Entities.ENTITIES.register(modEventBus);
        // 注册世界渲染事件
        // modEventBus.addListener(CustomHitboxRenderer::onRenderWorld);
        // modEventBus.addListener(ArmorTickHandler::onPlayerTick);

        new Init();
        // 启动文件监听器
        new ShopConfigWatcher(SHOP_MANAGER).watchConfigFile();
        new RewardConfigWatcher(REWARD_MANAGER).watchConfigFile();

        // 初始化公告系统
        NoticeManager.loadFromConfig();
        PlayerNoticeDataManager.init();

        // 初始化肢体伤害系统
        LimbDamageConfig.init();

        // GeckoLib.initialize();

        // 日志信息
        LOGGER.info("Economy System Mod Initialized!");
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        // 注册按键绑定的事件监听
        LOGGER.info("Registering Keybinds...");
    }
}
