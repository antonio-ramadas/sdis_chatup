package chatup.main;

import chatup.server.Server;
import chatup.server.ServerInfo;
import chatup.server.ServerKeystore;

public class ChatupServer {

    private static Server serverInstance;
    private static ServerKeystore serverKeystore;

    static void initializePrimary(int httpPort, int tcpPort) {

        try {
            serverKeystore = new ServerKeystore("server.jks", "123456");
            serverInstance = new chatup.server.PrimaryServer(tcpPort, httpPort);
        }
        catch (Exception ex) {
            System.out.println("Exception caught: " + ex.getMessage() + " in ChatupServer.contructor");
            System.exit(1);
        }
    }

    static void initializeSecondary(final ServerInfo primaryServer, int httpPort, int tcpPort) {

        try {
            serverKeystore = new ServerKeystore("server.jks", "123456");
            serverInstance = new chatup.server.SecondaryServer(primaryServer, tcpPort, httpPort);
        }
        catch (Exception ex) {
            System.out.println("Exception caught: " + ex.getMessage() + " in ChatupServer.contructor");
            System.exit(1);
        }
    }

    public static Server getInstance() {
        return serverInstance;
    }
}