package com.task10.dto;

import com.task10.model.Tables;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GetTablesResponse {
    private List<Tables> tables;
}
