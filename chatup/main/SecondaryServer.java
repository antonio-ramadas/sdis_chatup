package chatup.main;

import chatup.server.ServerInfo;

public class SecondaryServer {

    public static void main(String[] args) {

        if (args.length < 2 || args.length > 4) {
            System.out.println("USAGE: SecondaryServer <serverId> primaryIp:primaryPort (<tcpPort>) (<httpPort>)");
            System.exit(1);
        }

        int serverId = 0;
        int primaryIndex = 1;
        int tcpPort = ChatupGlobals.DefaultTcpPort;

        try {
            serverId = Integer.parseInt(args[0]);
        }
        catch (NumberFormatException ex) {
            System.out.println("USAGE: SecondaryServer <serverId> (<tcpPort>) (<httpPort>) primaryIp:primaryPort");
            System.exit(1);
        }

        if (args.length > 2) {

            try {
                tcpPort = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException ex) {
                tcpPort = ChatupGlobals.DefaultTcpPort;
            }

            primaryIndex++;
        }

        short httpPort = ChatupGlobals.DefaultHttpPort;

        if (args.length > 3) {

            try {
                httpPort = Short.parseShort(args[2]);
            }
            catch (NumberFormatException ex) {
                httpPort = ChatupGlobals.DefaultHttpPort;
            }

            primaryIndex++;
        }

        int separatorPosition = args[primaryIndex].indexOf(':');
        final String addressString = args[primaryIndex].substring(0, separatorPosition);

        try {

            final ServerInfo primaryServer = new ServerInfo(
                addressString,
                Integer.parseInt(args[primaryIndex].substring(separatorPosition + 1))
            );

           ChatupServer.initializeSecondary(serverId, primaryServer, httpPort, tcpPort);
        }
        catch (final NumberFormatException ex) {
            System.err.println("invalid primary server port, terminating application...");
            System.exit(1);
        }
    }
}