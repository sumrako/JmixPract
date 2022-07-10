package com.company.jmixpract.screen.calendar;

import javax.annotation.Nullable;

import io.jmix.core.metamodel.datatype.impl.EnumClass;

public enum CalendarMode implements EnumClass<String> {
    DAY("DAY"),
    WEEK("WEEK"),
    MONTH("MONTH");

    private String id;

    CalendarMode(String value) {
        this.id = value;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static CalendarMode fromId(String id) {
        for (CalendarMode at : CalendarMode.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}
