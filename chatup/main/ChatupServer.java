package chatup.main;

import chatup.server.Server;
import chatup.server.ServerInfo;
import chatup.server.ServerKeystore;

public class ChatupServer {

    private static Server serverInstance;
    private static ServerKeystore serverKeystore;

    static boolean initializePrimary(int httpPort, int tcpPort) {

        boolean exceptionThrown = false;

        try {
            serverKeystore = new ServerKeystore("server.jks", "123456");
            serverInstance = new chatup.server.PrimaryServer(tcpPort, httpPort);
        }
        catch (Exception ex) {
            System.out.println("Exception caught: " + ex.getMessage() + " in ChatupServer.contructor");
            exceptionThrown = true;
        }

        return exceptionThrown;
    }

    static boolean initializeSecondary(int serverId, final ServerInfo primaryServer, int httpPort, int tcpPort) {

        boolean exceptionThrown = false;

        try {
            serverKeystore = new ServerKeystore("server.jks", "123456");
            serverInstance = new chatup.server.SecondaryServer(serverId, primaryServer, tcpPort, httpPort);
        }
        catch (Exception ex) {
            System.out.println("Exception caught: " + ex.getMessage() + " in ChatupServer.contructor");
            exceptionThrown = true;
        }

        return exceptionThrown;
    }

    public static Server getInstance() {
        return serverInstance;
    }
}