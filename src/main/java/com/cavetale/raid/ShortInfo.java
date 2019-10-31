package com.cavetale.raid;

interface ShortInfo {
    String getShortInfo();

    static String of(ShortInfo inst) {
        if (inst == null) return "-";
        return inst.getShortInfo();
    }
}
