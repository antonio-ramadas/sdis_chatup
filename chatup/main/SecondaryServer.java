package chatup.main;

import chatup.server.ServerInfo;
import chatup.server.ServerType;

import java.time.Instant;

public class SecondaryServer {

    public static void main(String[] args) {

        if (args.length < 2 || args.length > 4) {
            ChatupGlobals.usage("<serverId> primaryIp:primaryPort (<tcpPort>) (<httpPort>)");
        }

        int serverId = 0;
        int tcpPort = ChatupGlobals.DefaultTcpPort;

        try {
            serverId = Integer.parseInt(args[0]);
        }
        catch (NumberFormatException ex) {
            ChatupGlobals.abort(ServerType.SECONDARY, ex);
        }

        if (args.length > 2) {

            try {
                tcpPort = Integer.parseInt(args[2]);
            }
            catch (NumberFormatException ex) {
                tcpPort = ChatupGlobals.DefaultTcpPort;
            }
        }

        short httpPort = ChatupGlobals.DefaultHttpPort;

        if (args.length > 3) {

            try {
                httpPort = Short.parseShort(args[3]);
            }
            catch (NumberFormatException ex) {
                httpPort = ChatupGlobals.DefaultHttpPort;
            }
        }

        int separatorPosition = args[1].indexOf(':');
        final String addressString = args[1].substring(0, separatorPosition);

        try {

            final ServerInfo primaryServer = new ServerInfo(
                serverId,
                Instant.now().getEpochSecond(),
                addressString,
                Integer.parseInt(args[1].substring(separatorPosition + 1))
            );

           ChatupServer.initializeSecondary(primaryServer, httpPort, tcpPort);
        }
        catch (final NumberFormatException ex) {
            ChatupGlobals.abort(ServerType.SECONDARY, ex);
        }
    }
}