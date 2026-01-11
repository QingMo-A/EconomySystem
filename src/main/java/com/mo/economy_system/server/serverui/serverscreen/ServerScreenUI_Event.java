package com.mo.economy_system.server.serverui.serverscreen;

import com.mo.economy_system.EconomySystem;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.GuiOverlayManager;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class ServerScreenUI_Event {
    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (ServerScreenUI.isShowUI() && mc.player != null) {
            // 调用修复后的判断方法，无类型不匹配问题
            boolean isVanillaSystemOverlay = isVanillaSystemOverlay(event);
            if (isVanillaSystemOverlay) {
                event.setCanceled(true); // 仅屏蔽原版系统UI，TipsUI正常渲染
            }
        }
    }

    /**
     * 修复类型不匹配问题：仅比较同类型的IGuiOverlay实例
     * 完全使用公共API，无权限问题，无类型错误
     */
    private static boolean isVanillaSystemOverlay(RenderGuiOverlayEvent.Pre event) {
        // 定义需要屏蔽的原版UI枚举（可按需调整）
        VanillaGuiOverlay[] needHideOverlays = new VanillaGuiOverlay[]{
                VanillaGuiOverlay.PLAYER_HEALTH,    // 生命值
                VanillaGuiOverlay.FOOD_LEVEL,       // 饥饿值
                VanillaGuiOverlay.AIR_LEVEL,        // 氧气值
                VanillaGuiOverlay.ARMOR_LEVEL,      // 盔甲值
                VanillaGuiOverlay.EXPERIENCE_BAR,   // 经验条
                VanillaGuiOverlay.HOTBAR,           // 快捷栏
                VanillaGuiOverlay.CROSSHAIR         // 准星
        };

        // 获取当前渲染的UI（类型：IGuiOverlay）
        IGuiOverlay currentRenderOverlay = event.getOverlay().overlay();

        // 遍历需要屏蔽的原版UI，仅比较同类型的IGuiOverlay
        for (VanillaGuiOverlay vanillaOverlay : needHideOverlays) {
            // 1. 通过公共API获取NamedGuiOverlay（封装类）
            NamedGuiOverlay namedOverlay = GuiOverlayManager.findOverlay(vanillaOverlay.id());
            if (namedOverlay != null) {
                // 2. 提取NamedGuiOverlay中的IGuiOverlay实例（与currentRenderOverlay同类型）
                IGuiOverlay targetOverlay = namedOverlay.overlay();
                // 3. 同类型对象用==比较，无编译错误
                if (currentRenderOverlay == targetOverlay) {
                    return true; // 匹配到需要屏蔽的原版UI
                }
            }
        }

        return false; // 非原版系统UI（如TipsUI），不屏蔽
    }
}