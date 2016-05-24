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
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import com.esotericsoftware.minlog.Log;

public class Connection
{
    int id = -1;
    private String name;
    EndPoint endPoint;
    TcpConnection tcp;
    UdpConnection udp;
    InetSocketAddress udpRemoteAddress;
    private Listener[] listeners = {};
    private final Object listenerLock = new Object();
    volatile boolean isConnected;
    volatile KryoNetException lastProtocolError;

    protected Connection()
    {
    }

    void initialize(Serialization serialization, int writeBufferSize, int objectBufferSize)
    {
        tcp = new TcpConnection(serialization, writeBufferSize, objectBufferSize);
    }

    public int getId()
    {
        return id;
    }

    public boolean isConnected()
    {
        return isConnected;
    }

    public KryoNetException getLastProtocolError()
    {
        return lastProtocolError;
    }

    public int sendTCP(Object object)
    {
        try
        {
            int length = tcp.send(this, object);

            if (length == 0)
            {
            }
            else if (Log.DEBUG)
            {
                String objectString = object == null ? "null" : object.getClass().getSimpleName();

                if (!(object instanceof FrameworkMessage))
                {
                    Log.debug("kryonet", this + " sent TCP: " + objectString + " (" + length + ")");
                }
            }

            return length;
        }
        catch (IOException ex)
        {
            if (Log.DEBUG)
            {
                Log.debug("kryonet", "Unable to send TCP with connection: " + this, ex);
            }

            close();

            return 0;
        }
        catch (KryoNetException ex)
        {
            if (Log.ERROR)
            {
                Log.error("kryonet", "Unable to send TCP with connection: " + this, ex);
            }

            close();

            return 0;
        }
    }

    int sendUDP(Object object)
    {
        SocketAddress address = udpRemoteAddress;

        if (address == null && udp != null)
        {
            address = udp.connectedAddress;
        }

        if (address == null && isConnected)
        {
            throw new IllegalStateException("Connection is not connected via UDP.");
        }

        try
        {
            if (address == null)
            {
                throw new SocketException("Connection is closed.");
            }

            int length = udp.send(this, object, address);

            if (Log.DEBUG)
            {
                if (length != -1)
                {
                    final String objectString = object == null ? "null" : object.getClass().getSimpleName();

                    if (!(object instanceof FrameworkMessage))
                    {
                        Log.debug("kryonet", this + " sent UDP: " + objectString + " (" + length + ")");
                    }
                }
                else
                {
                    Log.debug("kryonet", this + " was unable to send, UDP socket buffer full.");
                }
            }

            return length;
        }
        catch (IOException ex)
        {
            if (Log.DEBUG)
            {
                Log.debug("kryonet", "Unable to send UDP with connection: " + this, ex);
            }

            close();

            return 0;
        }
        catch (KryoNetException ex)
        {
            if (Log.ERROR)
            {
                Log.error("kryonet", "Unable to send UDP with connection: " + this, ex);
            }

            close();

            return 0;
        }
    }

    public void close()
    {
        boolean wasConnected = isConnected;

        isConnected = false;
        tcp.close();

        if (udp != null && udp.connectedAddress != null)
        {
            udp.close();
        }

        if (wasConnected)
        {
            notifyDisconnected();
        }

        setConnected(false);
    }

    public void addListener(Listener listener)
    {
        synchronized (listenerLock)
        {
            final Listener[] myListeners = listeners;
            int n = myListeners.length;

            for (final Listener myListener : myListeners)
            {
                if (listener == myListener)
                {
                    return;
                }
            }

            final Listener[] newListeners = new Listener[n + 1];

            newListeners[0] = listener;
            System.arraycopy(myListeners, 0, newListeners, 1, n);

            listeners = newListeners;
        }
    }

    public void removeListener(Listener listener)
    {
        synchronized (listenerLock)
        {
            final Listener[] listeners = this.listeners;
            int n = listeners.length;

            if (n == 0)
            {
                return;
            }

            Listener[] newListeners = new Listener[n - 1];

            for (int i = 0, ii = 0; i < n; i++)
            {
                Listener copyListener = listeners[i];

                if (listener == copyListener)
                    continue;
                if (ii == n - 1)
                    return;
                newListeners[ii++] = copyListener;
            }

            this.listeners = newListeners;
        }
    }

    void notifyConnected()
    {
        final Listener[] myListeners = listeners;

        for (final Listener myListener : myListeners)
        {
            myListener.connected(this);
        }
    }

    void notifyDisconnected()
    {
        final Listener[] myListeners = listeners;

        for (final Listener myListener : myListeners)
        {
            myListener.disconnected(this);
        }
    }

    void notifyIdle()
    {
        final Listener[] myListeners = listeners;

        for (final Listener myListener : myListeners)
        {
            myListener.idle(this);

            if (!isIdle())
            {
                break;
            }
        }
    }

    void notifyReceived(Object object)
    {
        final Listener[] myListeners = listeners;

        for (final Listener myListener : myListeners)
        {
            myListener.received(this, object);
        }
    }

    public EndPoint getEndPoint()
    {
        return endPoint;
    }

    public InetSocketAddress getRemoteAddressTCP()
    {
        final SocketChannel socketChannel = tcp.socketChannel;

        if (socketChannel != null)
        {
            final Socket socket = tcp.socketChannel.socket();

            if (socket != null)
            {
                return (InetSocketAddress) socket.getRemoteSocketAddress();
            }
        }

        return null;
    }

    public InetSocketAddress getRemoteAddressUDP()
    {
        final InetSocketAddress connectedAddress = udp.connectedAddress;

        if (connectedAddress != null)
        {
            return connectedAddress;
        }

        return udpRemoteAddress;
    }

    public void setBufferPositionFix(boolean bufferPositionFix)
    {
        tcp.bufferPositionFix = bufferPositionFix;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public int getTcpWriteBufferSize()
    {
        return tcp.writeBuffer.position();
    }

    public boolean isIdle()
    {
        return tcp.writeBuffer.position() / (float) tcp.writeBuffer.capacity() < tcp.idleThreshold;
    }

    public void setIdleThreshold(float idleThreshold)
    {
        tcp.idleThreshold = idleThreshold;
    }

    public String toString()
    {
        if (name != null)
        {
            return name;
        }

        return "Connection " + id;
    }

    void setConnected(boolean paramConnected)
    {
        isConnected = paramConnected;

        if (paramConnected && name == null)
        {
            name = "Connection " + id;
        }
    }
}