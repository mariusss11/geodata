package com.geodata.exceptions;

public class BorrowNotFoundException extends RuntimeException {
    public BorrowNotFoundException() {
    }

    public BorrowNotFoundException(String message) {
        super(message);
    }
}
