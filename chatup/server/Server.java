package chatup.server;

import chatup.room.Room;
import com.eclipsesource.json.Json;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.util.Pair;

import java.io.*;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.Executors;

public abstract class Server {

    private HttpServer httpServer;
    private SSLServerSocket tcpSocket;

    private short httpPort;
    private short tcpPort;
    private int sequenceRoom = 0;

    protected final HashMap<Integer, Room> rooms = new HashMap<>();
    protected final HashMap<Integer, ServerInfo> servers = new HashMap<>();

    public Server(final HttpHandler httpHandler, short httpPort, short tcpPort) {

        this.httpPort = httpPort;
        this.tcpPort = tcpPort;

        try {
            httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);
            httpServer.createContext("/", httpHandler);
            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.start();
        }
        catch (IOException ex) {
            System.out.println("Exception caught: " + ex.getMessage() + " in Server.contructor");
        }

        try {
            SSLServerSocketFactory socketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            tcpSocket = (SSLServerSocket) socketFactory.createServerSocket(tcpPort);
        }
        catch (IOException ex) {
            System.out.println("Exception caught: " + ex.getMessage() + " in Server.contructor");
        }

        ReceiveThread receiveThread = new ReceiveThread(tcpSocket);
        receiveThread.run();
    }

    public short getHttpPort() { return httpPort; }
    public short getTcpPort() { return tcpPort; }
    public SSLServerSocket getServerSocket() { return tcpSocket; }

    protected void sendTCPMessage(final InetAddress hostAddress, short hostPort, final String message) {

        try (final SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(hostAddress, hostPort);
             final OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
             final BufferedWriter bw = new BufferedWriter(out)) {
            bw.write(message);
            bw.flush();
        }
        catch (IOException ex) {
            System.out.println("Exception caught: " + ex.getMessage() + " in Server.contructor");
        }
    }

    public boolean removeServer(int serverId) {

        if (!servers.containsKey(serverId)) {
            return false;
        }

        servers.remove(serverId);

        return true;
    }

    public boolean deleteRoom(int roomId, final String userToken) {

        System.out.println("roomId:" + roomId);
        System.out.println("token:" + userToken);

        if (!rooms.containsKey(roomId)) {
            return false;
        }

        final Room selectedRoom = rooms.get(roomId);

        if (!selectedRoom.getOwner().equals(userToken)) {
            return false;
        }

        rooms.remove(roomId);

        return true;
    }

    public boolean createRoom(final String roomName, final String roomPassword, final String roomOwner) {

        System.out.println("roomName:" + roomName);
        System.out.println("roomOwner:" + roomOwner);

        if (roomPassword == null) {
            rooms.put(++sequenceRoom, new Room(roomName, roomOwner));
        }
        else {
            System.out.println("roomPassword:" + roomPassword);
            rooms.put(++sequenceRoom, new Room(roomName, roomPassword, roomOwner));
        }

        return true;
    }

    public boolean joinRoom(int roomId, final String userToken) {

        System.out.println("roomId:" + roomId);
        System.out.println("token:" + userToken);

        if (!rooms.containsKey(roomId)) {
            return false;
        }

        final Room selectedRoom = rooms.get(roomId);
        final String userEmail = "marques999@gmail.com";

        //users.get(userToken);

        if (userEmail == null || selectedRoom == null) {
            return false;
        }

        if (selectedRoom.registerUser(new Pair<>(userToken, userEmail))) {
            return true;
        }

        return false;
    }

    public boolean userLogin(String userEmail, String userToken) {

        System.out.println("email:" + userEmail);
        System.out.println("token:" + userToken);

        return true;
    }

    public boolean userLogout(String userEmail, String userToken) {

        System.out.println("email:" + userEmail);
        System.out.println("token:" + userToken);

        return true;
    }

    public final String getRooms() {
        return Json.object().add("nothing", 0).toString();
    }

    private class ReceiveThread extends Thread {

        private SSLServerSocket tcpSocket;

        public ReceiveThread(SSLServerSocket tcpSocket){
            this.tcpSocket = tcpSocket;
        }

        public void run(){

            System.out.println("Server is now listening for TCP messages: ");

            while(true) {

                try (final SSLSocket socket = (SSLSocket) tcpSocket.accept();
                     final InputStreamReader in = new InputStreamReader(socket.getInputStream());
                     final BufferedReader br = new BufferedReader(in)) {
                        String message = null;
                        while ((message = br.readLine()) != null) {
                           // TODO: redirect message to dispatcher
                        }
                }
                catch (IOException ex) {
                    System.out.println("Exception caught: " + ex.getMessage() + " in ReceiveThread.run");
                }
            }
        }
    }

    public abstract ServerType getType();
}