package com.geodata.exceptions;

public class ClientServiceException extends RuntimeException {
    public ClientServiceException() {
    }

    public ClientServiceException(String message) {
        super(message);
    }
}
