package com.geodata.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientDTO {
    private int clientId;
    private String name;
    private String email;



    public ClientDTO(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
