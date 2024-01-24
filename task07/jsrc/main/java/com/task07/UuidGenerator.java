package com.task07;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.StringInputStream;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.syndicate.deployment.annotations.events.RuleEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;

@LambdaHandler(lambdaName = "uuid_generator",
        roleName = "uuid_generator-role"
)
@RuleEventSource(
        targetRule = "uuid_trigger"
)
public class UuidGenerator implements RequestHandler<Object, Void> {
    private static final String PREFIX = "cmtr-c04fb6ad-";
    private static final String SUFFIX = "-test";
    private static final String BACKET_NAME = PREFIX + "uuid-storage" + SUFFIX;
    private static final int UUID_AMOUNT = 10;
    private static final String IDS_ATTR = "ids";
    @Override
    public Void handleRequest(Object request, Context context) {
        List<UUID> uuidList = new ArrayList<>();
        for (int i = 0; i < UUID_AMOUNT; i++) {
            uuidList.add(UUID.randomUUID());
        }

        Map<String, Object> data = new HashMap<>();
        data.put(IDS_ATTR, uuidList);

        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String jsonResult = "";
        try {
            jsonResult = objectWriter.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        try {
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();
            InputStream inputStream = new StringInputStream(jsonResult);
            String currentTime = Instant.now().toString();
            s3Client.putObject(BACKET_NAME, currentTime, inputStream, new ObjectMetadata());
        } catch (Exception e) {
            context.getLogger().log("Exception: " + e.getMessage());
        }
        return null;
    }
}
