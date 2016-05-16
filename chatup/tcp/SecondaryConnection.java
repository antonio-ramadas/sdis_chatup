package chatup.tcp;

import chatup.server.ServerKeystore;

import java.io.IOException;

public class SecondaryConnection extends SSLConnection {

    public SecondaryConnection(short paramPort, ServerKeystore serverKeystore) throws IOException {
        super(paramPort, serverKeystore);
    }

    @Override
    public void handle(TcpMessage message){

    }
}