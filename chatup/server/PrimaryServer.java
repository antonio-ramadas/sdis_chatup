package chatup.server;

import chatup.backend.PrimaryDispatcher;

import java.util.HashMap;

public class PrimaryServer extends Server{

    private final HashMap<String, String> users = new HashMap<>();

    public PrimaryServer(short paramPort, short tcpPort) {
        super(new PrimaryDispatcher(), paramPort, tcpPort);
    }

    @Override
    public ServerType getType() {
        return ServerType.PRIMARY;
    }
}