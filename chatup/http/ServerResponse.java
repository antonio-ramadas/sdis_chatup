package chatup.http;

public enum ServerResponse {

    AuthenticationFailed("authenticationFailed"),
    DatabaseError("databaseError"),
    InvalidCommand("unexpectedCommand"),
    InvalidMethod("invalidMethod"),
    InvalidService("invalidService"),
    InvalidToken("invalidToken"),
    MissingParameters("missingParameters"),
    OperationFailed("operationFailed"),
    RoomNotFound("roomNotFound"),
    ServerNotFound("serverNotFound"),
    ServiceOffline("serviceOffline"),
    SuccessResponse("success"),
    WrongPassword("wrongPassword"),
    AlreadyJoined("alreadyJoined"),;

    private final String responseMessage;

    ServerResponse(final String paramResponse) {
        responseMessage = paramResponse;
    }

    @Override
    public String toString() {
        return responseMessage;
    }
}