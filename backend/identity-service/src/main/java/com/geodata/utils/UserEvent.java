package com.geodata.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserEvent {

    private int userId;
    private String name;
    private String username;
    private LocalDateTime dateTime;


}
