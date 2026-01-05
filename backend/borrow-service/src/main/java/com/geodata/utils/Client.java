package com.geodata.utils;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Client {
    private int clientId;
    private String name;
    private String email;



    public Client(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
