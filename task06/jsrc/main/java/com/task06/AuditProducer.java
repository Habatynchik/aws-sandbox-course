package com.task06;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@LambdaHandler(lambdaName = "audit_producer",
        roleName = "audit_producer-role",
        timeout = 20
)
@DynamoDbTriggerEventSource(
        targetTable = "Configuration",
        batchSize = 10
)
public class AuditProducer implements RequestHandler<DynamodbEvent, Void> {
    private static final String PREFIX = "cmtr-c04fb6ad-";
    private static final String SUFFIX = "-test";
    private static final String TABLE_NAME = PREFIX + "Audit" + SUFFIX;
    private static final String INSERT_EVENT_TYPE = "INSERT";
    private static final String MODIFY_EVENT_TYPE = "MODIFY";
    private static final String ID_ATTR = "id";
    private static final String ITEM_KEY_ATTR = "itemKey";
    private static final String MODIFICATION_TIME_ATTR = "modificationTime";
    private static final String UPDATED_ATTRIBUTE_ATTR = "updatedAttribute";
    private static final String KEY_ATTR = "key";
    private static final String VALUE_ATTR = "value";
    private static final String NEW_VALUE_ATTR = "newValue";
    private static final String OLD_VALUE_ATTR = "oldValue";

    @Override
    public Void handleRequest(DynamodbEvent dynamodbEvent, Context context) {
        AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard().build();

        dynamodbEvent.getRecords().stream()
                .filter(Objects::nonNull)
                .filter(e -> e.getEventName().equals(INSERT_EVENT_TYPE) || e.getEventName().equals(MODIFY_EVENT_TYPE))
                .forEach(ddbRecord -> {
                    Map<String, AttributeValue> newImageData = ddbRecord.getDynamodb().getNewImage();
                    Map<String, AttributeValue> oldImageData = ddbRecord.getDynamodb().getOldImage();

                    PutItemRequest putItemRequest = new PutItemRequest();
                    putItemRequest.withTableName(TABLE_NAME)
                            .addItemEntry(ID_ATTR, new AttributeValue().withS(UUID.randomUUID().toString()))
                            .addItemEntry(ITEM_KEY_ATTR, new AttributeValue().withS(newImageData.get(KEY_ATTR).getS()))
                            .addItemEntry(MODIFICATION_TIME_ATTR, new AttributeValue().withS(Instant.now().toString()));

                    if (ddbRecord.getEventName().equals(INSERT_EVENT_TYPE)) {
                        Map<String, AttributeValue> newValueMap = newImageData.entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> new AttributeValue(e.getValue().getS())));

                        putItemRequest.addItemEntry(NEW_VALUE_ATTR, new AttributeValue().withM(newValueMap));
                    } else {
                        putItemRequest.addItemEntry(UPDATED_ATTRIBUTE_ATTR, new AttributeValue().withS(VALUE_ATTR));
                        putItemRequest.addItemEntry(OLD_VALUE_ATTR, oldImageData.get(VALUE_ATTR));
                        putItemRequest.addItemEntry(NEW_VALUE_ATTR, newImageData.get(VALUE_ATTR));
                    }

                    dynamoDB.putItem(putItemRequest);
                });
        return null;
    }
}
