package com.fizzed.rabbitmq.util;

import com.fizzed.rabbitmq.util.impl.DefaultRabbitPublishFuture;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ReturnListener;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitReliablePublisher {
    static private final Logger log = LoggerFactory.getLogger(RabbitReliablePublisher.class);
    
    private final Connection connection;
    private Channel channel;
    private boolean initialized;
    private final Listener listener;
    private final ReentrantLock lock;
    private final ArrayList<DefaultRabbitPublishFuture> futures;
    private long pendingTimeout;
    
    public RabbitReliablePublisher(
            Connection connection) {

        this.connection = connection;
        this.lock = new ReentrantLock();
        this.futures = new ArrayList<>(1);      // usually just 1 outstanding
        this.pendingTimeout = 5000;             // 5 secs by default
        this.listener = new Listener();
    }

    public void setPendingTimeout(long timeout, TimeUnit tu) {
        this.pendingTimeout = tu.toMillis(timeout);
    }

    public long getPendingTimeout() {
        return pendingTimeout;
    }
    
    private DefaultRabbitPublishFuture removeFutureByDeliveryTag(
            long deliveryTag) {
        
        for (int i = 0; i < futures.size(); i++) {
            DefaultRabbitPublishFuture future = futures.get(i);
            if (future.getDeliveryTag() == deliveryTag) {
                futures.remove(i);
                return future;
            }
        }
        
        return null;
    }
    
    private DefaultRabbitPublishFuture removeFutureByMessageId(
            String messageId) {
        
        if (messageId != null) {
            for (int i = 0; i < futures.size(); i++) {
                DefaultRabbitPublishFuture future = futures.get(i);
                if (future.getMessageId().equals(messageId)) {
                    futures.remove(i);
                    return future;
                }
            }
        }
        
        return null;
    }
    
    private void failAllFutures(RabbitException exception) {
        lock.lock();
        try {
            for (int i = 0; i < futures.size(); i++) {
                DefaultRabbitPublishFuture future = futures.get(i);
                future.failure(exception);
            }
            this.futures.clear();
        } finally {
            lock.unlock();
        }
    }
    
    private class Listener implements ReturnListener, ConfirmListener, ShutdownListener {
        
        @Override
        public void handleAck(long deliveryTag, boolean multiple) throws IOException {
            log.trace("Message acked: deliveryTag={}", deliveryTag);
            lock.lock();
            try {
                final DefaultRabbitPublishFuture future = removeFutureByDeliveryTag(deliveryTag);
                if (future != null) {
                    future.success();
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void handleNack(long deliveryTag, boolean multiple) throws IOException {
            log.warn("Message nacked: deliveryTag={}", deliveryTag);
            lock.lock();
            try {
                final DefaultRabbitPublishFuture future = removeFutureByDeliveryTag(deliveryTag);
                if (future != null) {
                    future.failure(new MessageNackedException(future.getMessageId()));
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void handleReturn(int replyCode, String replyText, String exchange, String routingKey, AMQP.BasicProperties properties, byte[] body) throws IOException {
            final String messageId = properties != null ? properties.getMessageId() : null;
            log.warn("Message returned: replyCode={}, replyText={}, messageId={}", replyCode, replyText, messageId);
            lock.lock();
            try {
                final DefaultRabbitPublishFuture future = removeFutureByMessageId(messageId);
                if (future != null) {
                    future.failure(new MessageReturnedException(replyCode, replyText, exchange, routingKey));
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void shutdownCompleted(ShutdownSignalException cause) {
            // com.rabbitmq.client.ShutdownSignalException: channel error; protocol method: #method<channel.close>(reply-code=404, reply-text=NOT_FOUND - no exchange 'ExchangeDoesNotExist' in vhost '/', class-id=60, method-id=40)
            log.warn("Channel shutdown: {}", cause.getMessage());
            lock.lock();
            try {
                // cancel all pending futures
                failAllFutures(new RabbitException(cause.getMessage(), cause));
            } finally {
                lock.unlock();
            }            
        }

    }
    
    private void reinit() throws IOException {
        this.initialized = false;
        
        if (this.channel != null) {
            try {
                this.channel.close();
            }
            catch (Exception e) {
                // ignore all
            }
            this.channel = null;
        }
        
        this.init();
    }
    
    private void init() throws IOException {
        if (this.channel == null || !this.initialized) {
            if (this.channel == null) {
                this.channel = this.connection.createChannel();
                log.info("Created channel {} for reliable publishing", this.channel.getChannelNumber());
            }

            this.channel.confirmSelect();
            this.channel.addConfirmListener(this.listener);
            this.channel.addReturnListener(this.listener);
            this.channel.addShutdownListener(this.listener);
            this.initialized = true;
        }
    }
    
    public RabbitPublishFuture publish(
            String exchangeName,
            String routingKey,
            boolean mandatory,
            BasicProperties properties,
            byte[] messageBytes) throws IOException, InterruptedException, TimeoutException {
        
        Objects.requireNonNull(exchangeName, "exchangeName was null");
        Objects.requireNonNull(routingKey, "routingKey was null");
        Objects.requireNonNull(properties, "properties was null");
        Objects.requireNonNull(messageBytes, "messageBytes was null");
        
        this.lock.lock();
        try {
            //
            // add listeners if not yet added
            //
            this.init();
            
            
            //
            // cancel any outstanding futures that have not yet completed?
            //
            
            final long timestamp = System.currentTimeMillis();
            int attempt = 1;

            try {
                return this.publishWithFuture(attempt, timestamp, exchangeName, routingKey, mandatory, properties, messageBytes);
            }
            catch (AlreadyClosedException e) {
                log.warn("Unable to cleanly publish message to channel (will try to re-establish channel): {}", e.getMessage());
                // try to re-establish connection...
                this.reinit();
                attempt++;
                return this.publishWithFuture(attempt, timestamp, exchangeName, routingKey, mandatory, properties, messageBytes);
            }
        }
        finally {
            this.lock.unlock();
        }
    }
    
    private RabbitPublishFuture publishWithFuture(
            int attempt,
            long timestamp,
            String exchangeName,
            String routingKey,
            boolean mandatory,
            BasicProperties properties,
            byte[] messageBytes) throws IOException, InterruptedException, TimeoutException {
        
        final long nextDeliveryTag = this.channel.getNextPublishSeqNo();
            
        log.debug("Will publish message: attempt={}, deliveryTag={}, messageId={}",
            attempt, nextDeliveryTag, properties.getMessageId());

        final DefaultRabbitPublishFuture future = new DefaultRabbitPublishFuture(
            timestamp,
            nextDeliveryTag,
            properties.getMessageId(),
            this.pendingTimeout);

        this.futures.add(future);

        try {
            this.channel.basicPublish(exchangeName, routingKey, mandatory, properties, messageBytes);
            
            return future;
        }
        catch (RuntimeException e) {
            log.error("Unable to cleanly publish message to channel", e);
            
            this.futures.remove(future);    // remove this future...
            
            throw e;
        }
    }
    
}