package chatup.main;

import chatup.model.Message;
import chatup.model.MessageCache;

class PrimaryServer {

    public static void main(final String[] args) {

        if (args.length > 2) {
            ChatupGlobals.usage("(<tcpPort>) (<httpPort>)");
        }

        short tcpPort = ChatupGlobals.DefaultTcpPort;

        if (args.length > 0) {

            try {
                tcpPort = Short.parseShort(args[0]);
            }
            catch (final NumberFormatException ex) {
                tcpPort = ChatupGlobals.DefaultTcpPort;
            }
        }

        short httpPort = ChatupGlobals.DefaultHttpPort;

        if (args.length > 1) {

            try {
                httpPort = Short.parseShort(args[1]);
            }
            catch (final NumberFormatException ex) {
                httpPort = ChatupGlobals.DefaultHttpPort;
            }
        }

        ChatupServer.initializePrimary(httpPort, tcpPort);
    }
}