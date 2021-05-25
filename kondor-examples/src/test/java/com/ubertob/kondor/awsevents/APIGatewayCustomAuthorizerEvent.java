package com.ubertob.kondor.awsevents;

import java.util.Map;

public class APIGatewayCustomAuthorizerEvent {

    private String version;
    private String type;
    private String methodArn;
    private String identitySource;
    private String authorizationToken;
    private String resource;
    private String path;
    private String httpMethod;
    private Map<String, String> headers;
    private Map<String, String> queryStringParameters;
    private Map<String, String> pathParameters;
    private Map<String, String> stageVariables;
    private RequestContext requestContext;


    public static class RequestContext {
        private String path;
        private String accountId;
        private String resourceId;
        private String stage;
        private String requestId;
        private Identity identity;
        private String resourcePath;
        private String httpMethod;
        private String apiId;
    }


    public static class Identity {
        private String apiKey;
        private String sourceIp;
    }
}