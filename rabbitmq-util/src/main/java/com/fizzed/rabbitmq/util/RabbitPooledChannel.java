package com.fizzed.rabbitmq.util;

import com.rabbitmq.client.Channel;
import java.io.Closeable;
import java.io.IOException;

public class RabbitPooledChannel implements Closeable {

    private final RabbitChannelPool pool;
    private final Channel wrapped;
    private volatile boolean closed;

    public RabbitPooledChannel(
            RabbitChannelPool pool,
            Channel channel) {
        
        this.pool = pool;
        this.wrapped = channel;
    }

    public Channel unwrap() {
        if (this.closed) {
            throw new IllegalStateException("Pooled channel closed");
        }
        return this.wrapped;
    }
    
    @Override
    public void close() throws IOException {
        this.closed = true;
        this.pool.returnChannel(this.wrapped);
    }
    
}