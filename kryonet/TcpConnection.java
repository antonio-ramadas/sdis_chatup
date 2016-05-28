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

import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

class TcpConnection
{
    SocketChannel socketChannel;

    final ByteBuffer readBuffer;
    final ByteBuffer writeBuffer;
    private SelectionKey selectionKey;

    float idleThreshold = 0.1f;

    private volatile long lastWriteTime;
    private volatile long lastReadTime;

    private boolean bufferPositionFix;
    private int currentObjectLength;
    private int keepAliveMillis = 8000;
    private int timeoutMillis = 12000;

    private final Serialization serialization;
    private final Object writeLock = new Object();

    TcpConnection(Serialization paramSerialization, int writeBufferSize, int objectBufferSize)
    {
        serialization = paramSerialization;
        writeBuffer = ByteBuffer.allocate(writeBufferSize);
        readBuffer = ByteBuffer.allocate(objectBufferSize);
        readBuffer.flip();
    }

    final SelectionKey accept(Selector selector, SocketChannel socketChannel) throws IOException
    {
        writeBuffer.clear();
        readBuffer.clear();
        readBuffer.flip();
        currentObjectLength = 0;

        try
        {
            this.socketChannel = socketChannel;
            socketChannel.configureBlocking(false);
            Socket socket = socketChannel.socket();
            socket.setTcpNoDelay(true);
            selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
            lastReadTime = lastWriteTime = System.currentTimeMillis();

            return selectionKey;
        }
        catch (final IOException ex)
        {
            close();
            throw ex;
        }
    }

    void connect(Selector selector, SocketAddress remoteAddress, int timeout) throws IOException
    {
        close();
        writeBuffer.clear();
        readBuffer.clear();
        readBuffer.flip();
        currentObjectLength = 0;

        try
        {
            final SocketChannel socketChannel = selector.provider().openSocketChannel();
            final Socket socket = socketChannel.socket();

            socket.setTcpNoDelay(true);
            socket.connect(remoteAddress, timeout);
            socketChannel.configureBlocking(false);

            this.socketChannel = socketChannel;
            selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
            selectionKey.attach(this);
            lastReadTime = lastWriteTime = System.currentTimeMillis();
        }
        catch (final IOException ex)
        {
            close();
            throw new IOException("Unable to connect to: " + remoteAddress);
        }
    }

    final Object readObject(Connection connection) throws IOException
    {
        SocketChannel socketChannel = this.socketChannel;

        if (socketChannel == null)
        {
            throw new SocketException("Connection is closed.");
        }

        if (currentObjectLength == 0)
        {
            int lengthLength = serialization.getLengthLength();

            if (readBuffer.remaining() < lengthLength)
            {
                readBuffer.compact();

                int bytesRead = socketChannel.read(readBuffer);

                readBuffer.flip();

                if (bytesRead == -1)
                {
                    throw new SocketException("Connection is closed.");
                }

                lastReadTime = System.currentTimeMillis();

                if (readBuffer.remaining() < lengthLength)
                {
                    return null;
                }
            }

            currentObjectLength = serialization.readLength(readBuffer);

            if (currentObjectLength <= 0)
            {
                throw new KryoNetException("Invalid object length: " + currentObjectLength);
            }

            if (currentObjectLength > readBuffer.capacity())
            {
                throw new KryoNetException("Unable to read object larger than read buffer: " + currentObjectLength);
            }
        }

        int length = currentObjectLength;

        if (readBuffer.remaining() < length)
        {
            readBuffer.compact();

            int bytesRead = socketChannel.read(readBuffer);

            readBuffer.flip();

            if (bytesRead == -1)
            {
                throw new SocketException("Connection is closed.");
            }

            lastReadTime = System.currentTimeMillis();

            if (readBuffer.remaining() < length)
            {
                return null;
            }
        }

        currentObjectLength = 0;

        int startPosition = readBuffer.position();
        int oldLimit = readBuffer.limit();

        readBuffer.limit(startPosition + length);
        Object object;

        try
        {
            object = serialization.read(connection, readBuffer);
        }
        catch (Exception ex)
        {
            throw new KryoNetException("Error during deserialization.", ex);
        }

        readBuffer.limit(oldLimit);

        if (readBuffer.position() - startPosition != length)
        {
            throw new KryoNetException("Incorrect number of bytes (" + (startPosition + length - readBuffer.position())  + " remaining) used to deserialize object: " + object);
        }

        return object;
    }

    void writeOperation() throws IOException
    {
        synchronized (writeLock)
        {
            if (writeToSocket())
            {
                selectionKey.interestOps(SelectionKey.OP_READ);
            }

            lastWriteTime = System.currentTimeMillis();
        }
    }

    private boolean writeToSocket() throws IOException
    {
        SocketChannel socketChannel = this.socketChannel;

        if (socketChannel == null)
        {
            throw new SocketException("Connection is closed.");
        }

        final ByteBuffer buffer = writeBuffer;

        buffer.flip();

        while (buffer.hasRemaining())
        {
            if (bufferPositionFix)
            {
                buffer.compact();
                buffer.flip();
            }

            if (socketChannel.write(buffer) == 0)
            {
                break;
            }
        }

        buffer.compact();

        return buffer.position() == 0;
    }

    int send(final Connection paramConnection, final Object object) throws IOException
    {
        final SocketChannel socketChannel = this.socketChannel;

        if (socketChannel == null)
        {
            throw new SocketException("Connection is closed.");
        }

        synchronized (writeLock)
        {
            int start = writeBuffer.position();
            int lengthLength = serialization.getLengthLength();

            writeBuffer.position(writeBuffer.position() + lengthLength);

            try
            {
                serialization.write(paramConnection, writeBuffer, object);
            }
            catch (final KryoNetException ex)
            {
                throw new KryoNetException("Error serializing object of type: " + object.getClass().getName(), ex);
            }

            int end = writeBuffer.position();

            writeBuffer.position(start);
            serialization.writeLength(writeBuffer, end - lengthLength - start);
            writeBuffer.position(end);

            if (start == 0 && !writeToSocket())
            {
                selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }
            else
            {
                selectionKey.selector().wakeup();
            }

            lastWriteTime = System.currentTimeMillis();

            return end - start;
        }
    }

    void close()
    {
        try
        {
            if (socketChannel != null)
            {
                socketChannel.close();
                socketChannel = null;

                if (selectionKey != null)
                {
                    selectionKey.selector().wakeup();
                }
            }
        }
        catch (final IOException ignored)
        {
        }
    }

    boolean needsKeepAlive(long time)
    {
        return socketChannel != null && keepAliveMillis > 0 && time - lastWriteTime > keepAliveMillis;
    }

    boolean isTimedOut(long time)
    {
        return socketChannel != null && timeoutMillis > 0 && time - lastReadTime > timeoutMillis;
    }
}