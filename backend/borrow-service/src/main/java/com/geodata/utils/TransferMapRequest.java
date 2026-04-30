package com.geodata.utils;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferMapRequest {
    @NotNull
    private int borrowId;

    @NotBlank
    private String newBorrowerName;

    @NotNull
    private LocalDate newExpectedReturnDate;
}
