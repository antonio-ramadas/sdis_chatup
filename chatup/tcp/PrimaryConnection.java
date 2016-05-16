package chatup.tcp;

import chatup.server.ServerKeystore;

import java.io.IOException;

public class PrimaryConnection extends SSLConnection {

    private PrimaryTcpDispatcher dispatcher;

    public PrimaryConnection(short paramPort, ServerKeystore serverKeystore, PrimaryTcpDispatcher dispatcher) throws IOException {
        super(paramPort, serverKeystore);
        this.dispatcher = dispatcher;
    }

    public void handle(TcpMessage message) {

    }
}
