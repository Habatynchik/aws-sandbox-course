package com.task09;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sdk.OpenMeteoAPI;
import com.syndicate.deployment.annotations.LambdaUrlConfig;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.TracingMode;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import com.task09.model.Forecast;
import com.task09.model.Weather;

import java.util.UUID;

@LambdaHandler(lambdaName = "processor",
        roleName = "processor-role",
        tracingMode = TracingMode.Active,
        layers = {"sdk-layer"},
        runtime = DeploymentRuntime.JAVA8
)
@LambdaLayer(
        layerName = "sdk-layer",
        libraries = {"lib/OpenMeteoAPI.jar"},
        runtime = DeploymentRuntime.JAVA8,
        artifactExtension = ArtifactExtension.ZIP
)
@LambdaUrlConfig(
        authType = AuthType.NONE,
        invokeMode = InvokeMode.BUFFERED
)
public class Processor implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final int STATUS_CODE_OK = 200;
    private static final String MESSAGE_OK = "Successfully";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.defaultClient();
        OpenMeteoAPI openMeteoAPI = new OpenMeteoAPI();
        Gson gson = new Gson();

        String latestForecast = openMeteoAPI.getLatestForecast();
        Forecast forecast = gson.fromJson(latestForecast, new TypeToken<Forecast>() {
        }.getType());
        Weather weatherRecord = Weather.builder()
                .id(UUID.randomUUID().toString())
                .forecast(forecast)
                .build();

        DynamoDBMapper dbMapper = new DynamoDBMapper(dynamoDB);
        dbMapper.save(weatherRecord);

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(STATUS_CODE_OK)
                .withBody(MESSAGE_OK);
    }
}
