package chatup.main;

public class PrimaryServer{

    public static void main(String[] args) {
        new chatup.server.PrimaryServer((short)8085);
    }
}