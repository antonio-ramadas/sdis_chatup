package chatup.tcp;

import chatup.server.ServerKeystore;

import java.io.IOException;


public class SecondaryConnection extends SSLConnection {

    public SecondaryConnection(short paramPort, ServerKeystore serverKeystore) throws IOException {
        super(paramPort, serverKeystore);
    }

    @Override
    public void handle(TcpMessage message){
        switch(message.getType()){
            case ReplaceServer:
                ReplaceServer replace = (ReplaceServer) message;
                break;
            case DeleteServer:
                DeleteServer delete = (DeleteServer) message;
                break;
            case CreateRoom:
                CreateRoom create = (CreateRoom) message;
                break;
            case JoinRoom:
                JoinRoom join = (JoinRoom) message;
                break;
            case LeaveRoom:
                LeaveRoom leave = (LeaveRoom) message;
                break;
            case UserDisconnect:
                UserDisconnect disconnect = (UserDisconnect) message;
                break;
            default:
                break;
        }
    }
}