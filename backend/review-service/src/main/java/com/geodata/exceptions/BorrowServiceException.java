package com.geodata.exceptions;

public class BorrowServiceException extends RuntimeException {
    public BorrowServiceException() {
    }

    public BorrowServiceException(String message) {
        super(message);
    }
}
