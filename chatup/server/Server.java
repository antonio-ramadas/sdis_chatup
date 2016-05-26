package chatup.server;

import chatup.http.HttpDispatcher;
import chatup.http.ServerResponse;
import chatup.main.ChatupGlobals;
import chatup.model.Message;

import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javafx.util.Pair;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.Executors;

public abstract class Server {

    final HashMap<String, String> users = new HashMap<>();
    final ServerType serverType;

    Server(final HttpHandler httpHandler, final ServerType paramType, int httpPort) throws SQLException {

        serverType = paramType;

        try {
            /*
            final KeyStore serverKeystore = tcpConnection.getServerKeystore();
            final KeyManagerFactory kmf = serverKeystore.getKeyManager();
            final TrustManagerFactory tmf = serverKeystore.getTrustManager();
            final SSLContext sslContext = SSLContext.getInstance("TLSv1");

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            httpServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    try {
                        SSLContext c = SSLContext.getDefault();
                        SSLEngine engine = c.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());
                        SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
                        params.setSSLParameters(defaultSSLParameters);
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
            });
            */
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);
            httpServer.createContext("/", httpHandler);
            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.start();
        }
        catch (final IOException ex) {
            ChatupGlobals.abort(serverType, ex);
        }
    }

    public HashMap<String, String> getUsers() {
        return users;
    }

    public ServerType getType() {
        return serverType;
    }

    public int getId() {
        return -1;
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

    public ServerResponse userLogin(final String userEmail, final String userToken) {
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

    public ServerResponse syncRoom(int roomId, int serverId) {
        throw new UnsupportedOperationException("SyncRoom");
    }

    public ServerResponse getMessages(final HttpDispatcher httpExchange, final String userToken, int roomId, long roomTimestamp) {
        throw new UnsupportedOperationException("RetrieveMessages");
    }

    public ServerResponse insertMessage(Message userMessage) {
        throw new UnsupportedOperationException("InsertMessage");
    }

    public abstract boolean validateToken(String getValue);
}