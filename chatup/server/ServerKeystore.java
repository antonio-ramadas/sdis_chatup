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

    private String filePath;
    private String keystorePassword;
    private KeyManagerFactory kmf;
    private TrustManagerFactory tmf;

    public ServerKeystore(final String filePath, final String keystorePassword)
            throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {

        this.filePath = filePath;
        this.keystorePassword = keystorePassword;
        KeyStore keystore = KeyStore.getInstance("JKS");
        FileInputStream fileIn = new FileInputStream(filePath);
        keystore.load(fileIn, keystorePassword.toCharArray());

        kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keystore, keystorePassword.toCharArray());

        tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(keystore);
    }

    public KeyManagerFactory getKeyManager() { return kmf; }
    public TrustManagerFactory getTrustManager() { return tmf; }
    public String getPassword() { return keystorePassword; }
    public String getPath() { return filePath; }

}
