package com.geodata.exceptions;

public class ItemNotAvailableException extends RuntimeException {
    public ItemNotAvailableException() {
    }

    public ItemNotAvailableException(String message) {
        super(message);
    }
}
