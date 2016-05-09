package chatup.server;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class ServerKeystore {

    private KeyManagerFactory kmf;
    private TrustManagerFactory tmf;

    public ServerKeystore(final String filePath, final String storePass, final String keyPass)
            throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {

        FileInputStream fileIn = new FileInputStream(filePath);
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(fileIn, storePass.toCharArray());

        kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keystore, keyPass.toCharArray());

        tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(keystore);
    }

    public KeyManagerFactory getKeyManager() { return kmf; }
    public TrustManagerFactory getTrustManager() { return tmf; }

}
