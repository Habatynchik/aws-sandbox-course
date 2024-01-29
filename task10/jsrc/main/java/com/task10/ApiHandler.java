package com.task10;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.task10.dto.*;
import com.task10.service.DynamoDbService;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.*;

@LambdaHandler(lambdaName = "api_handler", roleName = "api_handler-role")
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private CognitoIdentityProviderClient cognitoClient;
    private AmazonDynamoDB amazonDynamoDB;
    private static final String REGION = "eu-central-1";

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        this.initCognito();
        this.initDynamoDbClient();
        Gson gson = new GsonBuilder().create();

        String url = input.getPath();
        System.out.println(input);
        System.out.println(input.getPathParameters());
        if (url.equals("/signup")) {
            APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent();
            SignupRequest signupRequest = gson.fromJson(input.getBody(), new TypeToken<SignupRequest>() {
            }.getType());
            try {
                signUp(cognitoClient, getUserPoolClientId(), signupRequest.getFirstName(), signupRequest.getPassword(), signupRequest.getEmail());
                apiGatewayProxyResponseEvent.withStatusCode(200);
            } catch (CognitoIdentityProviderException e) {
                apiGatewayProxyResponseEvent.withStatusCode(400);
            }

            return apiGatewayProxyResponseEvent;
        }

        if (url.equals("/signin")) {
            SigninRequest signinRequest = gson.fromJson(input.getBody(), new TypeToken<SigninRequest>() {
            }.getType());
            APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent();

            try {
                AdminInitiateAuthResponse adminInitiateAuthResponse = login(cognitoClient, signinRequest.getEmail(), signinRequest.getPassword());
                SigningResponse signingResponse = new SigningResponse();
                signingResponse.setAccessToken(adminInitiateAuthResponse.authenticationResult().idToken());
                apiGatewayProxyResponseEvent.withBody(gson.toJson(signingResponse));

                return apiGatewayProxyResponseEvent;
            } catch (Exception e) {
                apiGatewayProxyResponseEvent.setStatusCode(400);
                return apiGatewayProxyResponseEvent;
            }
        }

        if (url.equals("/tables")) {
            DynamoDbService dynamoDbService = new DynamoDbService();
            APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent();
            if (input.getHttpMethod().equals("GET")) {
                apiGatewayProxyResponseEvent.withBody(gson.toJson(dynamoDbService.getAllTables(amazonDynamoDB)));
                return apiGatewayProxyResponseEvent;
            } else if (input.getHttpMethod().equals("POST")) {
                TablesRequest tablesRequest = gson.fromJson(input.getBody(), new TypeToken<TablesRequest>() {
                }.getType());
                apiGatewayProxyResponseEvent.withBody(gson.toJson(dynamoDbService.createTable(amazonDynamoDB, tablesRequest)));
                return apiGatewayProxyResponseEvent;
            }
        }

        if (input.getResource().equals("/tables/{tableId}")) {
            APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent();
            DynamoDbService dynamoDbService = new DynamoDbService();
            int tableId = Integer.parseInt(input.getPathParameters().get("tableId"));
            return apiGatewayProxyResponseEvent.withBody(gson.toJson(dynamoDbService.getTableById(amazonDynamoDB, tableId)));
        }

        if (url.equals("/reservations")) {
            DynamoDbService dynamoDbService = new DynamoDbService();
            APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent();
            if (input.getHttpMethod().equals("GET")) {
                apiGatewayProxyResponseEvent.withBody(gson.toJson(dynamoDbService.getAllReservations(amazonDynamoDB)));
                return apiGatewayProxyResponseEvent;
            } else if (input.getHttpMethod().equals("POST")) {
                ReservationsRequest reservationsRequest = gson.fromJson(input.getBody(), new TypeToken<ReservationsRequest>() {
                }.getType());
                try{
                    apiGatewayProxyResponseEvent.withBody(gson.toJson(dynamoDbService.createReservation(amazonDynamoDB, reservationsRequest)));
                }catch (Exception e){
                    System.out.println(e);
                    apiGatewayProxyResponseEvent.withStatusCode(400);
                }
                return apiGatewayProxyResponseEvent;
            }
        }

        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent();
        apiGatewayProxyResponseEvent.withStatusCode(500);
        return apiGatewayProxyResponseEvent;
    }

    private void initCognito() {
        cognitoClient = CognitoIdentityProviderClient.builder().region(Region.EU_CENTRAL_1) // Change to your desired region
                .build();
    }

    private AdminInitiateAuthResponse login(CognitoIdentityProviderClient cognitoClient, String userName, String password) {
        try {
            Map<String, String> authParameters = new HashMap<>();
            authParameters.put("USERNAME", userName);
            authParameters.put("PASSWORD", password);

            AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder().clientId(getUserPoolClientId()).userPoolId(getUserPoolId()).authParameters(authParameters).authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH).build();

            AdminInitiateAuthResponse response = cognitoClient.adminInitiateAuth(authRequest);
            return response;

        } catch (CognitoIdentityProviderException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        }

        return null;
    }

    private String getUserPoolClientId() {
        ListUserPoolClientsRequest userPoolClientsRequest = ListUserPoolClientsRequest.builder().userPoolId(getUserPoolId()).build();

        ListUserPoolClientsResponse userPoolClientsResponse = cognitoClient.listUserPoolClients(userPoolClientsRequest);

        return userPoolClientsResponse.userPoolClients().get(0).clientId();
    }

    private String getUserPoolId() {
        ListUserPoolsRequest listUserPoolsRequest = ListUserPoolsRequest.builder().build();

        ListUserPoolsResponse userPoolsResponse = cognitoClient.listUserPools(listUserPoolsRequest);

        return userPoolsResponse.userPools().get(0).id();
    }

    private void signUp(CognitoIdentityProviderClient identityProviderClient, String clientId, String userName, String password, String email) {
        AttributeType userAttrs = AttributeType.builder().name("name").value(userName).name("email").value(email).build();

        List<AttributeType> userAttrsList = new ArrayList<>();
        userAttrsList.add(userAttrs);
        try {
            SignUpRequest signUpRequest = SignUpRequest.builder().userAttributes(userAttrsList).username(email).clientId(clientId).password(password).build();
            identityProviderClient.signUp(signUpRequest);
            System.out.println("User has been signed up");

            AdminConfirmSignUpRequest confirmSignUpRequest = AdminConfirmSignUpRequest.builder()
                    .userPoolId(getUserPoolId())
                    .username(email)
                    .build();
            identityProviderClient.adminConfirmSignUp(confirmSignUpRequest);
            System.out.println("Sing up confirmed");
        } catch (CognitoIdentityProviderException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    private void initDynamoDbClient() {
        this.amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withRegion(REGION)
                .build();
    }
}
