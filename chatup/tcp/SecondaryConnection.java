package chatup.tcp;

import chatup.server.ServerKeystore;

import java.io.IOException;

public class SecondaryConnection extends SSLConnection {

    private SecondaryTcpDispatcher dispatcher;

    public SecondaryConnection(short paramPort, ServerKeystore serverKeystore, SecondaryTcpDispatcher dispatcher) throws IOException {
        super(paramPort, serverKeystore);
        this.dispatcher = dispatcher;
    }

    public void handle(TcpMessage message){

    }
}
