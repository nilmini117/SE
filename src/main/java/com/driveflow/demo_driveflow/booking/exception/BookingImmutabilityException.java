package com.driveflow.demo_driveflow.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class BookingImmutabilityException extends RuntimeException {
    public BookingImmutabilityException(String message) {
        super(message);
    }
}
