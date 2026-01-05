package com.geodata.utils;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Client {

    private int clientId;
    private String name;
    private String email;
    private boolean isEnabled;

    public Client(String name, String email) {
        this.name = name;
        this.email = email;
        isEnabled = true;
    }
}
