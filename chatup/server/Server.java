package chatup.server;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public abstract class Server {

    private HttpServer httpServer;
    private SSLServerSocket tcpSocket;
    private short httpPort;
    private short tcpPort;

    public Server(final HttpHandler httpHandler, short httpPort, short tcpPort) {

        this.httpPort = httpPort;
        this.tcpPort = tcpPort;

        try {
            httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);
            httpServer.createContext("/", httpHandler);
            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.start();
        }
        catch (IOException ex) {
            System.out.println("Exception caught: " + ex.getMessage() + " in Server.contructor");
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