package com.fizzed.rabbitmq.util;

import java.util.concurrent.TimeoutException;

public interface RabbitPublishFuture {
 
    long getSentAt();

    long getDeliveryTag();

    String getMessageId();
    
    void await() throws InterruptedException, TimeoutException, RabbitException;
    
}