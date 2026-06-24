package wearweather.wearweather_server.application.gemini;

import java.util.ArrayList;
import java.util.List;

public record OutfitSelection(
        String description,
        List<Long> topIds,
        Long bottomId,
        Long outerId,
        Long shoesId,
        List<Long> accIds,
        Long bagId
) {
    public OutfitSelection {
        topIds = topIds == null ? List.of() : List.copyOf(topIds);
        accIds = accIds == null ? List.of() : List.copyOf(accIds);
    }

    public List<Long> allIds() {
        List<Long> ids = new ArrayList<>(topIds);
        add(ids, bottomId);
        add(ids, outerId);
        add(ids, shoesId);
        ids.addAll(accIds);
        add(ids, bagId);
        return List.copyOf(ids);
    }

    private void add(List<Long> ids, Long id) {
        if (id != null) ids.add(id);
    }
}
