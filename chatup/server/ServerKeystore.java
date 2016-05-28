package chatup.server;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import java.io.FileInputStream;

import java.security.KeyStore;

public class ServerKeystore {

    private final KeyManagerFactory kmf;
    private final TrustManagerFactory tmf;

    public ServerKeystore(final String keystorePath, final String keystorePassword) throws Exception {
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(new FileInputStream(keystorePath), keystorePassword.toCharArray());
        kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keystore, keystorePassword.toCharArray());
        tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(keystore);
    }

    final KeyManagerFactory getKeyManager() {
        return kmf;
    }

    final TrustManagerFactory getTrustManager() {
        return tmf;
    }
}