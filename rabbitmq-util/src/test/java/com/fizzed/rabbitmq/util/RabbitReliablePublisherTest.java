package com.fizzed.rabbitmq.util;

import com.rabbitmq.client.AMQP;
import java.util.UUID;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

public class RabbitReliablePublisherTest extends RabbitBaseTest {
 
    static private final String QUEUE_NAME1 = "ReliablePublisherTestQueue1";

    @Before
    @Override
    public void before() throws Exception {
        super.before();
        this.createQueueIfNotExists(QUEUE_NAME1);
    }
    
    @Test
    public void sendToNonExistentExchange() throws Exception {
        final RabbitReliablePublisher sender = new RabbitReliablePublisher(this.connection);

        final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
            .messageId(UUID.randomUUID().toString())
            .deliveryMode(2)
            .build();

        final byte[] messageBytes = "Hello".getBytes();

        try {
            sender.publish("ExchangeDoesNotExist", "QueueDoesNotExist", true, properties, messageBytes)
                .await();

            fail();
        }
        catch (RabbitException e) {
            // expected
        }
    }
    
    @Test
    public void sendToNonExistentExchangeThenCorrectExchange() throws Exception {
        final RabbitReliablePublisher sender = new RabbitReliablePublisher(this.connection);

        final AMQP.BasicProperties properties1 = new AMQP.BasicProperties.Builder()
            .messageId(UUID.randomUUID().toString())
            .deliveryMode(2)
            .build();

        final byte[] messageBytes = "Hello".getBytes();

        try {
            sender.publish("ExchangeDoesNotExist", "QueueDoesNotExist", true, properties1, messageBytes)
                .await();

            fail();
        }
        catch (RabbitException e) {
            // expected
        }

        // NOTE: The underlying channel was closed by the previously bad call...

        final AMQP.BasicProperties properties2 = new AMQP.BasicProperties.Builder()
            .messageId(UUID.randomUUID().toString())
            .deliveryMode(2)
            .build();

        sender.publish("", QUEUE_NAME1, true, properties2, messageBytes)
            .await();
    }
    
    @Test
    public void sendToNonExistentQueue() throws Exception {
        final RabbitReliablePublisher sender = new RabbitReliablePublisher(this.connection);

        final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
            .messageId(UUID.randomUUID().toString())
            .deliveryMode(2)
            .build();

        final byte[] messageBytes = "Hello".getBytes();

        try {
            sender.publish("", "QueueDoesNotExist", true, properties, messageBytes)
                .await();

            fail();
        }
        catch (MessageReturnedException e) {
            // expected
        }
    }
    
}