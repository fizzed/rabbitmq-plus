package com.fizzed.rabbitmq.util;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

public class RabbitPullQueueTest extends RabbitBaseTest {
 
    static private final String QUEUE_NAME1 = "RabbitPullQueueTestQueue1";
    static protected Channel publishChannel;

    @Before
    public void before() throws Exception {
        if (publishChannel == null) {
            publishChannel = connection.createChannel();
        }
        createQueueIfNotExists(QUEUE_NAME1);
        publishChannel.queuePurge(QUEUE_NAME1);
    }

    private void publish(String queueName, String message) throws IOException {
        final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
            .messageId(UUID.randomUUID().toString())
            .deliveryMode(2)
            .build();
        
        log.info("Will publish message id={}, body={}", properties.getMessageId(), message);
        
        publishChannel.basicPublish("", queueName, properties, message.getBytes(StandardCharsets.UTF_8));
    }
    
    @Test
    public void pop() throws Exception {
        try (RabbitPullQueue queue = new RabbitPullQueue(connection, QUEUE_NAME1)) {

            this.publish(QUEUE_NAME1, "Hello 1");

            final RabbitMessage message1 = queue.pop(2, TimeUnit.SECONDS);

            assertThat(message1, is(not(nullValue())));
            assertThat(new String(message1.getBody()), is("Hello 1"));
        }
    }
    
    @Test
    public void popArriveAfterWaiting() throws Exception {
        try (RabbitPullQueue queue = new RabbitPullQueue(connection, QUEUE_NAME1)) {

            // schedule a publish in 1 second
            final Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(750L);
                    this.publish(QUEUE_NAME1, "Hello 2"); 
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            thread.start();


            final RabbitMessage message1 = queue.pop(2, TimeUnit.SECONDS);

            assertThat(message1, is(not(nullValue())));
            assertThat(new String(message1.getBody()), is("Hello 2"));
        }
    }
    
    @Test
    public void popTimeout() throws Exception {
        try (RabbitPullQueue queue = new RabbitPullQueue(connection, QUEUE_NAME1)) {

            try {
                queue.pop(1, TimeUnit.SECONDS);
                fail();
            }
            catch (TimeoutException e) {
                // expected
            }
        }
    }
    
    @Test
    public void popNoWaitingDoesNotLoseMessages() throws Exception {
        try (RabbitPullQueue queue1 = new RabbitPullQueue(connection, QUEUE_NAME1)) {
            
            queue1.setPendingTimeout(1, TimeUnit.SECONDS);

            this.publish(QUEUE_NAME1, "Hello 1");
            final RabbitMessage message1 = queue1.pop(1, TimeUnit.SECONDS);
            assertThat(new String(message1.getBody()), is("Hello 1"));

            this.publish(QUEUE_NAME1, "Hello 2");
            this.publish(QUEUE_NAME1, "Hello 3");
            this.publish(QUEUE_NAME1, "Hello 4");
            this.publish(QUEUE_NAME1, "Hello 5");

            // make sure we "cancel" the pending messages, nack it so its requeued
            log.debug("Waiting 2 secs to ensure re-queue of messages");
            Thread.sleep(2000L);

            final RabbitMessage message2 = queue1.pop(1, TimeUnit.SECONDS);
            assertThat(new String(message2.getBody()), is("Hello 2"));

            final RabbitMessage message3 = queue1.pop(1, TimeUnit.SECONDS);
            assertThat(new String(message3.getBody()), is("Hello 3"));

            final RabbitMessage message4 = queue1.pop(1, TimeUnit.SECONDS);
            assertThat(new String(message4.getBody()), is("Hello 4"));

            final RabbitMessage message5 = queue1.pop(1, TimeUnit.SECONDS);
            assertThat(new String(message5.getBody()), is("Hello 5"));
        }
    }
    
}