/* Copyright (c) 2008, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package kryonet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.IntMap;

import static com.esotericsoftware.minlog.Log.*;

public class KryoServer implements EndPoint{

    private final Serialization serialization;
    private final int writeBufferSize, objectBufferSize;
    private final Selector selector;
    private int emptySelects;
    private ServerSocketChannel serverChannel;
    private Connection[] connections = {};
    private IntMap<Connection> pendingConnections = new IntMap();
    Listener[] listeners = {};
    private Object listenerLock = new Object();
    private int nextConnectionID = 1;
    private volatile boolean shutdown;
    private Object updateLock = new Object();
    private Thread updateThread;

    private Listener dispatchListener = new Listener(){

        public void connected(final Connection connection) {

            final Listener[] myListeners = KryoServer.this.listeners;

            for (int i = 0, n = myListeners.length; i < n; i++) {
                myListeners[i].connected(connection);
            }
        }

        public void disconnected(final Connection connection) {

            removeConnection(connection);

            final Listener[] myListeners = KryoServer.this.listeners;

            for (final Listener myListener : myListeners) {
                myListener.disconnected(connection);
            }
        }

        public void received(Connection connection, Object object) {

            final Listener[] myListeners = KryoServer.this.listeners;

            for (final Listener myListener : myListeners) {
                myListener.received(connection, object);
            }
        }

        public void idle(Connection connection) {

            final Listener[] listeners = KryoServer.this.listeners;

            for (final Listener listener : listeners) {
                listener.idle(connection);
            }
        }
    };

    public KryoServer() {
        this(16384, 2048);
    }

    public KryoServer(int writeBufferSize, int objectBufferSize) {
        this(writeBufferSize, objectBufferSize, new KryoSerialization());
    }

    public KryoServer(int writeBufferSize, int objectBufferSize, Serialization serialization) {

        this.writeBufferSize = writeBufferSize;
        this.objectBufferSize = objectBufferSize;
        this.serialization = serialization;

        try {
            selector = Selector.open();
        }
        catch (IOException ex) {
            throw new RuntimeException("Error opening selector.", ex);
        }
    }

    public Serialization getSerialization() {
        return serialization;
    }

    public Kryo getKryo() {
        return ((KryoSerialization) serialization).getKryo();
    }

    public void bind(int tcpPort) throws IOException {
        bind(new InetSocketAddress(tcpPort));
    }

    private void bind(InetSocketAddress tcpPort) throws IOException {

        close();

        synchronized (updateLock) {

            selector.wakeup();

            try {

                serverChannel = selector.provider().openServerSocketChannel();
                serverChannel.socket().bind(tcpPort);
                serverChannel.configureBlocking(false);
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);

                if (DEBUG) {
                    debug("kryonet", "Accepting connections on port: " + tcpPort + "/TCP");
                }
            }
            catch (IOException ex) {
                close();
                throw ex;
            }
        }
        if (INFO) {
            info("kryonet", "KryoServer opened.");
        }
    }

    public void update(int timeout) throws IOException {

        updateThread = Thread.currentThread();

        synchronized (updateLock) {}

        long startTime = System.currentTimeMillis();
        int select;

        if (timeout > 0) {
            select = selector.select(timeout);
        }
        else {
            select = selector.selectNow();
        }

        if (select == 0) {

            emptySelects++;

            if (emptySelects == 100) {

                emptySelects = 0;

                long elapsedTime = System.currentTimeMillis() - startTime;

                try {

                    if (elapsedTime < 25) {
                        Thread.sleep(25 - elapsedTime);
                    }
                }
                catch (InterruptedException ex) {}
            }
        }
        else {

            emptySelects = 0;

            final Set<SelectionKey> keys = selector.selectedKeys();

            synchronized (keys) {

                for (final Iterator<SelectionKey> iter = keys.iterator(); iter.hasNext(); ) {

                    keepAlive();
                    SelectionKey selectionKey = iter.next();
                    iter.remove();

                    final Connection fromConnection = (Connection) selectionKey.attachment();

                    try {
                        int ops = selectionKey.readyOps();

                        if (fromConnection != null) { // Must be a TCP read or write operation.

                            if ((ops & SelectionKey.OP_READ) == SelectionKey.OP_READ) {

                                try {

                                    while (true) {

                                        Object object = fromConnection.tcp.readObject(fromConnection);

                                        if (object == null) {
                                            break;
                                        }

                                        if (DEBUG) {
                                            String objectString = object == null ? "null" : object.getClass()
                                                                                                  .getSimpleName();
                                            if (!(object instanceof FrameworkMessage)) {
                                                debug("kryonet", fromConnection + " received TCP: " + objectString);
                                            }
                                        }

                                        fromConnection.notifyReceived(object);
                                    }
                                }
                                catch (IOException ex) {

                                    if (DEBUG) {
                                        debug("kryonet", fromConnection + " update: " + ex.getMessage());
                                    }

                                    fromConnection.close();
                                }
                                catch (KryoNetException ex) {

                                    if (ERROR) {
                                        error("kryonet", "Error reading TCP from connection: " + fromConnection, ex);
                                    }

                                    fromConnection.close();
                                }
                            }
                            if ((ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {

                                try {
                                    fromConnection.tcp.writeOperation();
                                }
                                catch (IOException ex) {

                                    if (DEBUG) {
                                        debug("kryonet", fromConnection + " update: " + ex.getMessage());
                                    }

                                    fromConnection.close();
                                }
                            }

                            continue;
                        }

                        if ((ops & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {

                            ServerSocketChannel serverChannel = this.serverChannel;

                            if (serverChannel == null) {
                                continue;
                            }

                            try {
                                SocketChannel socketChannel = serverChannel.accept();
                                if (socketChannel != null) {
                                    acceptOperation(socketChannel);
                                }
                            }
                            catch (IOException ex) {
                                if (DEBUG) {
                                    debug("kryonet", "Unable to accept new connection.", ex);
                                }
                            }
                            continue;
                        }

                        selectionKey.channel().close();
                    }
                    catch (CancelledKeyException ex) {
                        if (fromConnection != null) {
                            fromConnection.close();
                        }
                        else {
                            selectionKey.channel().close();
                        }
                    }
                }
            }
        }

        long time = System.currentTimeMillis();
        final Connection[] connections = this.connections;

        for (final Connection connection : connections) {

            if (connection.tcp.isTimedOut(time)) {

                if (DEBUG) {
                    debug("kryonet", connection + " timed out.");
                }

                connection.close();
            }
            else {

                if (connection.tcp.needsKeepAlive(time)) {
                    connection.sendTCP(FrameworkMessage.keepAlive);
                }
            }

            if (connection.isIdle()) {
                connection.notifyIdle();
            }
        }
    }

    private void keepAlive() {

        long time = System.currentTimeMillis();

        final Connection[] connections = this.connections;

        for (final Connection connection : connections) {

            if (connection.tcp.needsKeepAlive(time)) {
                connection.sendTCP(FrameworkMessage.keepAlive);
            }
        }
    }

    public void run() {

        shutdown = false;

        while (!shutdown) {

            try {
                update(250);
            }
            catch (IOException ex) {
                if (ERROR) {
                    error("kryonet", "Error updating server connections.", ex);
                }
                close();
            }
        }
    }

    public void start() {
        new Thread(this, "KryoServer").start();
    }

    public void stop() {

        if (!shutdown) {
            close();
            shutdown = true;
        }
    }

    private void acceptOperation(SocketChannel socketChannel) {

        final Connection connection = newConnection();

        connection.initialize(serialization, writeBufferSize, objectBufferSize);
        connection.endPoint = this;

        try {

            final SelectionKey selectionKey = connection.tcp.accept(selector, socketChannel);

            selectionKey.attach(connection);

            int id = nextConnectionID++;

            if (nextConnectionID == -1) {
                nextConnectionID = 1;
            }

            connection.id = id;
            connection.setConnected(true);
            connection.addListener(dispatchListener);
            addConnection(connection);

            final FrameworkMessage.RegisterTCP registerConnection = new FrameworkMessage.RegisterTCP();

            registerConnection.connectionID = id;
            connection.sendTCP(registerConnection);
            connection.notifyConnected();
        }
        catch (IOException ex) {

            connection.close();

            if (DEBUG) {
                debug("kryonet", "Unable to accept TCP connection.", ex);
            }
        }
    }

    protected Connection newConnection() {
        return new Connection();
    }

    private void addConnection(Connection connection) {

        final Connection[] newConnections = new Connection[connections.length + 1];

        newConnections[0] = connection;
        System.arraycopy(connections, 0, newConnections, 1, connections.length);
        connections = newConnections;
    }

    void removeConnection(Connection connection) {

        final ArrayList<Connection> temp = new ArrayList(Arrays.asList(connections));

        temp.remove(connection);
        connections = temp.toArray(new Connection[temp.size()]);
        pendingConnections.remove(connection.id);
    }

    public void sendToAllTCP(final Object object) {

        for (final Connection connection : connections) {
            connection.sendTCP(object);
        }
    }

    public void sendToAllExceptTCP(int connectionId, final Object myObject) {

        for (final Connection connection : connections) {

            if (connection.id != connectionId) {
                connection.sendTCP(myObject);
            }
        }
    }

    public void sendToTCP(int connectionId, final Object myObject) {

        for (final Connection connection : connections) {

            if (connection.id == connectionId) {
                connection.sendTCP(myObject);
                break;
            }
        }
    }

    public void addListener(final Listener listener) {

        synchronized (listenerLock) {

            final Listener[] myListeners = listeners;
            int n = myListeners.length;

            for (final Listener myListener : myListeners) {

                if (listener == myListener) {
                    return;
                }
            }

            final Listener[] newListeners = new Listener[n + 1];

            newListeners[0] = listener;
            System.arraycopy(myListeners, 0, newListeners, 1, n);
            listeners = newListeners;
        }
    }

    public void removeListener(final Listener listener) {

        synchronized (listenerLock) {

            final Listener[] myListeners = listeners;
            int n = myListeners.length;
            final Listener[] newListeners = new Listener[n - 1];

            for (int i = 0, ii = 0; i < n; i++) {

                final Listener copyListener = myListeners[i];

                if (listener == copyListener) {
                    continue;
                }

                if (ii == n - 1) {
                    return;
                }

                newListeners[ii++] = copyListener;
            }

            listeners = newListeners;
        }
    }

    public void close() {

        Connection[] connections = this.connections;

        if (INFO && connections.length > 0) {
            info("kryonet", "Closing server connections...");
        }

        for (int i = 0, n = connections.length; i < n; i++) {
            connections[i].close();
        }
        connections = new Connection[0];

        final ServerSocketChannel serverChannel = this.serverChannel;

        if (serverChannel != null) {
            try {
                serverChannel.close();
                if (INFO) {
                    info("kryonet", "KryoServer closed.");
                }
            }
            catch (IOException ex) {
                if (DEBUG) {
                    debug("kryonet", "Unable to close server.", ex);
                }
            }
            this.serverChannel = null;
        }

        synchronized (updateLock) { // Blocks to avoid a select while the selector is used to bind the server connection.
        }

        selector.wakeup();

        try {
            selector.selectNow();
        }
        catch (IOException ignored) {}
    }

    public void dispose() throws IOException {
        close();
        selector.close();
    }

    public Thread getUpdateThread() {
        return updateThread;
    }

    public Connection[] getConnections() {
        return connections;
    }
}