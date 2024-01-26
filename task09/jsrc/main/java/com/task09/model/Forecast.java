package com.task09.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import lombok.AllArgsConstructor;
import lombok.Data;

@DynamoDBDocument
@Data
@AllArgsConstructor
public class Forecast {
    private int elevation;
    private double generationtime_ms;
    private Hourly hourly;
    private HourlyUnits hourly_units;
    private double latitude;
    private double longutude;
    private String timezone;
    private String timezone_abbreviation;
    private long utc_offset_seconds;
}
