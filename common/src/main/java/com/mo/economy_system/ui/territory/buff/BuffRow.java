package com.mo.economy_system.ui.territory.buff;

import com.mo.economy_system.common.territory.TerritoryBuffCost;
import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import java.util.Objects;

public record BuffRow(Buff buff, TerritoryBuffCost cost, BuffResourceSnapshot resources,
                      BuffAvailability availability) {
    public BuffRow {
        Objects.requireNonNull(buff, "buff");
        Objects.requireNonNull(cost, "cost");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(availability, "availability");
    }

    public static BuffRow inspect(Buff buff, BuffResourceSnapshot resources) {
        Objects.requireNonNull(buff, "buff");
        Objects.requireNonNull(resources, "resources");
        try {
            TerritoryBuffCost cost = TerritoryBuffCost.aggregate(buff);
            boolean missingItems = cost.items().entrySet().stream()
                    .anyMatch(item -> resources.known()
                            && resources.itemCount(item.getKey()) < item.getValue());
            boolean missingExperience = resources.known()
                    && resources.experienceLevel() < cost.experience();
            BuffAvailability availability;
            if (buff.level() >= buff.maxLevel()) availability = BuffAvailability.MAX_LEVEL;
            else if (missingItems && missingExperience) {
                availability = BuffAvailability.MISSING_ITEMS_AND_EXPERIENCE;
            } else if (missingItems) availability = BuffAvailability.MISSING_ITEMS;
            else if (missingExperience) availability = BuffAvailability.MISSING_EXPERIENCE;
            else availability = BuffAvailability.AVAILABLE;
            return new BuffRow(buff, cost, resources, availability);
        } catch (RuntimeException invalidCost) {
            return new BuffRow(buff, TerritoryBuffCost.empty(), resources,
                    BuffAvailability.INVALID_COST);
        }
    }

    public BuffAction action() {
        if (buff.level() >= buff.maxLevel()) return BuffAction.MAX;
        return buff.unlocked() ? BuffAction.UPGRADE : BuffAction.UNLOCK;
    }
}
