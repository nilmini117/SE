package com.driveflow.demo_driveflow.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BookingCancellationNotAllowedException extends RuntimeException {
    public BookingCancellationNotAllowedException(String message) {
        super(message);
    }
}
