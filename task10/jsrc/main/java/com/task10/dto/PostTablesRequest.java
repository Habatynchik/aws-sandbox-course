package com.task10.dto;

import lombok.Data;

@Data
public class PostTablesRequest {
    private int id;
    private int number;
    private int places;
    private boolean isVip;
    private int minOrder;
}
