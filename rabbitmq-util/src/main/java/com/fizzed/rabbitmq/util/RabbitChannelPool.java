package com.fizzed.rabbitmq.util;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.util.NoSuchElementException;

public class RabbitChannelPool implements Cloneable {

    private GenericObjectPool<Channel> internalPool;
    
    public RabbitChannelPool(
            Connection connection) {
        
        this(connection, new RabbitChannelPoolConfig());
    }

    public RabbitChannelPool(
            Connection connection,
            RabbitChannelPoolConfig poolConfig) {
        
        if (this.internalPool != null) {
            try {
                closeInternalPool();
            } catch (Exception e) {
                
            }
        }

        final RabbitChannelPoolFactory factory = new RabbitChannelPoolFactory(connection);
        
        this.internalPool = new GenericObjectPool<>(factory, poolConfig);
    }

    public int getActiveCount() {
        return this.internalPool.getNumActive();
    }

    public int getIdleCount() {
        return this.internalPool.getNumIdle();
    }

    public int getMinIdleCount() {
        return this.internalPool.getMinIdle();
    }

    public int getMaxIdleCount() {
        return this.internalPool.getMaxIdle();
    }
    
    public int getMaxTotalCount() {
        return this.internalPool.getMaxTotal();
    }
    
    // helpers
    
    public void basicPublish(
            String exchange,
            String routingKey,
            AMQP.BasicProperties properties,
            byte[] messageBody) throws IOException {
        
        try (RabbitPooledChannel pooledChannel = this.getChannel()) {
            pooledChannel.unwrap().basicPublish(exchange, routingKey, properties, messageBody);
        }
    }

    public void basicPublish(
            String exchange,
            String routingKey,
            boolean mandatory,
            AMQP.BasicProperties properties,
            byte[] messageBody) throws IOException {
        
        try (RabbitPooledChannel pooledChannel = this.getChannel()) {
            pooledChannel.unwrap().basicPublish(exchange, routingKey, mandatory, properties, messageBody);
        }
    }
    
    
    // internal stuff
    
    
    private void closeInternalPool() {
        try {
            this.internalPool.close();
        } catch (Exception e) {
            throw new RabbitException("Could not destroy the pool", e);
        }
    }

    public void returnChannel(Channel channel) {
        try {
            if (channel.isOpen()) {
                // clear un-safe values that could have been set
                channel.clearReturnListeners();
                channel.clearConfirmListeners();
                this.internalPool.returnObject(channel);
            } else {
                this.internalPool.invalidateObject(channel);
            }
        } catch (Exception e) {
            throw new RabbitException("Could not return the resource to the pool", e);
        }
    }

    public RabbitPooledChannel getChannel() {
        try {
            Channel channel = this.internalPool.borrowObject();
            
            return new RabbitPooledChannel(this, channel);
        }
        catch (NoSuchElementException nse) {
            if (null == nse.getCause()) { // The exception was caused by an exhausted pool
                throw new RabbitException("Could not get a resource since the pool is exhausted", nse);
            }
            // Otherwise, the exception was caused by the implemented activateObject() or ValidateObject()
            throw new RabbitException("Could not get a resource from the pool", nse);
        } catch (Exception e) {
            throw new RabbitException("Could not get a resource from the pool", e);
        }
    }
    
}