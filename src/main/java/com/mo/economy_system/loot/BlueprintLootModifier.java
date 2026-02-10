package com.mo.economy_system.loot;

import com.mo.economy_system.core.blueprint_system.PlayerBlueprintData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;

public class BlueprintLootModifier extends LootModifier {
    public static final Codec<BlueprintLootModifier> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance)
        .and(Codec.FLOAT.fieldOf("chance").forGetter(modifier -> modifier.chance))
        .and(Codec.BOOL.optionalFieldOf("only_hostile", false).forGetter(modifier -> modifier.onlyHostile))
        .and(Codec.BOOL.optionalFieldOf("only_chests", false).forGetter(modifier -> modifier.onlyChests))
        .apply(instance, BlueprintLootModifier::new));

    private final float chance;
    private final boolean onlyHostile;
    private final boolean onlyChests;

    public BlueprintLootModifier(LootItemCondition[] conditions, float chance, boolean onlyHostile, boolean onlyChests) {
        super(conditions);
        this.chance = chance;
        this.onlyHostile = onlyHostile;
        this.onlyChests = onlyChests;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() > this.chance) {
            return generatedLoot;
        }

        if (onlyHostile) {
            Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
            if (!(entity instanceof Monster)) {
                return generatedLoot;
            }
        }

        if (onlyChests) {
            Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
            BlockState blockState = context.getParamOrNull(LootContextParams.BLOCK_STATE);
            if (entity != null || blockState != null) {
                return generatedLoot;
            }
        }

        ItemStack blueprint = PlayerBlueprintData.createRandomBlueprint(context.getRandom());
        if (!blueprint.isEmpty()) {
            generatedLoot.add(blueprint);
        }

        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
