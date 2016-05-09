package chatup.server;

import chatup.backend.PrimaryDispatcher;
import chatup.user.UserSession;

import java.util.HashMap;

public class PrimaryServer extends Server {

    private final HashMap<String, UserSession> users = new HashMap<>();
    private final HashMap<Integer, ServerInfo> servers = new HashMap<>();

    public PrimaryServer(short paramPort, short tcpPort) {
        super(new PrimaryDispatcher(), paramPort, tcpPort);
    }
}