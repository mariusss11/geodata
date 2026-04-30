package com.geodata.utils;

import com.geodata.model.Borrows;
import com.geodata.model.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferResponse {
    Borrows newBorrow;
    String newBorrowerName;
    Map mapTransferred;
}
