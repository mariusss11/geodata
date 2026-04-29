package com.geodata.utils;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferMapRequest {
    @NotNull
    private int borrowId;
    @NotNull
    private int mapId;
    @NotNull
    private int userIdToTransfer;
}
