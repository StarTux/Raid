package com.cavetale.raid;

import java.util.List;
import lombok.Data;

/**
 * JSONable.
 */
@Data
public final class Escort implements ShortInfo {
    private String name;
    private Place place = null;
    private List<String> dialogue;
    private boolean disappear;

    @Override
    public String getShortInfo() {
        return name
            + (place == null ? "" : " place=" + place.getShortInfo())
            + (dialogue == null ? "" : " dialogue=" + dialogue.size())
            + (disappear ? " disappear!" : "");
    }
}
