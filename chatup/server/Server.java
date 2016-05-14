package chatup.server;

import chatup.http.HttpFields;
import chatup.user.UserMessage;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.sun.net.httpserver.*;
import chatup.room.Room;
import com.eclipsesource.json.Json;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.util.Pair;
import sun.rmi.transport.tcp.TCPConnection;

import java.io.*;
import javax.net.ssl.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.*;
import java.util.HashMap;
import java.util.concurrent.Executors;

public abstract class Server {

    private HttpServer httpServer;
    private SSLConnection tcpConnection;
    private ServerKeystore serverKeystore;

    private short httpPort;
    private int sequenceRoom = 0;

    protected final HashMap<Integer, Room> rooms = new HashMap<>();
    protected final HashMap<Integer, ServerInfo> servers = new HashMap<>();

    public Server(final ServerKeystore serverKeystore, final HttpHandler httpHandler, short httpPort, short tcpPort) {

        this.httpPort = httpPort;
        this.serverKeystore = serverKeystore;

        try {
            tcpConnection = new SSLConnection(tcpPort, serverKeystore);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        try {
            // TODO: verify if 'TLSv1' is what we really want
            // TODO: clean up this mess (and understand it as well)
         //   final KeyManagerFactory kmf = serverKeystore.getKeyManager();
         //   final TrustManagerFactory tmf = serverKeystore.getTrustManager();

            httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);

        /*    final SSLContext sslContext = SSLContext.getInstance("TLSv1");

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            httpServer.setHttpsConfigurator(new HttpsConfigurator(sslContext){
                public void configure(HttpsParameters params) {
                    try {
                        SSLContext c = SSLContext.getDefault();
                        SSLEngine engine = c.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());
                        SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
                        params.setSSLParameters(defaultSSLParameters);
                    }
                    catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
            });*/

            httpServer.createContext("/", httpHandler);
            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.start();
        }
        catch (IOException ex) {
            System.out.println("Exception caught: " + ex.getMessage() + " in Server.contructor");
        }
      /*  catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        catch (KeyManagementException e) {
            e.printStackTrace();
        }
*/
        createRoom("Justin Bieber", null, "bca7cd6bdaf6efaf7ae8g5130ae76f8a");
        createRoom("XXX NAZIS XXX", "femnazi", "bca7cd6bdaf6efaf7ae8g5130ae76f8a");
        createRoom("MigaxPraSempre", null, "bca7cd6bdaf6efaf7ae8g5130ae76f8a");
        tcpConnection.getThread().start();
    }

    public short getHttpPort() {
        return httpPort;
    }

    public short getTcpPort() {
        return tcpConnection.getPort();
    }

    public SSLServerSocket getServerSocket() {
        return tcpConnection.getSocket();
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

        if (roomPassword == null) {
            rooms.put(++sequenceRoom, new Room(roomName, roomOwner));
        }
        else {
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

        return userEmail.equals("marques999@gmail.com") && userToken.equals("14191091");
    }

    public boolean userLogout(String userEmail, String userToken) {

        System.out.println("email:" + userEmail);
        System.out.println("token:" + userToken);

        return true;
    }

    public final JsonValue getRooms() {

        final JsonValue newArray = Json.array();

        rooms.forEach((k,v) ->
        {
            if (v.isPrivate())
            {
                newArray.asArray().add(Json.object()
                        .add(HttpFields.RoomName, v.getName())
                        .add(HttpFields.UserToken, v.getOwner())
                        .add(HttpFields.RoomPassword, v.getPassword())
                        .add(HttpFields.RoomId, k)
                );
            }
            else
            {
                newArray.asArray().add(Json.object()
                        .add(HttpFields.RoomName, v.getName())
                        .add(HttpFields.UserToken, v.getOwner())
                        .add(HttpFields.RoomId, k)
                );
            }
        });

        return newArray;
    }

    public UserMessage[] retrieveMessages(final String userToken, int roomId) {
        return null;
    }

    public boolean registerMessage(final String userToken, int roomId, final String msgContents) {
        return false;
    }


    public abstract ServerType getType();
}