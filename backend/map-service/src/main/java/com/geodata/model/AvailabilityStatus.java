package com.geodata.model;

public enum AvailabilityStatus {

    LOST,

    //  Booked for someone, future improvements
    RESERVED,

    // Open for anyone who wants to borrow it
    AVAILABLE,

    // Borrowed by a client
    BORROWED,

    ;

    public String dbValue() {
        return this.name().toUpperCase();
    }
}
