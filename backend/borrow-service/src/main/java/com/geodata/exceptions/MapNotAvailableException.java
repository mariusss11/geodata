package com.geodata.exceptions;

public class MapNotAvailableException extends RuntimeException {
    public MapNotAvailableException() {
    }

    public MapNotAvailableException(String message) {
        super(message);
    }
}
