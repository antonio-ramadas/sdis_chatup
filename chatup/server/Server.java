package chatup.server;

import com.sun.net.httpserver.*;

import java.io.*;
import javax.net.ssl.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.*;
import java.util.concurrent.Executors;

public abstract class Server {

    private HttpsServer httpServer;
    private SSLServerSocket tcpSocket;
    private ServerKeystore serverKeystore;
    private short httpPort;
    private short tcpPort;

    public Server(final ServerKeystore serverKeystore, final HttpHandler httpHandler, short httpPort, short tcpPort) {

        this.httpPort = httpPort;
        this.tcpPort = tcpPort;
        this.serverKeystore = serverKeystore;

        try {
            // TODO: verify if 'TLSv1' is what we really want
            KeyManagerFactory kmf = serverKeystore.getKeyManager();
            TrustManagerFactory tmf = serverKeystore.getTrustManager();
            httpServer = HttpsServer.create(new InetSocketAddress(httpPort), 0);
            SSLContext sslContext = SSLContext.getInstance("TLSv1");
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

            httpServer.createContext("/", httpHandler);
            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.start();
        }
        catch (IOException ex) {
            System.out.println("Exception caught: " + ex.getMessage() + " in Server.contructor");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        try {
            SSLServerSocketFactory socketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            tcpSocket = (SSLServerSocket) socketFactory.createServerSocket(tcpPort);
        }
        catch (IOException ex) {
            System.out.println("Exception caught: " + ex.getMessage() + " in Server.contructor");
        }

        ReceiveThread receiveThread = new ReceiveThread(tcpSocket);
        receiveThread.run();
    }

    public short getHttpPort() { return httpPort; }
    public short getTcpPort() { return tcpPort; }
    public SSLServerSocket getServerSocket() { return tcpSocket; }

    protected void sendTCPMessage(final InetAddress hostAddress, short hostPort, final String message) {

        try (final SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(hostAddress, hostPort);
             final OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
             final BufferedWriter bw = new BufferedWriter(out)) {
            bw.write(message);
            bw.flush();
        }
        catch (IOException ex) {
            System.out.println("Exception caught: " + ex.getMessage() + " in Server.contructor");
        }
    }

    // PRIVATE INNER CLASS

    private class ReceiveThread extends Thread {

        private SSLServerSocket tcpSocket;

        public ReceiveThread(SSLServerSocket tcpSocket){
            this.tcpSocket = tcpSocket;
        }

        public void run(){

            System.out.println("Server is now listening for TCP messages: ");

            while(true) {

                try (final SSLSocket socket = (SSLSocket) tcpSocket.accept();
                     final InputStreamReader in = new InputStreamReader(socket.getInputStream());
                     final BufferedReader br = new BufferedReader(in)) {
                        String message = null;
                        while ((message = br.readLine()) != null) {
                           // TODO: redirect message to dispatcher
                        }
                }
                catch (IOException ex) {
                    System.out.println("Exception caught: " + ex.getMessage() + " in ReceiveThread.run");
                }
            }
        }
    }
}