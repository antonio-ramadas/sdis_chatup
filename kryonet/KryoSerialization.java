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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import kryonet.FrameworkMessage.DiscoverHost;
import kryonet.FrameworkMessage.KeepAlive;
import kryonet.FrameworkMessage.RegisterTCP;

import java.nio.ByteBuffer;

class KryoSerialization implements Serialization
{
    private final Kryo kryo;
    private final ByteBufferInput input;
    private final ByteBufferOutput output;

    KryoSerialization()
    {
        this(new Kryo());
        kryo.setReferences(false);
        kryo.setRegistrationRequired(true);
    }

    private KryoSerialization(final Kryo kryoInstance)
    {
        kryo = kryoInstance;
        kryoInstance.register(RegisterTCP.class);
        kryoInstance.register(KeepAlive.class);
        kryoInstance.register(DiscoverHost.class);
        input = new ByteBufferInput();
        output = new ByteBufferOutput();
    }

    final Kryo getKryo()
    {
        return kryo;
    }

    public synchronized void write(final Connection connection, final ByteBuffer buffer, final Object object)
    {
        output.setBuffer(buffer);
        kryo.getContext().put("connection", connection);
        kryo.writeClassAndObject(output, object);
        output.flush();
    }

    public synchronized Object read(final Connection connection, final ByteBuffer buffer)
    {
        input.setBuffer(buffer);
        kryo.getContext().put("connection", connection);
        return kryo.readClassAndObject(input);
    }

    public void writeLength(final ByteBuffer buffer, int length)
    {
        buffer.putInt(length);
    }

    public int readLength(final ByteBuffer buffer)
    {
        return buffer.getInt();
    }

    public int getLengthLength()
    {
        return 4;
    }
}