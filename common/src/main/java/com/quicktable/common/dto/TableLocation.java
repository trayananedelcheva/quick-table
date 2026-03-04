package com.quicktable.common.dto;

public enum TableLocation {
    INSIDE("Вътре"),
    SUMMER_GARDEN("Лятна градина"),
    WINTER_GARDEN("Зимна градина");

    private final String displayName;

    TableLocation(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
