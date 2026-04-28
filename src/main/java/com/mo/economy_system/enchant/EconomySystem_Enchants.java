package com.mo.economy_system.enchant;

import net.neoforged.bus.api.IEventBus;

public class EconomySystem_Enchants {
    public static void register(IEventBus eventBus) {
        // Enchantments are data-driven in 1.21.1. Keep this hook so the old
        // 1:1 project structure can stay in place while JSON definitions carry
        // the registrations.
    }
}
