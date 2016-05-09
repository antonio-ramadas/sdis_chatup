package chatup.main;

import chatup.server.ServerType;

class PrimaryServer{

    public static void main(String[] args) {
        ChatupServer.initialize(ServerType.PRIMARY, (short)8085, (short)8087);
    }
}