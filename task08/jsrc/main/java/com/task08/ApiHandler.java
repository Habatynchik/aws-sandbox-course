package com.task08;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.syndicate.deployment.annotations.LambdaUrlConfig;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

import demo.custom.sdk.OpenMeteoAPI;

@LambdaHandler(lambdaName = "api_handler",
        roleName = "api_handler-role",
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
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final int STATUS_CODE_OK = 200;
    private static final int STATUS_CODE_BAD_REQUEST = 400;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        OpenMeteoAPI openMeteoAPI = new OpenMeteoAPI();

        try {
            String forecast = openMeteoAPI.getLatestForecast();
            context.getLogger().log(forecast);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(STATUS_CODE_OK)
                    .withBody(forecast);
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(STATUS_CODE_BAD_REQUEST);
        }

    }
}
