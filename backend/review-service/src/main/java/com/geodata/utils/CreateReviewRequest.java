package com.geodata.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateReviewRequest {

    private int itemId;
    private String comment;
    private int rating;
    private boolean isAnonymous;
}
