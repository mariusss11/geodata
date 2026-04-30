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
public class BorrowedMapDto {
    private int borrowId;
    private int mapId;
    private String borrowerName;
    private String name;
    private int year;
    private boolean isEnabled;
    private String availabilityStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
