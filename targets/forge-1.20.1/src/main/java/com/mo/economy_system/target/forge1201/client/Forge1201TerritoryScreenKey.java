package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.screen.territory_system.Screen_Territory;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = EconomyConstants.MOD_ID, value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Forge1201TerritoryScreenKey {
  private static final KeyMapping OPEN = new KeyMapping(
      "key.economy_system.open_screen", GLFW.GLFW_KEY_I, "key.categories.economy_system");
  private Forge1201TerritoryScreenKey() {}

  @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(OPEN); }

  @Mod.EventBusSubscriber(modid = EconomyConstants.MOD_ID, value = Dist.CLIENT)
  public static final class Input {
    private Input() {}
    @SubscribeEvent public static void key(InputEvent.Key event) {
      if (OPEN.consumeClick()) Minecraft.getInstance().setScreen(new Screen_Territory());
    }
  }
}
