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

public class ServerKeystore{

    private final String keystorePath;
    private final String keystorePassword;
    private final KeyManagerFactory kmf;
    private final TrustManagerFactory tmf;

    public ServerKeystore(final String filePath, final String myPassword)
        throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {

        final KeyStore keystore = KeyStore.getInstance("JKS");
        final FileInputStream fileIn = new FileInputStream(filePath);

        keystorePath = filePath;
        keystorePassword = myPassword;
        keystore.load(fileIn, keystorePassword.toCharArray());
        kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keystore, keystorePassword.toCharArray());
        tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(keystore);
    }

    public final KeyManagerFactory getKeyManager() {
        return kmf;
    }

    public final TrustManagerFactory getTrustManager() {
        return tmf;
    }

    public final String getPassword() {
        return keystorePassword;
    }

    public final String getPath() {
        return keystorePath;
    }
}