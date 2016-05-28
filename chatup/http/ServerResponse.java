package chatup.http;

public enum ServerResponse {

    AlreadyJoined("alreadyJoined"),
    AuthenticationFailed("authenticationFailed"),
    DatabaseError("databaseError"),
    InvalidOperation("unexpectedCommand"),
    InvalidMethod("invalidMethod"),
    InvalidService("invalidService"),
    InvalidToken("invalidToken"),
    MissingParameters("missingParameters"),
    OperationFailed("operationFailed"),
    RoomExists("roomExists"),
    RoomNotFound("roomNotFound"),
    ServerNotFound("serverNotFound"),
    ServiceOffline("serviceOffline"),
    SessionExists("sessionExists"),
    SuccessResponse("success"),
    WrongPassword("wrongPassword");

    private final String responseMessage;

    ServerResponse(final String paramResponse) {
        responseMessage = paramResponse;
    }

    @Override
    public String toString() {
        return responseMessage;
    }
}