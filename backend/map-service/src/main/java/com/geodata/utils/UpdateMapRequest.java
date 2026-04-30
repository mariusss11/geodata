package com.geodata.utils;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMapRequest {

    @NotNull
    private String name;

    @NotNull
    private int yearPublished;

}
