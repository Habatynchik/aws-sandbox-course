package com.task10;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.task10.dto.*;
import com.task10.exception.ReservationAlreadyExist;
import com.task10.exception.TableNotFoundException;
import com.task10.model.DynamoDbService;
import com.task10.model.impl.DynamoDbServiceImpl;
import com.task10.service.Tables;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.HashMap;
import java.util.Map;

@LambdaHandler(lambdaName = "api_handler",
        roleName = "api_handler-role",
        runtime = DeploymentRuntime.JAVA8
)
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final int STATUS_CODE_OK = 200;
    private static final int STATUS_CODE_ERROR = 400;
    private static final int STATUS_CODE_PAGE_NOT_FOUND = 404;

    private static final String GET_METHOD = "GET";
    private static final String POST_METHOD = "POST";

    private CognitoIdentityProviderClient cognitoClient;
    private AmazonDynamoDB dynamoDB;
    private DynamoDbService dynamoDbService;
    private Gson gson;

    public ApiHandler() {
        this.initCognitoIdentityProviderClient();
        this.initDynamoDbClient();
        dynamoDbService = new DynamoDbServiceImpl(dynamoDB);
        gson = new Gson();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {

        context.getLogger().log(input.getBody());

        if (input.getPath().startsWith("/signup")) {
            return handleSignOutRequest(input, context);
        } else if (input.getPath().startsWith("/signin")) {
            return handleSignInRequest(input, context);
        } else if (input.getPath().startsWith("/tables") && input.getHttpMethod().equals(GET_METHOD)) {
            return getTables(input);
        } else if (input.getPath().startsWith("/tables") && input.getHttpMethod().equals(POST_METHOD)) {
            return createTables(input);
        } else if (input.getPath().startsWith("/reservations") && input.getHttpMethod().equals(GET_METHOD)) {
            return getReservations();
        } else if (input.getPath().startsWith("/reservations") && input.getHttpMethod().equals(POST_METHOD)) {
            return createReservation(input);
        }
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(STATUS_CODE_PAGE_NOT_FOUND);
    }


    private APIGatewayProxyResponseEvent handleSignInRequest(APIGatewayProxyRequestEvent event, Context context) {
        SigninRequest signinRequest = gson.fromJson(event.getBody(), SigninRequest.class);

        try {
            AdminInitiateAuthResponse authResponse = signIn(cognitoClient, signinRequest.getEmail(), signinRequest.getPassword());
            SigninResponse signinResponse = SigninResponse.builder()
                    .accessToken(authResponse.authenticationResult().accessToken())
                    .build();

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(STATUS_CODE_OK)
                    .withBody(gson.toJson(signinResponse));
        } catch (CognitoIdentityProviderException e) {
            context.getLogger().log(e.awsErrorDetails().errorMessage());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(STATUS_CODE_ERROR);
        }
    }

    private APIGatewayProxyResponseEvent handleSignOutRequest(APIGatewayProxyRequestEvent event, Context context) {
        SignupRequest signupRequest = gson.fromJson(event.getBody(), SignupRequest.class);

        try {
            signUp(cognitoClient, getUserPoolClientId(), signupRequest.getFirstName(), signupRequest.getPassword(), signupRequest.getEmail());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(STATUS_CODE_OK);
        } catch (CognitoIdentityProviderException e) {
            context.getLogger().log(e.awsErrorDetails().errorMessage());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(STATUS_CODE_ERROR);
        }
    }

    private APIGatewayProxyResponseEvent getTables(APIGatewayProxyRequestEvent event) {
        if (event.getPathParameters() != null && event.getPathParameters().containsKey("tableId")) {
            int tableId = Integer.parseInt(event.getPathParameters().get("tableId"));
            return getTablesById(tableId);
        }
        return getAllTables();
    }

    private APIGatewayProxyResponseEvent getTablesById(int id) {
        try {
            Tables tables = dynamoDbService.getTablesById(id);

            return new APIGatewayProxyResponseEvent()
                    .withBody(gson.toJson(tables))
                    .withStatusCode(STATUS_CODE_OK);
        } catch (TableNotFoundException e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(STATUS_CODE_ERROR);
        }
    }

    private APIGatewayProxyResponseEvent getAllTables() {
        GetTablesResponse response = dynamoDbService.getAllTables();

        return new APIGatewayProxyResponseEvent()
                .withBody(gson.toJson(response))
                .withStatusCode(STATUS_CODE_OK);
    }

    private APIGatewayProxyResponseEvent getReservations() {
        GetReservationsResponse response = dynamoDbService.getAllReservations();

        return new APIGatewayProxyResponseEvent()
                .withBody(gson.toJson(response))
                .withStatusCode(STATUS_CODE_OK);
    }

    private APIGatewayProxyResponseEvent createTables(APIGatewayProxyRequestEvent event) {


        PostTablesRequest request = gson.fromJson(event.getBody(), PostTablesRequest.class);
        PostTablesResponse response = dynamoDbService.createTables(request);

        return new APIGatewayProxyResponseEvent()
                .withBody(gson.toJson(response))
                .withStatusCode(STATUS_CODE_OK);
    }

    private APIGatewayProxyResponseEvent createReservation(APIGatewayProxyRequestEvent event) {
        PostReservationsRequest request = gson.fromJson(event.getBody(), PostReservationsRequest.class);

        try {
            PostReservationsResponse response = dynamoDbService.createReservations(request);

            return new APIGatewayProxyResponseEvent()
                    .withBody(gson.toJson(response))
                    .withStatusCode(STATUS_CODE_OK);
        } catch (TableNotFoundException | ReservationAlreadyExist e) {
            return new APIGatewayProxyResponseEvent()
                    .withBody(e.toString())
                    .withStatusCode(STATUS_CODE_ERROR);
        }
    }

    private AdminInitiateAuthResponse signIn(CognitoIdentityProviderClient cognitoClient, String username, String password) throws CognitoIdentityProviderException {
        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", username);
        authParameters.put("PASSWORD", password);

        AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                .clientId(getUserPoolClientId())
                .userPoolId(getUserPoolId())
                .authParameters(authParameters)
                .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                .build();

        return cognitoClient.adminInitiateAuth(authRequest);
    }

    private void signUp(CognitoIdentityProviderClient identityProviderClient, String clientId, String userName, String password, String email) throws CognitoIdentityProviderException {
        AttributeType attributeType = AttributeType.builder()
                .name("name").value(userName)
                .name("email").value(email)
                .build();

        SignUpRequest signUpRequest = SignUpRequest.builder()
                .userAttributes(attributeType)
                .username(email)
                .clientId(clientId)
                .password(password)
                .build();
        identityProviderClient.signUp(signUpRequest);

        AdminConfirmSignUpRequest confirmSignUpRequest = AdminConfirmSignUpRequest.builder()
                .userPoolId(getUserPoolId())
                .username(email)
                .build();

        identityProviderClient.adminConfirmSignUp(confirmSignUpRequest);

    }

    private String getUserPoolClientId() {
        ListUserPoolClientsRequest userPoolClientsRequest = ListUserPoolClientsRequest.builder()
                .userPoolId(getUserPoolId())
                .build();
        ListUserPoolClientsResponse userPoolClientsResponse = cognitoClient.listUserPoolClients(userPoolClientsRequest);

        return userPoolClientsResponse.userPoolClients().get(0).clientId();
    }

    private String getUserPoolId() {
        ListUserPoolsRequest listUserPoolsRequest = ListUserPoolsRequest.builder().build();
        ListUserPoolsResponse userPoolsResponse = cognitoClient.listUserPools(listUserPoolsRequest);

        return userPoolsResponse.userPools().get(0).id();
    }

    private void initCognitoIdentityProviderClient() {
        cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.EU_CENTRAL_1)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }

    private void initDynamoDbClient() {
        dynamoDB = AmazonDynamoDBClientBuilder.defaultClient();
    }
}
