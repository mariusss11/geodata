package com.geodata.exceptions;

public class InvalidRoleException extends RuntimeException {
    public InvalidRoleException() {
    }

    public InvalidRoleException(String message) {
        super(message);
    }
}
