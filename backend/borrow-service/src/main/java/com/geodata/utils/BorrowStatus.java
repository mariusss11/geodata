package com.geodata.utils;

public enum BorrowStatus {

    LOST,

    // Booked for someone, future improvements
    RESERVED,

    // Open for anyone who wants to borrow it
    AVAILABLE,

    // Borrowed by a client
    BORROWED,

    TRANSFERRED,

    //  Waits for the approval of the librarian, before being borrowed by someone
    PENDING_BORROW_APPROVAL,

    //  Waits for the approval of the librarian, before being returned by someone
    PENDING_RETURN_APPROVAL,





    ;

    public String dbValue() {
        return this.name().toUpperCase();
    }
}
