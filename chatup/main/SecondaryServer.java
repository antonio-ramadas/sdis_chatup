package chatup.main;

import chatup.server.ServerInfo;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class SecondaryServer {

    public static void main(String[] args) {

        if (args.length < 1 || args.length > 3) {
            System.out.println("USAGE: SecondaryServer (<tcpPort>) (<httpPort>) primaryIp:primaryPort");
            System.exit(1);
        }

        int primaryIndex = 0;
        short tcpPort = ChatupGlobals.DefaultTcpPort;

        if (args.length > 1) {

            try {
                tcpPort = Short.parseShort(args[0]);
            }
            catch (NumberFormatException ex) {
                tcpPort = ChatupGlobals.DefaultTcpPort;
            }

            primaryIndex++;
        }

        short httpPort = ChatupGlobals.DefaultHttpPort;

        if (args.length == 3) {

            try {
                httpPort = Short.parseShort(args[1]);
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
                InetAddress.getByName(addressString),
                Short.parseShort(args[primaryIndex].substring(separatorPosition + 1))
            );

           ChatupServer.initializeSecondary(primaryServer, httpPort, tcpPort);
        }
        catch (final NumberFormatException ex) {
            System.err.println("invalid primary server port, terminating application...");
            System.exit(1);
        }
        catch (final UnknownHostException ex) {
            System.err.println("invalid primary server address, terminating application...");
            System.exit(1);
        }
    }
}