package com.geodata.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class LibraryItemDTO {

    private int itemId;
    private String title;

    @JsonIgnore
    private String author;
    @JsonIgnore
    private int yearPublished;
    @JsonIgnore
    private boolean isBorrowed;


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

    @Override
    public String toString() {
        return "LibraryItemDTO{" +
                "itemId=" + itemId +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", yearPublished=" + yearPublished +
                ", isBorrowed=" + isBorrowed +
                ", itemType='" + itemType + '\'' +
                '}';
    }
}
