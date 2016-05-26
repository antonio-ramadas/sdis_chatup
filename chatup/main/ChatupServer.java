package chatup.main;

import chatup.server.Server;
import chatup.server.ServerInfo;
import chatup.server.ServerKeystore;
import chatup.server.ServerType;

public class ChatupServer {

    private static Server serverInstance;
    private static ServerKeystore serverKeystore;

    static void initializePrimary(int httpPort, int tcpPort) {

        try {
            serverKeystore = new ServerKeystore("server.jks", "123456");
            serverInstance = new chatup.server.PrimaryServer(tcpPort, httpPort);
        }
        catch (final Exception ex) {
            ChatupGlobals.abort(ServerType.PRIMARY, ex);
        }
    }

    static void initializeSecondary(final ServerInfo primaryServer, int httpPort, int tcpPort) {

        try {
            serverKeystore = new ServerKeystore("server.jks", "123456");
            serverInstance = new chatup.server.SecondaryServer(primaryServer, tcpPort, httpPort);
        }
        catch (final Exception ex) {
            ChatupGlobals.abort(ServerType.SECONDARY, ex);
        }
    }

    public static Server getInstance() {
        return serverInstance;
    }
}