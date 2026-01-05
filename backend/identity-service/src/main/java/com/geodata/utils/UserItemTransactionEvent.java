package com.geodata.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
public class UserItemTransactionEvent {
    private String name;
    private String username;
    private String itemTitle;
    private String itemType;
    private LocalDateTime eventTime;
}
