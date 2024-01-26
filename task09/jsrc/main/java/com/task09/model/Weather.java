package com.task09.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

@DynamoDBTable(tableName = "Weather")
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Weather {
    private String id;
    private Forecast forecast;

    @DynamoDBHashKey(attributeName = "id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDBAttribute(attributeName = "forecast")
    public void setForecast(Forecast forecast) {
        this.forecast = forecast;
    }

    public Forecast getForecast() {
        return forecast;
    }
}
