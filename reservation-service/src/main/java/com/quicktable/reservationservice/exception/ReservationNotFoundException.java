package com.quicktable.reservationservice.exception;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(Long id) {
        super("Резервация с ID " + id + " не е намерена");
    }
}
