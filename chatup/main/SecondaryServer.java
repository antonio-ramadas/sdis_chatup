package chatup.main;

import chatup.server.ServerInfo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class SecondaryServer {

    public static void main(String[] args) throws UnknownHostException {

        if (args.length < 1) {
            System.out.println("USAGE: SecondaryServer ip:port (ip:port...)");
            System.exit(1);
        }

        final ArrayList<ServerInfo> primaryServers = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {

            int separatorPosition = args[i].indexOf(':');
            short serverPort = 0;

            String addressString = args[i].substring(0, separatorPosition);
            InetAddress serverAddress = null;

            try {
                serverAddress = InetAddress.getByName(addressString);
                serverPort = Short.parseShort(args[i].substring(separatorPosition + 1));
            }
            catch (final NumberFormatException ex) {
                System.err.println("invalid primary server port, terminating application...");
                System.exit(1);
            }
            catch (final UnknownHostException ex) {
                System.err.println("invalid primary server address, terminating application...");
                System.exit(1);
            }

            //primaryServers.add(new ServerInfo(serverAddress, serverPort, 0));
        }

        for (final ServerInfo server : primaryServers) {
            System.out.println("server address : " + server.getAddress() + ", server port : " + server.getTcpPort());
        }

        final ServerLogger loggerInstance = ServerLogger.getInstance("myServer");

        loggerInstance.error("abcd");
        loggerInstance.information("abcd");
    }
}
