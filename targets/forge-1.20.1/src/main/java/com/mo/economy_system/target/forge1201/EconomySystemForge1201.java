package com.mo.economy_system.target.forge1201;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.target.forge1201.network.Forge1201NetworkChannel;
import net.minecraftforge.fml.common.Mod;
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
        EconomyServices.init(new Forge1201Platform());
        Forge1201NetworkChannel.register();
        LOGGER.info("EconomySystem Forge 1.20.1 bridge target initialized");
    }
}
