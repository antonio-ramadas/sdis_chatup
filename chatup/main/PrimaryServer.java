package chatup.main;

public class PrimaryServer {

    public static void main(String[] args) {
        ChatupServer.initialize(new chatup.server.PrimaryServer((short)8085, (short)8087));
    }
}