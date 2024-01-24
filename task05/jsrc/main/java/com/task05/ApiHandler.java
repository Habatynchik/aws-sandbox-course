package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;

import java.time.Instant;
import java.util.UUID;

@LambdaHandler(lambdaName = "api_handler",
        roleName = "api_handler-role"
)
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final String PREFIX = "cmtr-c04fb6ad-";
    private static final String SUFFIX = "-test";
    private static final String TABLE_NAME = PREFIX + "Events" + SUFFIX;
    private static final int SUCCESS_CODE = 201;
    private static final int SERVER_ERROR_CODE = 500;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        LambdaLogger logger = context.getLogger();
        try {
            AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
            DynamoDB dynamoDB = new DynamoDB(client);
            Table table = dynamoDB.getTable(TABLE_NAME);

            ObjectMapper objectMapper = new ObjectMapper();
            EventRequest eventRequest = objectMapper.readValue(apiGatewayProxyRequestEvent.getBody(), EventRequest.class);

            String id = UUID.randomUUID().toString();
            String createAt = Instant.now().toString();

            Item item = new Item()
                    .withPrimaryKey("id", id)
                    .withNumber("principalId", eventRequest.getPrincipalId())
                    .withMap("body", eventRequest.getContent())
                    .withString("createAt", createAt);

            PutItemOutcome response = table.putItem(item);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(SUCCESS_CODE)
                    .withBody(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            logger.log("Exception: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(SERVER_ERROR_CODE)
                    .withBody("An error occurred: " + e.getMessage());
        }
    }
}
