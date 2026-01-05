package com.geodata.exceptions;

public class MapNotFoundException extends RuntimeException {
    public MapNotFoundException(String message) {
        super(message);
    }
}
