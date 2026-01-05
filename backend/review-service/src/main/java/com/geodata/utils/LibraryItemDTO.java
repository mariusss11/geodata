package com.geodata.utils;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@ToString
public class LibraryItemDTO {

    private int id;
    private String title;

    //    @JsonIgnore
    private String author;
    //    @JsonIgnore
    private int yearPublished;
    //    @JsonIgnore
    private boolean isBorrowed;

    private boolean isEnabled = true;

    private String itemType;

    public LibraryItemDTO() {
    }

    public LibraryItemDTO(String title, String author, String itemType) {
        this.title = title;
        this.author = author;
        this.itemType = itemType;
    }


    public LibraryItemDTO(String title, String author, int yearPublished, boolean isBorrowed) {
        this.title = title;
        this.author = author;
        this.yearPublished = yearPublished;
        this.isBorrowed = isBorrowed;
    }
}
