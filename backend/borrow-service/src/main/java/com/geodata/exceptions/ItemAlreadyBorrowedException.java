package com.geodata.exceptions;

public class ItemAlreadyBorrowedException extends RuntimeException {
    public ItemAlreadyBorrowedException() {
    }

    public ItemAlreadyBorrowedException(String message) {
        super(message);
    }
}
