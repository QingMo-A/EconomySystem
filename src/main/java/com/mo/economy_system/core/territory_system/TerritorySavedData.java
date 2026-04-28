package com.mo.economy_system.core.territory_system;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class TerritorySavedData extends SavedData {

    private static final String DATA_NAME = "territory_data";
    private final Map<UUID, Territory> territoryByID = new HashMap<>();

    public static TerritorySavedData load(CompoundTag nbt, HolderLookup.Provider registries) {
        TerritorySavedData data = new TerritorySavedData();
        ListTag territoriesTag = nbt.getList("Territories", Tag.TAG_COMPOUND);

        for (Tag tag : territoriesTag) {
            CompoundTag territoryTag = (CompoundTag) tag;
            Territory territory = Territory.fromNBT(territoryTag);
            data.territoryByID.put(territory.getTerritoryID(), territory);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        ListTag territoriesTag = new ListTag();
        for (Territory territory : territoryByID.values()) {
            territoriesTag.add(territory.toNBT());
        }
        nbt.put("Territories", territoriesTag);
        return nbt;
    }

    public static TerritorySavedData getInstance(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(TerritorySavedData::new, TerritorySavedData::load), DATA_NAME);
    }

    public void addTerritory(Territory territory) {
        territoryByID.put(territory.getTerritoryID(), territory);
        setDirty();
    }

    public void removeTerritory(UUID territoryID) {
        territoryByID.remove(territoryID);
        setDirty();
    }

    public Collection<Territory> getAllTerritories() {
        return territoryByID.values();
    }

    public Territory getTerritoryByID(UUID territoryID) {
        return territoryByID.get(territoryID);
    }
}
