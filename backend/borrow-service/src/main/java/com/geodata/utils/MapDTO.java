package com.geodata.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class MapDTO {

    private int id;
    private String name;
    private int year;
    private boolean isEnabled = true;
    private String availabilityStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
