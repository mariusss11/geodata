package com.geodata.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BorrowActionsDTO {
    private int id;
    private LibraryItem item;
    private Client client;
    private String status;
    private String statusReason;
    private LocalDateTime actionDate;
}

