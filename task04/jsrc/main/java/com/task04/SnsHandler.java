package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.syndicate.deployment.annotations.events.SnsEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;

@LambdaHandler(
		lambdaName = "sns_handler",
		roleName = "sns_handler-role"
)
@SnsEventSource(
		targetTopic = "lambda_topic"
)
public class SnsHandler implements RequestHandler<SNSEvent, Void> {
	@Override
	public Void handleRequest(SNSEvent snsEvent, Context context) {
		LambdaLogger logger = context.getLogger();
		for (SNSEvent.SNSRecord snsRecord : snsEvent.getRecords()) {
			SNSEvent.SNS sns = snsRecord.getSNS();
			logger.log(sns.getMessage());
		}
		return null;
	}
}
