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

    private final KeyManagerFactory kmf;

    public ServerKeystore(final String keystorePath, final String keystorePassword) throws IOException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, CertificateException {
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(new FileInputStream(keystorePath), keystorePassword.toCharArray());
        kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keystore, keystorePassword.toCharArray());
    }

    final KeyManagerFactory getKeyManager() {
        return kmf;
    }
}