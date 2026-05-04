package com.quicktable.common.dto;

public enum ReservationStatus {
    CONFIRMED,      // Потвърдена (начален статус при създаване)
    CANCELLED,      // Отказана от клиента
    REJECTED,       // Отхвърлена от ресторанта
    COMPLETED,      // Завършена (клиентът е посетил ресторанта)
    NO_SHOW         // Клиентът не се е явил
}
