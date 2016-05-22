package chatup.server;

import chatup.http.HttpFields;
import chatup.http.ServerResponse;
import chatup.model.Database;
import chatup.model.Message;
import chatup.model.MessageCache;
import chatup.model.Room;

import chatup.tcp.SendMessage;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.Json;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.Executors;

public abstract class Server {

    final Database serverDatabase = Database.getInstance();
    final HashMap<Integer, Room> rooms = new HashMap<>();
    final HashMap<Integer, ServerInfo> servers = new HashMap<>();
    final HashMap<String, String> users = new HashMap<>();

    private HttpServer httpServer;
    private ServerType serverType;
    private ServerLogger serverLogger;

    private int httpPort;

    Server(final HttpHandler httpHandler, final ServerType paramType, int httpPort) throws SQLException {

        this.httpPort = httpPort;
        serverLogger = new ServerLogger(this);
        serverType = paramType;

        try {
            //   final KeyStore serverKeystore = tcpConnection.getServerKeystore();
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
            System.out.println("Exception caught: " + ex.getMessage() + " in KryoServer.contructor");
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
    }

    public ServerLogger getLogger() {
        return serverLogger;
    }

    public ServerType getType() {
        return serverType;
    }

    public int getId() {
        return -1;
    }

    /*
     * SERVER MESSAGES
     */
    public ServerResponse insertServer(int serverId, final String serverAddress, int serverPort) {
        throw new UnsupportedOperationException("InsertServer");
    }

    public ServerResponse updateServer(int serverId, final String serverAddress, int serverPort) {
        throw new UnsupportedOperationException("UpdateServer");
    }

    public ServerResponse deleteServer(int serverId) {
        throw new UnsupportedOperationException("DeleteServer");
    }

    /*
     * USER SERVICE PROTOCOL
     */

    public ServerResponse userLogin(final String userToken, final String userEmail) {
        throw new UnsupportedOperationException("UserLogin");
    }

    public ServerResponse userDisconnect(final String userToken, final String userEmail) {
        throw new UnsupportedOperationException("UserLogin");
    }

    /*
     * ROOM SERVICE PROTOCOL
     */

    public ServerResponse createRoom(final String roomName, final String roomPassword, final String roomOwner) {
        throw new UnsupportedOperationException("CreateRoom");
    }

    public ServerResponse deleteRoom(int roomId) {
        throw new UnsupportedOperationException("DeleteRoom");
    }

    public ServerResponse joinRoom(int roomId, final String userToken) {
        throw new UnsupportedOperationException("JoinRoom");
    }

    public ServerResponse joinRoom(int roomId, final String userEmail, final String userToken) {
        throw new UnsupportedOperationException("JoinRoom");
    }

    public ServerResponse leaveRoom(int roomId, final String userToken) {
        throw new UnsupportedOperationException("LeaveRoom");
    }

    public ServerResponse syncRoom(int roomId) {
        throw new UnsupportedOperationException("SyncRoom");
    }

    public ServerResponse syncRoom(int roomId, final MessageCache messageCache) {
        throw new UnsupportedOperationException("SyncRoom");
    }

    public final JsonValue getRooms() {

        final JsonValue newArray = Json.array();

        rooms.forEach((k, v) -> newArray.asArray()
            .add(Json.object()
            .add(HttpFields.RoomName, v.getName())
            .add(HttpFields.UserToken, v.getOwner())
            .add(HttpFields.RoomPrivate, v.isPrivate())
            .add(HttpFields.RoomId, k)
        ));

        return newArray;
    }

    /*
     * MESSAGE SERVICE PROTOCOL
     */

    public MessageCache getMessages(final String userToken, int roomId) {
        throw new UnsupportedOperationException("RetrieveMessages");
    }

    public ServerResponse insertMessage(final Message paramMessage) {
        throw new UnsupportedOperationException("InsertMessage");
    }

    public ServerResponse notifyMessage(int roomId, final String userToken, final String msgContents) {
        throw new UnsupportedOperationException("NotifyMessage");
    }
}