package com.quicktable.restaurantservice.entity;

public enum TableCategory {
    INSIDE("Вътре"),
    SUMMER_GARDEN("Лятна градина"),
    WINTER_GARDEN("Зимна градина");

    private final String displayName;

    TableCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
