package com.task10.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@DynamoDBTable(tableName = "cmtr-c04fb6ad-Tables-test")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Tables {
    @DynamoDBHashKey(attributeName = "id")
    private int id;
    private int number;
    private int places;
    private boolean isVip;
    private int minOrder;
}
