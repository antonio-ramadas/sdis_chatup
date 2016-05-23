package chatup.server;

import chatup.http.ServerResponse;
import chatup.model.Message;
import chatup.model.MessageCache;
import chatup.model.Room;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.Executors;

public abstract class Server {

    final HashMap<String, String> users = new HashMap<>();
    private ServerType serverType;

    Server(final HttpHandler httpHandler, final ServerType paramType, int httpPort) throws SQLException {

        final HttpServer httpServer;

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
    }

    public ServerType getType() {
        return serverType;
    }

    public int getId() {
        return -1;
    }

    //---------------------------------------------------------------
    // SERVER: insertServer, updateServer, deleteServer
    //---------------------------------------------------------------

    public ServerResponse insertServer(final ServerInfo serverInfo) {
        throw new UnsupportedOperationException("InsertServer");
    }

    public ServerResponse updateServer(final ServerInfo serverInfo) {
        throw new UnsupportedOperationException("UpdateServer");
    }

    public ServerResponse deleteServer(int serverId) {
        throw new UnsupportedOperationException("DeleteServer");
    }

    //--------------------------------
    // USER: userLogin, userDisconnect
    //---------------------------------

    public ServerResponse userLogin(final String userToken, final String userEmail) {
        throw new UnsupportedOperationException("UserLogin");
    }

    public ServerResponse userDisconnect(final String userToken, final String userEmail) {
        throw new UnsupportedOperationException("UserLogin");
    }

    //---------------------------------------------------------
    // ROOM(PRIMARY): createRoom, getRooms, joinRoom, leaveRoom
    //---------------------------------------------------------

    public ServerResponse createRoom(final String roomName, final String roomPassword, final String roomOwner) {
        throw new UnsupportedOperationException("CreateRoom");
    }

    public JsonValue getRooms() {
        throw new UnsupportedOperationException("RetrieveRooms");
    }

    public ServerResponse joinRoom(int roomId, final String userEmail, final String userToken) {
        throw new UnsupportedOperationException("JoinRoom");
    }

    public ServerResponse leaveRoom(int roomId, final String userToken) {
        throw new UnsupportedOperationException("LeaveRoom");
    }

    //--------------------------------------------------
    // ROOM(SECONDARY): deleteRoom, syncRoom, updateRoom
    //--------------------------------------------------

    public ServerResponse deleteRoom(int roomId) {
        throw new UnsupportedOperationException("DeleteRoom");
    }

    public ServerResponse syncRoom(int roomId, int serverId) {
        throw new UnsupportedOperationException("SyncRoom");
    }

    public ServerResponse syncRoom(int roomId, final MessageCache messageCache) {
        throw new UnsupportedOperationException("SyncRoom");
    }

    public ServerResponse updateRoom(final Room updateRoom) {
        throw new UnsupportedOperationException("UpdateRoom");
    }

    //---------------------------------------------------
    // MESSAGE: getMessages, insertMessage, notifyMessage
    //---------------------------------------------------

    public JsonValue getMessages(final String userToken, int roomId) {
        throw new UnsupportedOperationException("RetrieveMessages");
    }

    public ServerResponse insertMessage(final Message paramMessage) {
        throw new UnsupportedOperationException("InsertMessage");
    }

    public ServerResponse notifyMessage(int roomId, final String userToken, final String msgContents) {
        throw new UnsupportedOperationException("NotifyMessage");
    }
}