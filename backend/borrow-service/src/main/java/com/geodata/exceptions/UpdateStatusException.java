package com.geodata.exceptions;

public class UpdateStatusException extends RuntimeException {
    public UpdateStatusException() {
    }

    public UpdateStatusException(String message) {
        super(message);
    }
}
