package chatup.main;

import chatup.server.Server;
import chatup.server.ServerKeystore;
import chatup.server.ServerType;

import java.io.IOException;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import java.sql.SQLException;

public class ChatupServer {

    private static Server serverInstance;
    private static ServerKeystore serverKeystore;
    private static ServerType serverType;

    static void initialize(final ServerType paramType, short httpPort, short tcpPort) {

        try {
            serverKeystore = new ServerKeystore("server.jks", "123456");
            serverType = paramType;

            if (serverType == ServerType.PRIMARY) {
                serverInstance = new chatup.server.PrimaryServer(serverKeystore, httpPort, tcpPort);
            }
            else {
                serverInstance = new chatup.server.SecondaryServer(serverKeystore, httpPort, tcpPort);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        catch (KeyStoreException e) {
            e.printStackTrace();
        }
        catch (CertificateException e) {
            e.printStackTrace();
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        }
    }

    public static Server getInstance() {
        return serverInstance;
    }
}