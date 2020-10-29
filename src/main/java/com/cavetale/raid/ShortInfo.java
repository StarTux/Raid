package com.cavetale.raid;

public interface ShortInfo {
    String getShortInfo();

    static String of(ShortInfo inst) {
        if (inst == null) return "-";
        return inst.getShortInfo();
    }
}
