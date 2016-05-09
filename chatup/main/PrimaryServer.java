package chatup.main;

import chatup.server.Server;
import chatup.server.ServerKeystore;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class PrimaryServer{

    public static void main(String[] args) {

        ServerKeystore serverKeystore = null;
        ChatupServer.initialize(new chatup.server.PrimaryServer((short)8085, (short)8087));

        try {
            serverKeystore = new ServerKeystore("/home/pedro/Desktop/SDIS/chatup.jks", "sdis1516");
            primaryServer = new chatup.server.PrimaryServer(serverKeystore, (short) 8085, (short) 8087);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        }

    }
}