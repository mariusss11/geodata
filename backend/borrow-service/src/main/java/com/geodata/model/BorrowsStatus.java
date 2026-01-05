package com.geodata.model;

public enum BorrowsStatus {
    LOST,

    // Librarian approved, waiting for pickup, for future improvements
    APPROVED,

    // Client has requested approval to borrow the item
    BORROW_REQUESTED,

    // Librarian declined the borrow request
    BORROW_DECLINED,

    // Client picked up the item
    BORROWED,

    // Client wants to return the item
    RETURN_REQUESTED,

    // Librarian declined the return (e.g., damaged, wrong item)
    RETURN_DECLINED,

    // Return accepted and completed
    RETURNED
    ;

    /**
     * Converts the enum value to a lowercase string for database storage.
     * @return the lowercase name of the enum constant.
     */
    public String dbValue() {
        return this.name().toLowerCase();
    }
}
