package com.geodata.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryItemDTORequest {
    private String categoryName;
    private String itemTitle;
    private String itemType;
    private String author;
}
