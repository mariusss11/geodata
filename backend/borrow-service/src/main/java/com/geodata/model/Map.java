package com.geodata.model;

import com.geodata.utils.BorrowStatus;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Slf4j
@Getter
@Setter
public class Map {
    private int id;
    private String name;
    private int year;
    private boolean isEnabled = true;
    private String availabilityStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isAvailable() {
        return availabilityStatus.equalsIgnoreCase(BorrowStatus.AVAILABLE.dbValue());
    }

    public boolean canBeTransferred() {
        return isAvailable() && isEnabled();
    }



}
