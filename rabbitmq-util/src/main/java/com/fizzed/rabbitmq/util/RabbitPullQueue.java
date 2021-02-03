package com.fizzed.rabbitmq.util;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitPullQueue<E> implements Closeable {
    static private final Logger log = LoggerFactory.getLogger(RabbitPullQueue.class);
    
    private final String routingKey;
    private final Connection connection;
    // state
    private final ReentrantLock lock;
    private final Condition messageReceivedCondition;
    private final Condition messagePoppedCondition;
    private final ConsumerImpl consumer;
    private long pendingTimeout;
    private Channel channel;
    private String consumerTag;
    private RabbitMessage pendingMessage;
    private Long pendingDeliveryTag;
    private volatile int waiterCount;
    
    public RabbitPullQueue(
            Connection connection,
            String routingKey) {
        
        this.connection = connection;
        this.routingKey = routingKey;
        this.lock = new ReentrantLock();
        this.messageReceivedCondition = this.lock.newCondition();
        this.messagePoppedCondition = this.lock.newCondition();
        this.consumer = new ConsumerImpl();
        this.waiterCount = 0;
        this.pendingTimeout = 5000;     // 5 secs by default
    }

    public void setPendingTimeout(long timeout, TimeUnit tu) {
        this.pendingTimeout = tu.toMillis(timeout);
    }

    public long getPendingTimeout() {
        return pendingTimeout;
    }

    public int getWaiterCount() {
        return this.waiterCount;
    }
    
    @Override
    public void close() throws IOException {
        this.stop();
    }
    
    public void stop() {
        this.nack(true);
        RabbitHelper.closeQuietly(this.channel);
        this.channel = null;
    }
    
    public RabbitMessage pop(long timeout, TimeUnit tu) throws IOException, InterruptedException, TimeoutException {
        this.lock.lock();
        try {
            this.waiterCount++;
            
            // create channel
            if (this.channel == null || !this.channel.isOpen()) {
                this.consumerTag = null;
                this.channel = this.connection.createChannel();
                log.debug("Created channel {} for pulling from routingKey {}",
                    this.channel.getChannelNumber(), this.routingKey);
            }
            
            // start consuming
            if (this.consumerTag == null) {
                this.channel.basicQos(1, false);     // only 1 pending message at a time...
                this.consumerTag = this.channel.basicConsume(this.routingKey, this.consumer);
                log.debug("Created consumer {} for pulling from routingKey {}",
                    this.consumerTag, this.routingKey);
            }
            
            // message not ready yet?
            if (this.pendingMessage == null) {
                // wait for one to arrive
                if (!this.messageReceivedCondition.await(timeout, tu)) {
                    throw new TimeoutException("Timeout while waiting");
                }
            }
            
            if (this.consumerTag == null) {
                // consuming must have been canceled while we were waiting
                throw new IOException("Consumer was canceled");
            }
         
            if (this.pendingMessage == null) {
                // NOTE: this should actually be impossible
                throw new IllegalStateException("Received condition signaled but message was null");
            }
            
            if (this.pendingDeliveryTag == null) {
                throw new IllegalStateException("Received condition signaled but deliveryTag was null");
            }
            
            RabbitMessage v = this.pendingMessage;
            
            this.channel.basicAck(this.pendingDeliveryTag, false);
            
            this.pendingMessage = null;
            this.pendingDeliveryTag = null;
            
            this.messagePoppedCondition.signal();
            
            return v;
        } finally {
            this.waiterCount--;
            this.lock.unlock();
        }
    }
    
    private void nack(
            boolean cancelConsuming) {
        
        this.lock.lock();
        try {
            // cancel consuming too?
            if (cancelConsuming) {
                if (this.channel != null) {
                    if (this.consumerTag != null) {
                        log.debug("Canceling consuming {}", this.routingKey);
                        try {
                            this.channel.basicCancel(this.consumerTag);
                        } catch (Exception e) {
                            // if the consumer does not exist, its possible the above errors out
                            log.error("Unable to cleanly cancel consuming: {}", e.getMessage());
                        }
                    }
                }

                this.consumerTag = null;

                // signal all waiting threads consuming has been canceled
                this.messageReceivedCondition.signalAll();
            }

            if (this.pendingDeliveryTag != null) {
                log.debug("Will nack any pending messages up to and including deliveryTag {}", this.pendingDeliveryTag);
                
                try {
                    this.channel.basicNack(this.pendingDeliveryTag, true, true);
                } catch (Exception e) {
                    log.error("Unable to cleanly nack: {}", e.getMessage(), e);
                }

                this.pendingDeliveryTag = null;
            }

            this.pendingMessage = null;
        } finally {
            this.lock.unlock();
        }
    }
    
    private class ConsumerImpl implements Consumer {

        @Override
        public void handleConsumeOk(String arg0) {
            
        }

        @Override
        public void handleCancelOk(String arg0) {
            
        }

        @Override
        public void handleCancel(String arg0) throws IOException {
            log.warn("Received cancel request, will shutdown consuming");
            RabbitPullQueue.this.nack(true);
        }

        @Override
        public void handleShutdownSignal(String arg0, ShutdownSignalException e) {
            log.warn("Received shutdown signal, will shutdown consuming");
            RabbitPullQueue.this.nack(true);
        }

        @Override
        public void handleRecoverOk(String arg0) {
            log.warn("handleRecoverOk");
        }

        @Override
        public void handleDelivery(
                String consumerTag,
                Envelope envelope,
                AMQP.BasicProperties properties,
                byte[] body)
                throws IOException {

            final RabbitPullQueue _this = RabbitPullQueue.this;
            final long deliveryTag = envelope.getDeliveryTag();
            final String messageId = properties != null ? properties.getMessageId() : null;
            final int bodySize = body != null ? body.length : 0;
            
            log.trace("Received message id={}, deliveryTag={} ({} bytes)", messageId, deliveryTag, bodySize);

            _this.lock.lock();
            try {
                //
                // do we already have a message waiting?
                //
                
                if (_this.pendingMessage != null) {
                    if (!_this.messagePoppedCondition.await(_this.pendingTimeout, TimeUnit.MILLISECONDS)) {
                        log.debug("Will cancel consumer and nack pending messages due to pending message already present");
                        _this.nack(true);
                        // NOTE: we want to cancel this pending message too!
                        _this.channel.basicNack(deliveryTag, false, false);
                        return;
                    }
                }
                
                _this.pendingMessage = new RabbitMessage(envelope, properties, body);
                _this.pendingDeliveryTag = deliveryTag;
                _this.messageReceivedCondition.signal();
                
                //
                // wait until its been popped and handled
                //
                
                if (!_this.messagePoppedCondition.await(_this.pendingTimeout, TimeUnit.MILLISECONDS)) {
                    log.debug("Will cancel consumer and nack pending messages due to timeout of pending message");
                    _this.nack(true);
                }
            }
            catch (InterruptedException e) {
//                log.debug("Interrupted exception", e);
                log.debug("Will cancel consumer and nack pending messages due to interrupted exception!");
               _this.nack(true);
            }
            finally {
                _this.lock.unlock();
            }
        }
        
    }

}