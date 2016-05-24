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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import java.security.AccessControlException;

import java.util.Iterator;
import java.util.Set;

import com.esotericsoftware.kryo.Kryo;

import kryonet.FrameworkMessage.RegisterTCP;
import kryonet.FrameworkMessage.RegisterUDP;

import static com.esotericsoftware.minlog.Log.*;

public class KryoClient extends Connection implements EndPoint {

    static {

        try {
            System.setProperty("java.net.preferIPv6Addresses", "false");
        }
        catch (AccessControlException ignored) {}
    }

    private final Serialization serialization;
    private Selector selector;
    private int emptySelects;

    private volatile boolean tcpRegistered;
    private volatile boolean udpRegistered;
    private volatile boolean shutdown;

    private final Object tcpRegistrationLock = new Object();
    private final Object udpRegistrationLock = new Object();
    private final Object updateLock = new Object();

    private Thread updateThread;
    private int connectTimeout;
    private InetAddress connectHost;
    private int connectTcpPort;
    private int connectUdpPort;
    private boolean isClosed;
    private ClientDiscoveryHandler discoveryHandler;

    public KryoClient()
    {
        this(8192, 2048);
    }

    public KryoClient(int writeBufferSize, int objectBufferSize)
    {
        this(writeBufferSize, objectBufferSize, new KryoSerialization());
    }

    private KryoClient(int writeBufferSize, int objectBufferSize, final Serialization serialization)
    {
        super();
        endPoint = this;

        this.serialization = serialization;
        this.discoveryHandler = ClientDiscoveryHandler.DEFAULT;

        initialize(serialization, writeBufferSize, objectBufferSize);

        try {
            selector = Selector.open();
        }
        catch (IOException ex) {
            throw new RuntimeException("Error opening selector.", ex);
        }
    }

    @Override
    public Serialization getSerialization()
    {
        return serialization;
    }

    @Override
    public Kryo getKryo()
    {
        return ((KryoSerialization)serialization).getKryo();
    }

    public void connect (int timeout, String host, int tcpPort) throws IOException
    {
        connect(timeout, InetAddress.getByName(host), tcpPort, -1);
    }

    private void connect (int timeout, InetAddress host, int tcpPort, int udpPort) throws IOException
    {
        if (host == null)
        {
            throw new IllegalArgumentException("host cannot be null.");
        }

        if (Thread.currentThread() == getUpdateThread())
        {
            throw new IllegalStateException("Cannot connect on the connection's update thread.");
        }

        this.connectTimeout = timeout;
        this.connectHost = host;
        this.connectTcpPort = tcpPort;
        this.connectUdpPort = udpPort;

        close();

        if (INFO)
        {
            if (udpPort != -1)
            {
                info("kryonet", "Connecting: " + host + ":" + tcpPort + "/" + udpPort);
            }
            else
            {
                info("kryonet", "Connecting: " + host + ":" + tcpPort);
            }
        }

        id = -1;

        try
        {
            if (udpPort != -1)
            {
                udp = new UdpConnection(serialization, tcp.readBuffer.capacity());
            }

            long endTime;

            synchronized (updateLock)
            {
                tcpRegistered = false;
                selector.wakeup();
                endTime = System.currentTimeMillis() + timeout;
                tcp.connect(selector, new InetSocketAddress(host, tcpPort), 5000);
            }

            synchronized (tcpRegistrationLock)
            {
                while (!tcpRegistered && System.currentTimeMillis() < endTime)
                {
                    try
                    {
                        tcpRegistrationLock.wait(100);
                    }
                    catch (InterruptedException ignored)
                    {
                    }
                }

                if (!tcpRegistered)
                {
                    throw new SocketTimeoutException("Connected, but timed out during TCP registration.\n" + "Note: KryoClient#update must be called in a separate thread during connect.");
                }
            }

            if (udpPort != -1)
            {
                final InetSocketAddress udpAddress = new InetSocketAddress(host, udpPort);

                synchronized (updateLock)
                {
                    udpRegistered = false;
                    selector.wakeup();
                    udp.connect(selector, udpAddress);
                }

                synchronized (udpRegistrationLock)
                {
                    while (!udpRegistered && System.currentTimeMillis() < endTime)
                    {
                        RegisterUDP registerUDP = new RegisterUDP();
                        registerUDP.connectionID = id;
                        udp.send(this, registerUDP, udpAddress);

                        try
                        {
                            udpRegistrationLock.wait(100);
                        }
                        catch (InterruptedException ex)
                        {
                        }
                    }

                    if (!udpRegistered)
                    {
                        throw new SocketTimeoutException("Connected, but timed out during UDP registration: " + host + ":" + udpPort);
                    }
                }
            }
        }
        catch (IOException ex)
        {
            close();
            throw ex;
        }
    }

    public void reconnect() throws IOException
    {
        reconnect(connectTimeout);
    }

    public void reconnect(int timeout) throws IOException
    {
        if (connectHost == null)
        {
            throw new IllegalStateException("This client has never been connected.");
        }

        connect(timeout, connectHost, connectTcpPort, connectUdpPort);
    }

