package com.quicktable.common.dto;

public enum ReservationStatus {
    PENDING,        // Изчаква потвърждение
    CONFIRMED,      // Потвърдена
    CANCELLED,      // Отказана от клиента
    REJECTED,       // Отхвърлена от ресторанта
    COMPLETED       // Завършена (клиентът е посетил ресторанта)
}
