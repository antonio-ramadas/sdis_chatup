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
}