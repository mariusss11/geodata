package com.geodata.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BorrowActions {

    private int id;
    private int itemId;
    private int clientId;
    private String status;
    private String statusReason;
    private LocalDateTime actionDate;

    public BorrowActions(int itemId, int clientId) {
        this.itemId = itemId;
        this.clientId = clientId;
    }

    protected BorrowActions(int itemId, int clientId, String status, String statusReason, LocalDateTime actionDate) {
        this.itemId = itemId;
        this.clientId = clientId;
        this.status = status;
        this.statusReason = statusReason;
        this.actionDate = actionDate;
    }
}
