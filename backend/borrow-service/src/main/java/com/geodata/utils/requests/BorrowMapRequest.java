package com.geodata.utils.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BorrowMapRequest {

    @NotNull
    private int mapId;

    @NotBlank
    private String borrowerName;

    @NotNull
    private LocalDate returnDate;
}
