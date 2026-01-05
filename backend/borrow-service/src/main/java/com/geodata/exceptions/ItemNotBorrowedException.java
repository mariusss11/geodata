package com.geodata.exceptions;

public class ItemNotBorrowedException extends ItemNotAvailableException {
    public ItemNotBorrowedException() {
    }

    public ItemNotBorrowedException(String message) {
        super(message);
    }
}
