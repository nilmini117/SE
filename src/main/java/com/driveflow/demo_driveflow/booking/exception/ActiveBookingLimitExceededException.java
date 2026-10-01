package com.driveflow.demo_driveflow.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ActiveBookingLimitExceededException extends RuntimeException {
    public ActiveBookingLimitExceededException(String message) {
        super(message);
    }
}
