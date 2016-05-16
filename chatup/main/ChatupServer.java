package chatup.main;

import chatup.server.Server;
import chatup.server.ServerInfo;
import chatup.server.ServerKeystore;
import chatup.server.ServerType;
import chatup.tcp.PrimaryConnection;
import chatup.tcp.SSLConnection;
import chatup.tcp.SecondaryConnection;
import chatup.tcp.TcpMessage;

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
    private static PrimaryConnection primaryConnection;
    private static SecondaryConnection secondaryConnection;

    static boolean initializePrimary(short httpPort, short tcpPort) {

        boolean exceptionThrown = false;

        try {
            serverKeystore = new ServerKeystore("server.jks", "123456");
            serverType = ServerType.PRIMARY;
            primaryConnection = new PrimaryConnection(tcpPort, serverKeystore);
            serverInstance = new chatup.server.PrimaryServer(primaryConnection, httpPort);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            exceptionThrown = true;
        }
        catch (SQLException ex) {
            ex.printStackTrace();
            exceptionThrown = true;
        }
        catch (KeyStoreException | UnrecoverableKeyException | CertificateException ex) {
            ex.printStackTrace();
            exceptionThrown = true;
        }
        catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            exceptionThrown = true;
        }

        return exceptionThrown;
    }

    static boolean initializeSecondary(final ServerInfo primaryServer, short httpPort, short tcpPort) {

        boolean exceptionThrown = false;

        try {
            serverKeystore = new ServerKeystore("server.jks", "123456");
            serverType = ServerType.PRIMARY;
            serverInstance = new chatup.server.SecondaryServer(serverKeystore, primaryServer, httpPort, tcpPort);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            exceptionThrown = true;
        }
        catch (SQLException ex) {
            ex.printStackTrace();
            exceptionThrown = true;
        }
        catch (KeyStoreException | UnrecoverableKeyException | CertificateException ex) {
            ex.printStackTrace();
            exceptionThrown = true;
        }
        catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            exceptionThrown = true;
        }

        return exceptionThrown;
    }

    public static ServerType getType() {
        return serverType;
    }

    public static Server getInstance() {
        return serverInstance;
    }
}