package chatup.tcp;

import chatup.model.Message;
import chatup.server.ServerInfo;
import chatup.server.ServerKeystore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

abstract public class SSLConnection {

    private final SSLServerSocket tcpSocket;

    public SSLConnection(short paramPort, final ServerKeystore serverKeystore) throws IOException {
        System.setProperty("javax.net.ssl.keyStore", serverKeystore.getPath());
        System.setProperty("javax.net.ssl.keyStorePassword", serverKeystore.getPassword());
        System.setProperty("javax.net.ssl.trustStore", "truststore.jts");
        System.setProperty("javax.net.ssl.trustStorePassword", serverKeystore.getPassword());
        SSLServerSocketFactory socketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        tcpSocket = (SSLServerSocket) socketFactory.createServerSocket(paramPort);
    }

    public final ReceiveThread getThread() {
        return new ReceiveThread(tcpSocket);
    }

    public final SSLServerSocket getSocket() {
        return tcpSocket;
    }

    public class ReceiveThread extends Thread {

        private final SSLServerSocket tcpSocket;

        ReceiveThread(SSLServerSocket tcpSocket) {
            this.tcpSocket = tcpSocket;
        }

        @Override
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

    public void sendObject(final ServerInfo paramServer, final Message paramObject) {

        try (final SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(paramServer.getAddress(), paramServer.getPort());
             final ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream()))
        {
            oos.writeObject(paramObject);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void send(final ServerInfo paramServer, final String message) {

        try (final SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(paramServer.getAddress(), paramServer.getPort());
             final OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
             final BufferedWriter bw = new BufferedWriter(out)) {
            bw.write(message);
            bw.flush();
        }
        catch (IOException ex) {
            System.out.println("Exception caught: " + ex.getMessage() + " in Server.contructor");
        }
    }

    public abstract void handle(TcpMessage message);
}