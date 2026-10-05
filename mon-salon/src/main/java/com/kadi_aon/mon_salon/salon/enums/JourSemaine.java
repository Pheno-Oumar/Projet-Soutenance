package com.kadi_aon.mon_salon.salon.enums;

import java.time.DayOfWeek;

public enum JourSemaine {
    LUNDI,
    MARDI,
    MERCREDI,
    JEUDI,
    VENDREDI,
    SAMEDI,
    DIMANCHE;

    public static JourSemaine from(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            return null;
        }
        return switch (dayOfWeek) {
            case MONDAY -> LUNDI;
            case TUESDAY -> MARDI;
            case WEDNESDAY -> MERCREDI;
            case THURSDAY -> JEUDI;
            case FRIDAY -> VENDREDI;
            case SATURDAY -> SAMEDI;
            case SUNDAY -> DIMANCHE;
        };
    }
}
