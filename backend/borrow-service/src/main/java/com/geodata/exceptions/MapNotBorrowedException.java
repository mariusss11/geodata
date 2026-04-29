package com.geodata.exceptions;

public class MapNotBorrowedException extends MapNotAvailableException {
    public MapNotBorrowedException() {
    }

    public MapNotBorrowedException(String message) {
        super(message);
    }
}
