package chatup.server;

public enum ServerType {

    PRIMARY("Primary"),
    SECONDARY("Secondary");

    ServerType(final String paramType) {
        serverType = paramType;
    }

    private final String serverType;

    @Override
    public String toString() {
        return serverType;
    }
}