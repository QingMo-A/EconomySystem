package com.mo.economy_system.datagen.lang;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;

public class EnUsLanguageProvider extends LanguageProvider {
    public EnUsLanguageProvider(DataGenerator gen, String modid) {
        super(gen.getPackOutput(), modid, "en_us");
    }

    @Override
    protected void addTranslations() {
        // 添加物品/方块/实体的翻译
        add("item.modid.example_item", "Example Item");
        add("block.modid.example_block", "Example Block");
        add("itemGroup.modid.creative_tab", "My Creative Tab");

        // 基因复苏药剂
        add("item.economy_system.restore_uninfected_potion", "Gene Resurgence Potion");
    }
}
