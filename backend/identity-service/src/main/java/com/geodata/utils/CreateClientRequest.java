package com.geodata.utils;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateClientRequest {

    private String name;
    private String email;

}
