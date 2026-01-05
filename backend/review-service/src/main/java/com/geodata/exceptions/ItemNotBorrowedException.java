package com.geodata.exceptions;

public class ItemNotBorrowedException extends RuntimeException {
    public ItemNotBorrowedException() {
    }

    public ItemNotBorrowedException(String message) {
        super(message);
    }
}
