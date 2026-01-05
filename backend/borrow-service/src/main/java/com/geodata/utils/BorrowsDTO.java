package com.geodata.utils;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BorrowsDTO {

    private int id;
    private Client client;
    private LibraryItem item;
    private LocalDateTime borrowDate;
    private LocalDateTime returnDate;
    private String status;
}

