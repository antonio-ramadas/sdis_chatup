package chatup.server;

import chatup.http.*;
import chatup.main.ChatupGlobals;
import chatup.model.Message;

import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.*;

import javafx.util.Pair;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public abstract class AbstractServer {

    final ConcurrentHashMap<String, String> users = new ConcurrentHashMap<>();
    private final ServerType serverType;

    private int mTcpPort;
    private int mHttpPort;

    AbstractServer(final ServerType paramType, int tcpPort, int httpPort) throws SQLException {

        serverType = paramType;
        mTcpPort = tcpPort;
        mHttpPort = httpPort;

        try {

            final HttpServer httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);
         /*   final ServerKeystore serverKeystore = ChatupServer.getKeystore();
            final KeyManagerFactory kmf = serverKeystore.getKeyManager();
            final TrustManagerFactory tmf = serverKeystore.getTrustManager();
            final SSLContext sslContext = SSLContext.getInstance("TLS");

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            httpServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {

                @Override
                public void configure(HttpsParameters params) {

                    try {

                        final SSLContext c = SSLContext.getDefault();
                        final SSLEngine engine = c.createSSLEngine();

                        params.setNeedClientAuth(false);
                        params.setWantClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());

                        final SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();

                        params.setSSLParameters(defaultSSLParameters);
                    }
                    catch (final Exception ex) {
                        ChatupGlobals.abort(serverType, ex);
                    }
                }
            });*/

            if (paramType == ServerType.PRIMARY) {
                httpServer.createContext(ChatupGlobals.HeartbeatServiceUrl, new HeartbeatService());
                httpServer.createContext(ChatupGlobals.RoomServiceUrl, new RoomService());
                httpServer.createContext(ChatupGlobals.UserServiceUrl, new UserService());
            }
            else {
                httpServer.createContext(ChatupGlobals.HeartbeatServiceUrl, new HeartbeatService());
                httpServer.createContext(ChatupGlobals.MessageServiceUrl, new MessageService());
            }

            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.start();
        }
        catch (final Exception ex) {
            ChatupGlobals.abort(serverType, ex);
        }
    }

    public ConcurrentHashMap<String, String> getUsers() {
        return users;
    }

    public ServerType getType() {
        return serverType;
    }

    public int getId() {
        return -1;
    }

    public int getHttpPort() {
        return mHttpPort;
    }

    public int getTcpPort() {
        return mTcpPort;
    }

    public ServerResponse insertServer(final ServerInfo serverInfo) {
        throw new UnsupportedOperationException("InsertServer");
    }

    public ServerResponse updateServer(final ServerInfo serverInfo) {
        throw new UnsupportedOperationException("UpdateServer");
    }

    public ServerResponse deleteServer(int serverId) {
        throw new UnsupportedOperationException("DeleteServer");
    }

    public ServerResponse userLogin(final String userToken) {
        throw new UnsupportedOperationException("UserLogin");
    }

    public ServerResponse userDisconnect(final String userEmail, final String userToken) {
        throw new UnsupportedOperationException("UserLogin");
    }

    public ServerResponse createRoom(final String roomName, final String roomPassword, final String roomOwner) {
        throw new UnsupportedOperationException("CreateRoom");
    }

    public JsonValue getRooms() {
        throw new UnsupportedOperationException("RetrieveRooms");
    }

    public Pair<ServerResponse, ServerInfo> joinRoom(int roomId, final String userEmail, final String userToken) {
        throw new UnsupportedOperationException("JoinRoom");
    }

    public ServerResponse leaveRoom(int roomId, final String userToken) {
        throw new UnsupportedOperationException("LeaveRoom");
    }

    public ServerResponse deleteRoom(int roomId) {
        throw new UnsupportedOperationException("DeleteRoom");
    }

    public ServerResponse getMessages(final HttpExchange httpExchange, final String userToken, int roomId, long roomTimestamp) {
        throw new UnsupportedOperationException("RetrieveMessages");
    }

    public ServerResponse sendMessage(final Message paramMessage) {
        throw new UnsupportedOperationException("SendMessage");
    }

    public boolean validateToken(final String userToken) {
        throw new UnsupportedOperationException("ValdiateToken");
    }

    public ServerResponse sendMessage(int roomId, String userToken, String messageBody) {
        throw new UnsupportedOperationException("SendMessage");
    }

    public String getEmail(String userToken) {

        final String userEmail = users.get(userToken);

        if (userEmail == null) {
            return userToken;
        }

        return userEmail;
    }
}