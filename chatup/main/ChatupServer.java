package chatup.main;

import chatup.server.Server;
import chatup.server.ServerType;

public class ChatupServer {

    private static Server serverInstance;
    private static ServerType serverType;

    public static void initialize(final Server myServer) {

        serverInstance = myServer;

        if (myServer instanceof chatup.server.PrimaryServer) {
            serverType = ServerType.PRIMARY;
        }
        else if (myServer instanceof chatup.server.SecondaryServer) {
            serverType = ServerType.SECONDARY;
        }
    }

    public static Server getInstance() {
        return serverInstance;
    }

    public static ServerType getType() {
        return serverType;
    }
}