    @Override
    public void update(int timeout) throws IOException
    {
        updateThread = Thread.currentThread();

        synchronized (updateLock)
        {
        }

        long startTime = System.currentTimeMillis();
        int select ;

        if (timeout > 0)
        {
            select = selector.select(timeout);
        }
        else
        {
            select = selector.selectNow();
        }

        if (select == 0)
        {
            emptySelects++;

            if (emptySelects == 100)
            {
                emptySelects = 0;

                long elapsedTime = System.currentTimeMillis() - startTime;

                try
                {
                    if (elapsedTime < 25)
                    {
                        Thread.sleep(25 - elapsedTime);
                    }
                }
                catch (InterruptedException ex)
                {
                }
            }
        }
        else
        {
            emptySelects = 0;
            isClosed = false;

            final Set<SelectionKey> keys = selector.selectedKeys();

            synchronized (keys)
            {
                for (final Iterator<SelectionKey> iter = keys.iterator(); iter.hasNext();)
                {
                    keepAlive();

                    final SelectionKey selectionKey = iter.next();

                    iter.remove();

                    try
                    {
                        int ops = selectionKey.readyOps();

                        if ((ops & SelectionKey.OP_READ) == SelectionKey.OP_READ)
                        {
                            if (selectionKey.attachment() == tcp)
                            {
                                while (true)
                                {
                                    final Object paramObject = tcp.readObject(this);

                                    if (paramObject == null)
                                    {
                                        break;
                                    }

                                    if (!tcpRegistered)
                                    {
                                        if (paramObject instanceof RegisterTCP)
                                        {
                                            id = ((RegisterTCP)paramObject).connectionID;

                                            synchronized (tcpRegistrationLock)
                                            {
                                                tcpRegistered = true;
                                                tcpRegistrationLock.notifyAll();

                                                if (udp == null)
                                                {
                                                    setConnected(true);
                                                }
                                            }

                                            if (udp == null)
                                            {
                                                notifyConnected();
                                            }
                                        }

                                        continue;
                                    }

                                    if (udp != null && !udpRegistered)
                                    {
                                        if (paramObject instanceof RegisterUDP)
                                        {
                                            synchronized (udpRegistrationLock)
                                            {
                                                udpRegistered = true;
                                                udpRegistrationLock.notifyAll();
                                                setConnected(true);
                                            }

                                            notifyConnected();
                                        }

                                        continue;
                                    }

                                    if (!isConnected)
                                    {
                                        continue;
                                    }

                                    if (DEBUG)
                                    {
                                        String objectString = paramObject == null ? "null" : paramObject.getClass().getSimpleName();

                                        if (!(paramObject instanceof FrameworkMessage))
                                        {
                                            debug("kryonet", this + " received TCP: " + objectString);
                                        }
                                    }

                                    notifyReceived(paramObject);
                                }
                            }
                            else
                            {
                                if (udp.readFromAddress() == null) continue;
                                Object object = udp.readObject(this);
                                if (object == null) continue;
                                if (DEBUG) {
                                    String objectString = object == null ? "null" : object.getClass().getSimpleName();
                                    debug("kryonet", this + " received UDP: " + objectString);
                                }
                                notifyReceived(object);
                            }
                        }
                        if ((ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) tcp.writeOperation();
                    } catch (CancelledKeyException ignored)
                    {
                    }
                }
            }
        }

        if (isConnected)
        {
            long time = System.currentTimeMillis();

            if (tcp.isTimedOut(time))
            {
                close();
            }
            else
            {
                keepAlive();
            }

            if (isIdle())
            {
                notifyIdle();
            }
        }
    }

    void keepAlive()
    {
        if (!isConnected)
        {
            return;
        }

        long time = System.currentTimeMillis();

        if (tcp.needsKeepAlive(time))
        {
            sendTCP(FrameworkMessage.keepAlive);
        }

        if (udp != null && udpRegistered && udp.needsKeepAlive(time))
        {
            sendUDP(FrameworkMessage.keepAlive);
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
            catch (IOException ex)
            {
                 if (DEBUG)
                 {
                    if (isConnected)
                    {
                        debug("kryonet", this + " update: " + ex.getMessage());
                    }
                    else
                    {
                        debug("kryonet", "Unable to update connection: " + ex.getMessage());
                    }
                }

                close();
            }
            catch (KryoNetException ex)
            {
                lastProtocolError = ex;

                if (ERROR)
                {
                    if (isConnected)
                    {
                        error("kryonet", "Error updating connection: " + this, ex);
                    }
                    else
                    {
                        error("kryonet", "Error updating connection.", ex);
                    }
                }

                close();

                throw ex;
            }
        }
    }

    @Override
    public void start()
    {
        if (updateThread != null)
        {
            shutdown = true;

            try
            {
                updateThread.join(5000);
            }
            catch (InterruptedException ignored)
            {
            }
        }

        updateThread = new Thread(this, "KryoClient");
        updateThread.setDaemon(true);
        updateThread.start();
    }

    @Override
    public void stop()
    {
        if (!shutdown)
        {
            close();
            shutdown = true;
            selector.wakeup();
        }
    }

    @Override
    public void close()
    {
        super.close();

        synchronized (updateLock)
        {
        }

        if (!isClosed)
        {
            isClosed = true;
            selector.wakeup();

            try
            {
                selector.selectNow();
            }
            catch (IOException ex)
            {
            }
        }
    }

    public void dispose() throws IOException
    {
        close();
        selector.close();
    }

    @Override
    public void addListener(final Listener listener)
    {
        super.addListener(listener);
    }

    @Override
    public void removeListener(final Listener listener)
    {
        super.removeListener(listener);
    }

    @Override
    public Thread getUpdateThread()
    {
        return updateThread;
    }
}