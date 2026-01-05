package com.geodata.utils.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BorrowMapRequest {
    @NotBlank
    private String itemTitle;

    @NotBlank
    private String itemType;

    @NotBlank
    private String authorName;

}
