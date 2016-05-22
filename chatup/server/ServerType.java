package chatup.server;

public enum ServerType {

    PRIMARY("primary"),
    SECONDARY("secondary");

    ServerType(final String paramType) {
        serverType = paramType;
    }

    private final String serverType;

    @Override
    public String toString() {
        return serverType;
    }
}