package com.geodata.exceptions;

public class ItemServiceException extends RuntimeException {
    public ItemServiceException() {
    }

    public ItemServiceException(String message) {
        super(message);
    }
}
