package chatup.server;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import java.io.*;
import java.net.InetAddress;

public class SSLConnection {

    private SSLServerSocket tcpSocket;

    public SSLConnection(short paramPort, final ServerKeystore serverKeystore) throws IOException {
        System.setProperty("javax.net.ssl.keyStore", serverKeystore.getPath());
        System.setProperty("javax.net.ssl.keyStorePassword", serverKeystore.getPassword());
        System.setProperty("javax.net.ssl.trustStore", "truststore.jts");
        System.setProperty("javax.net.ssl.trustStorePassword", serverKeystore.getPassword());
        SSLServerSocketFactory socketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        tcpPort = paramPort;
        tcpSocket = (SSLServerSocket) socketFactory.createServerSocket(tcpPort);
    }

    private short tcpPort;

    public ReceiveThread getThread() {
        return new ReceiveThread(tcpSocket);
    }

    public short getPort() {
        return tcpPort;
    }

    public final SSLServerSocket getSocket() {
        return tcpSocket;
    }

    public class ReceiveThread extends Thread {

        private SSLServerSocket tcpSocket;

        public ReceiveThread(SSLServerSocket tcpSocket) {
            this.tcpSocket = tcpSocket;
        }

        public void run() {

            System.out.println("Server is now listening for TCP messages: ");

            while (true) {

                try (final SSLSocket socket = (SSLSocket) tcpSocket.accept();
                     final InputStreamReader in = new InputStreamReader(socket.getInputStream());
                     final BufferedReader br = new BufferedReader(in)) {
                    String message = null;
                    while ((message = br.readLine()) != null) {
                        // TODO: redirect message to dispatcher
                    }
                } catch (IOException ex) {
                    System.out.println("Exception caught: " + ex.getMessage() + " in ReceiveThread.run");
                }
            }
        }
    }

    public void send(final InetAddress hostAddress, short hostPort, final String message) {

        try (final SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(hostAddress, hostPort);
             final OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
             final BufferedWriter bw = new BufferedWriter(out)) {
            bw.write(message);
            bw.flush();
        } catch (IOException ex) {
            System.out.println("Exception caught: " + ex.getMessage() + " in Server.contructor");
        }
    }
}