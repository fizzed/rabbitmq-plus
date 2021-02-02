package com.fizzed.rabbitmq.util.impl;

import com.fizzed.rabbitmq.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DefaultRabbitPublishFuture implements RabbitPublishFuture {
 
    private final long sentAt;
    private final long deliveryTag;
    private final String messageId;
    private final long timeoutMillis;
    private final CountDownLatch completedLatch;
    private RabbitException exception;
    
    public DefaultRabbitPublishFuture(
            long sentAt,
            long deliveryTag,
            String messageId,
            long timeoutMillis) {
        
        this.sentAt = sentAt;
        this.deliveryTag = deliveryTag;
        this.messageId = messageId;
        this.timeoutMillis = timeoutMillis;
        this.completedLatch = new CountDownLatch(1);
    }

    @Override
    public long getSentAt() {
        return sentAt;
    }

    @Override
    public long getDeliveryTag() {
        return deliveryTag;
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    public void success() {
        this.completedLatch.countDown();
    }
    
    public void failure(RabbitException exception) {
        this.exception = exception;
        this.completedLatch.countDown();
    }
    
    @Override
    public void await() throws InterruptedException, TimeoutException, RabbitException {
        if (!this.completedLatch.await(this.timeoutMillis, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("Timeout waiting for message delivery (tag="
                + this.deliveryTag + ", messageId=" + this.messageId + ")");
        }
        
        if (this.exception != null) {
            throw this.exception;
        }
    }

}