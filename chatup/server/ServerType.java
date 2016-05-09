package chatup.server;

public enum ServerType {

    PRIMARY("Primary"),
    SECONDARY("Secondary");

    private final String serverType;

    ServerType(final String paramType) {
        serverType = paramType;
    }

    public final String getType() {
        return serverType;
    }
}