package com.task09.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import lombok.AllArgsConstructor;
import lombok.Data;

@DynamoDBDocument
@Data
@AllArgsConstructor
public class HourlyUnits {
    private String temperature_2m;
    private String time;
}
