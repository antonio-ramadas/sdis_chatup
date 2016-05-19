package chatup.main;

import chatup.server.*;

import java.io.IOException;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import java.sql.SQLException;

public class ChatupServer {

    private static Server serverInstance;
    private static ServerKeystore serverKeystore;

    static boolean initializePrimary(int httpPort, int tcpPort) {

        boolean exceptionThrown = false;

        try {
            serverKeystore = new ServerKeystore("server.jks", "123456");
           // primaryConnection = new PrimaryConnection(tcpPort, serverKeystore);
            serverInstance = new chatup.server.PrimaryServer(tcpPort, httpPort);
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

    static boolean initializeSecondary(int serverId, final ServerInfo primaryServer, int httpPort, int tcpPort) {

        boolean exceptionThrown = false;

        try {
            serverKeystore = new ServerKeystore("server.jks", "123456");
            serverInstance = new chatup.server.SecondaryServer(serverId, primaryServer, tcpPort, httpPort);
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

    public static Server getInstance() {
        return serverInstance;
    }
}