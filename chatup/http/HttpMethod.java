package chatup.http;

public enum HttpMethod {

    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE");

    HttpMethod(final String paramMethod) {
        httpMethod = paramMethod;
    }

    private final String httpMethod;

    @Override
    public String toString() {
        return httpMethod;
    }
}