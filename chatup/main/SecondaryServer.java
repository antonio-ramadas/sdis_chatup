package chatup.main;

import chatup.server.ServerInfo;
import chatup.server.ServerType;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class SecondaryServer {

    private static final int defaultHttpPort = 8080;
    private static final int defaultTcpPort = 8085;

    private static ArrayList<ServerInfo> primaryServers = new ArrayList<>();

    public static void main(String[] args) {

        if (args.length < 1 || args.length > 3) {
            System.out.println("USAGE: SecondaryServer (<tcpPort>) (<httpPort>) primaryIp:primaryPort");
            System.exit(1);
        }

        int primaryIndex = 0;
        short tcpPort = defaultTcpPort;

        if (args.length > 1) {

            try {
                tcpPort = Short.parseShort(args[0]);
            }
            catch (NumberFormatException ex) {
                tcpPort = defaultTcpPort;
            }

            primaryIndex++;
        }

        short httpPort = defaultHttpPort;

        if (args.length == 3) {

            try {
                httpPort = Short.parseShort(args[1]);
            }
            catch (NumberFormatException ex) {
                httpPort = defaultHttpPort;
            }

            primaryIndex++;
        }

        // final ArrayList<ServerInfo> primaryServers = new ArrayList<>();
        // for (int i = 0; i < args.length; i++) {

        int separatorPosition = args[primaryIndex].indexOf(':');
        short primaryPort = 0;

        String addressString = args[primaryIndex].substring(0, separatorPosition);
        InetAddress serverAddress = null;

        try {
            serverAddress = InetAddress.getByName(addressString);
            primaryPort = Short.parseShort(args[primaryIndex].substring(separatorPosition + 1));
        }
        catch (final NumberFormatException ex) {
            System.err.println("invalid primary server port, terminating application...");
            System.exit(1);
        }
        catch (final UnknownHostException ex) {
            System.err.println("invalid primary server address, terminating application...");
            System.exit(1);
        }
        // TODO : change 5 as first argument -> should be id
       // primaryServers.add(new ServerInfo((int)5, serverAddress, serverPort));

        ChatupServer.initialize(ServerType.SECONDARY, httpPort, tcpPort);
    }
}