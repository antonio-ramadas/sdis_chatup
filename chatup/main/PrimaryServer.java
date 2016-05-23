package chatup.main;

import chatup.model.Message;
import chatup.model.MessageCache;
import google.collections.MinMaxPriorityQueue;

class PrimaryServer {

    public static void main(String[] args) {

        if (args.length > 2) {
            System.err.println("USAGE: chatup.main.PrimaryServer (<tcpPort>) (<httpPort>)");
            System.exit(1);
        }

        short tcpPort = ChatupGlobals.DefaultTcpPort;

        if (args.length > 0) {

            try {
                tcpPort = Short.parseShort(args[0]);
            }
            catch (NumberFormatException ex) {
                tcpPort = ChatupGlobals.DefaultTcpPort;
            }
        }

        short httpPort = ChatupGlobals.DefaultHttpPort;

        if (args.length > 1) {

            try {
                httpPort = Short.parseShort(args[1]);
            }
            catch (NumberFormatException ex) {
                httpPort = ChatupGlobals.DefaultHttpPort;
            }
        }

        ChatupServer.initializePrimary(httpPort, tcpPort);
    }
}