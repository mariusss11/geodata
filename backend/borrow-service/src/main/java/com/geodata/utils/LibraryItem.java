package com.geodata.utils;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@ToString
public class LibraryItem {

    private int id;
    private String title;

    private String author;
    private int yearPublished;
    private String availabilityStatus;
    private String itemType;
     private boolean isEnabled = true;

    public LibraryItem() {
    }

    public LibraryItem(String title, String author, String itemType) {
        this.title = title;
        this.author = author;
        this.itemType = itemType;
    }

    public LibraryItem(String title, String author, int yearPublished, String itemType) {
        this.title = title;
        this.author = author;
        this.yearPublished = yearPublished;
        this.itemType = itemType;
    }

    public LibraryItem(String title, String author, int yearPublished, String itemType, String status) {
        this.title = title;
        this.author = author;
        this.yearPublished = yearPublished;
        this.itemType = itemType;
        this.availabilityStatus = status;
    }

    public boolean isAvailable() {
        return availabilityStatus.equalsIgnoreCase(BorrowStatus.AVAILABLE.dbValue());
    }

}
