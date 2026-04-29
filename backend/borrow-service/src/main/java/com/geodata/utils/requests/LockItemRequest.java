package com.geodata.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LockItemRequest {
    private int mapId;

    // No-args constructor - required for Jackson
    public LockItemRequest() {
    }

    // All-args constructor for convenience
    public LockItemRequest(int mapId) {
        this.mapId = mapId;
    }

}
