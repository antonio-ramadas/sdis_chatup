package chatup.main;

import chatup.server.ServerType;

class PrimaryServer {

    private static final int defaultHttpPort = 8080;
    private static final int defaultTcpPort = 8085;

    public static void main(String[] args) {

        if (args.length > 2) {
            System.err.println("USAGE: chatup.PrimaryServer (<tcpPort>) (<httpPort>)");
            System.exit(1);
        }

        short tcpPort = defaultTcpPort;

        if (args.length > 0) {

            try {
                tcpPort = Short.parseShort(args[0]);
            }
            catch (NumberFormatException ex) {
                tcpPort = defaultTcpPort;
            }
        }

        short httpPort = defaultHttpPort;

        if (args.length > 1) {

            try {
                httpPort = Short.parseShort(args[1]);
            }
            catch (NumberFormatException ex) {
                httpPort = defaultHttpPort;
            }
        }

        ChatupServer.initialize(ServerType.PRIMARY, tcpPort, httpPort);
    }
}