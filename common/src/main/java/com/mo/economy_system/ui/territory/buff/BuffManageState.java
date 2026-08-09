package com.mo.economy_system.ui.territory.buff;

import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record BuffManageState(
        UUID territoryId, String territoryName, List<BuffRow> buffs,
        int page, int pageSize, int scrollOffset, String filter,
        ScreenState screenState, String errorKey, long requestId) {
    public BuffManageState {
        Objects.requireNonNull(territoryId, "territoryId");
        if (territoryName == null || territoryName.isBlank()) throw new IllegalArgumentException("territoryName");
        if (page < 0 || pageSize < 1 || scrollOffset < 0 || requestId < -1) throw new IllegalArgumentException("pagination");
        buffs = List.copyOf(Objects.requireNonNull(buffs, "buffs"));
        filter = Objects.requireNonNullElse(filter, "");
        screenState = Objects.requireNonNull(screenState, "screenState");
    }

    public List<BuffRow> filteredBuffs() {
        String needle = filter.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) return buffs;
        return buffs.stream().filter(row -> row.buff().id().toLowerCase(Locale.ROOT).contains(needle)
                || row.buff().displayText().toLowerCase(Locale.ROOT).contains(needle)
                || row.buff().effectId().toLowerCase(Locale.ROOT).contains(needle)).toList();
    }

    public int totalPages() {
        return Math.max(1, (filteredBuffs().size() + pageSize - 1) / pageSize);
    }

    public List<BuffRow> visibleBuffs() {
        List<BuffRow> values = filteredBuffs();
        int start = Math.min(page * pageSize, values.size());
        return values.subList(start, Math.min(values.size(), start + pageSize));
    }
}
