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

public class KryoServer implements EndPoint
{
    private final Object listenerLock = new Object();
    private final Object updateLock = new Object();
    private final Serialization serialization;
    private final Selector selector;

    private ServerSocketChannel serverChannel;
    private Connection[] connections = {};
    private IntMap<Connection> pendingConnections = new IntMap();
    private Listener[] listeners = {};
    private Thread updateThread;

    private int emptySelects;
    private int nextConnectionID = 1;

    private volatile boolean shutdown;
    private final int writeBufferSize;
    private final int objectBufferSize;

    private Listener dispatchListener = new Listener()
    {
        public void connected(final Connection connection)
        {
            final Listener[] myListeners = KryoServer.this.listeners;

            for (final Listener myListener : myListeners)
            {
                myListener.connected(connection);
            }
        }

        public void disconnected(final Connection connection)
        {
            removeConnection(connection);

            final Listener[] myListeners = KryoServer.this.listeners;

            for (final Listener myListener : myListeners)
            {
                myListener.disconnected(connection);
            }
        }

        public void received(Connection connection, Object object)
        {
            final Listener[] myListeners = KryoServer.this.listeners;

            for (final Listener myListener : myListeners)
            {
                myListener.received(connection, object);
            }
        }

        public void idle(Connection connection)
        {
            final Listener[] listeners = KryoServer.this.listeners;

            for (final Listener myListener : listeners)
            {
                myListener.idle(connection);
            }
        }
    };

    protected KryoServer() {
        this(16384, 2048);
    }

    private KryoServer(int writeBufferSize, int objectBufferSize) {
        this(writeBufferSize, objectBufferSize, new KryoSerialization());
    }

    private KryoServer(int writeBufferSize, int objectBufferSize, Serialization serialization) {

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

    @Override
    public Serialization getSerialization() {
        return serialization;
    }

    @Override
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
            }
            catch (IOException ex) {
                close();
                throw ex;
            }
        }
    }

    @Override
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
                catch (final InterruptedException ignored) {}
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

                                        fromConnection.notifyReceived(object);
                                    }
                                }
                                catch (IOException ex) {
                                    fromConnection.close();
                                }
                                catch (KryoNetException ex) {
                                    error("kryonet", "Error reading TCP from connection: " + fromConnection, ex);
                                    fromConnection.close();
                                }
                            }
                            if ((ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {

                                try {
                                    fromConnection.tcp.writeOperation();
                                }
                                catch (IOException ex) {
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
                            catch (IOException ignored) {}

                            continue;
                        }

                        selectionKey.channel().close();
                    }
                    catch (final CancelledKeyException ex) {

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

    private void keepAlive()
    {
        long time = System.currentTimeMillis();
        final Connection[] connections = this.connections;

        for (final Connection connection : connections)
        {
            if (connection.tcp.needsKeepAlive(time))
            {
                connection.sendTCP(FrameworkMessage.keepAlive);
            }
        }
    }

    @Override
    public void run()
    {
        shutdown = false;

        while (!shutdown)
        {
            try
            {
                update(250);
            }
            catch (final IOException ex)
            {
                close();
            }
        }
    }

    @Override
    public void start()
    {
        new Thread(this, "KryoServer").start();
    }

    public void stop()
    {
        if (!shutdown)
        {
            close();
            shutdown = true;
        }
    }

    private void acceptOperation(SocketChannel socketChannel)
    {
        final Connection connection = newConnection();

        connection.initialize(serialization, writeBufferSize, objectBufferSize);
        connection.endPoint = this;

        try
        {
            final SelectionKey selectionKey = connection.tcp.accept(selector, socketChannel);

            selectionKey.attach(connection);

            int id = nextConnectionID++;

            if (nextConnectionID == -1)
            {
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
        catch (final IOException ex)
        {
            connection.close();
        }
    }

    protected Connection newConnection()
    {
        return new Connection();
    }

    private void addConnection(final Connection paramConnection)
    {
        final Connection[] newConnections = new Connection[connections.length + 1];

        newConnections[0] = paramConnection;
        System.arraycopy(connections, 0, newConnections, 1, connections.length);
        connections = newConnections;
    }

    private void removeConnection(Connection connection)
    {
        final ArrayList<Connection> temp = new ArrayList(Arrays.asList(connections));

        temp.remove(connection);
        connections = temp.toArray(new Connection[temp.size()]);
        pendingConnections.remove(connection.id);
    }

    public void sendToAllTCP(final Object paramObject)
    {
        for (final Connection connection : connections)
        {
            connection.sendTCP(paramObject);
        }
    }

    public void sendToAllExceptTCP(int connectionId, final Object myObject)
    {
        for (final Connection connection : connections)
        {
            if (connection.id != connectionId)
            {
                connection.sendTCP(myObject);
            }
        }
    }

    public void sendToTCP(int connectionId, final Object myObject)
    {
        for (final Connection connection : connections)
        {
            if (connection.id == connectionId)
            {
                connection.sendTCP(myObject);
                break;
            }
        }
    }

    @Override
    public void addListener(final Listener paramListener)
    {
        synchronized (listenerLock)
        {
            final Listener[] myListeners = listeners;
            int n = myListeners.length;

            for (final Listener myListener : myListeners) {

                if (paramListener == myListener) {
                    return;
                }
            }

            final Listener[] newListeners = new Listener[n + 1];

            newListeners[0] = paramListener;
            System.arraycopy(myListeners, 0, newListeners, 1, n);
            listeners = newListeners;
        }
    }

    @Override
    public void removeListener(final Listener paramListener)
    {
        synchronized (listenerLock)
        {
            final Listener[] myListeners = listeners;
            int n = myListeners.length;
            final Listener[] newListeners = new Listener[n - 1];

            for (int i = 0, ii = 0; i < n; i++) {

                final Listener copyListener = myListeners[i];

                if (paramListener == copyListener) {
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

    @Override
    public void close()
    {
        Connection[] connections = this.connections;

        if (connections.length > 0) {
            info("kryonet", "Closing server connections...");
        }

        for (final Connection myConnection : connections) {
            myConnection.close();
        }

        connections = new Connection[0];

        final ServerSocketChannel serverChannel = this.serverChannel;

        if (serverChannel != null)
        {
            try
            {
                serverChannel.close();
            }
            catch (IOException ignored)
            {
            }

            this.serverChannel = null;
        }

        synchronized (updateLock)
        {
        }

        selector.wakeup();

        try
        {
            selector.selectNow();
        }
        catch (IOException ignored)
        {
        }
    }

    public void dispose() throws IOException
    {
        close();
        selector.close();
    }

    @Override
    public Thread getUpdateThread()
    {
        return updateThread;
    }
}