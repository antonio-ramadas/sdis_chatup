package chatup.tcp;

import chatup.server.ServerKeystore;

import java.io.IOException;

public class PrimaryConnection extends SSLConnection {

    public PrimaryConnection(short paramPort, ServerKeystore serverKeystore) throws IOException {
        super(paramPort, serverKeystore);
    }

    @Override
    public void handle(TcpMessage message) {
      switch(message.getType()){
            case LeaveRoom:
                LeaveRoom leave = (LeaveRoom) message;
                break;
          default:
              break;
        }
    }
}
