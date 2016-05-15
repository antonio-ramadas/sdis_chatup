package chatup.http;

public enum ServerResponse {

    AuthenticationFailed("authenticationFailed"),
    EmptyResponse("emptyResponse"),
    InvalidCommand("unexpectedCommand"),
    InvalidMethod("invalidMethod"),
    InvalidResponse("invalidResponse"),
    InvalidService("invalidService"),
    InvalidToken("invalidToken"),
    MissingParameters("missingParameters"),
    OperationFailed("operationFailed"),
    ProtocolError("protocolError"),
    RoomNotFound("roomNotFound"),
    ServiceOffline("serviceOffline"),
    ServerNotFound("serverNotFound"),
    SuccessResponse("success");

    private final String responseMessage;

    ServerResponse(final String paramResponse) {
        responseMessage = paramResponse;
    }

    @Override
    public String toString() {
        return responseMessage;
    }

    public static ServerResponse fromString(final String httpResponse) {

        if (httpResponse != null) {

            for (final ServerResponse hr : ServerResponse.values()) {

                if (httpResponse.equalsIgnoreCase(hr.responseMessage)) {
                    return hr;
                }
            }
        }

        return ServerResponse.InvalidResponse;
    }
}