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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Listener
{
    public void connected(final Connection connection)
    {
    }

    public void disconnected(final Connection connection)
    {
    }

    public void received(final Connection connection, final Object object)
    {
    }

    public void idle(final Connection connection)
    {
    }

    static abstract class QueuedListener extends Listener
    {
        final Listener listener;

        QueuedListener(final Listener paramListener)
        {
            if (paramListener == null)
            {
                throw new IllegalArgumentException("listener cannot be null.");
            }

            listener = paramListener;
        }

        @Override
        public void connected(final Connection connection)
        {
            queue(() -> listener.connected(connection));
        }

        @Override
        public void disconnected(final Connection connection)
        {
            queue(() -> listener.disconnected(connection));
        }

        @Override
        public void received(final Connection connection, final Object object)
        {
            queue(() -> listener.received(connection, object));
        }

        @Override
        public void idle(final Connection connection)
        {
            queue(() -> listener.idle(connection));
        }

        abstract protected void queue(final Runnable runnable);
    }

    static public class ThreadedListener extends QueuedListener {

        protected final ExecutorService threadPool;

        public ThreadedListener(final Listener paramListener)
        {
            this(paramListener, Executors.newFixedThreadPool(1));
        }

        public ThreadedListener(final Listener paramListener, final ExecutorService threadPool)
        {
            super(paramListener);

            if (threadPool == null)
            {
                throw new IllegalArgumentException("threadPool cannot be null.");
            }

            this.threadPool = threadPool;
        }

        @Override
        public void queue(Runnable runnable)
        {
            threadPool.execute(runnable);
        }
    }
}