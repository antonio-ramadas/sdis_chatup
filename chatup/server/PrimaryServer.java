package chatup.server;

import chatup.http.PrimaryDispatcher;

import java.util.HashMap;

public class PrimaryServer extends Server {

    private final HashMap<String, String> users = new HashMap<>();

    public PrimaryServer(ServerKeystore serverKeystore, short httpPort, short tcpPort) {
        super(serverKeystore, new PrimaryDispatcher(), httpPort, tcpPort);
    }

    @Override
    public ServerType getType() {
        return ServerType.PRIMARY;
    }
}