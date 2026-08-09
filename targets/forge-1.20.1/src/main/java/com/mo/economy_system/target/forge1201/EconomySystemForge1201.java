package com.mo.economy_system.target.forge1201;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.target.forge1201.item.Forge1201Items;
import com.mo.economy_system.target.forge1201.network.Forge1201NetworkChannel;
import com.mo.economy_system.target.forge1201.reward.Forge1201RewardEnchantments;
import com.mo.economy_system.common.settings.EconomySettings;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Forge 1.20.1 target entrypoint.
 *
 * <p>The target intentionally starts with no legacy gameplay registrations.
 * Features are enabled only after their NeoForge 1.21.1 semantics have been
 * extracted into common code and implemented by a Forge adapter.</p>
 */
@Mod(EconomySystemForge1201.MODID)
public final class EconomySystemForge1201 {
    public static final String MODID = EconomyConstants.MOD_ID;
    private static final Logger LOGGER = LogUtils.getLogger();

    public EconomySystemForge1201() {
        Forge1201Items.register(FMLJavaModLoadingContext.get().getModEventBus());
        Forge1201RewardEnchantments.register(FMLJavaModLoadingContext.get().getModEventBus());
        EconomyServices.init(new Forge1201Platform());
        EconomySettings.initialize();
        Forge1201NetworkChannel.register();
        LOGGER.info("EconomySystem Forge 1.20.1 bridge target initialized");
    }
}
