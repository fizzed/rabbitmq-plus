package com.fizzed.rabbitmq.util;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitBaseTest {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
 
    static protected ConnectionFactory connectionFactory;
    static protected Connection connection;
    
    @BeforeClass
    static public void beforeClass() throws Exception {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setUri("amqp://localhost:27672");
        connectionFactory.setUsername("root");
        connectionFactory.setPassword("test");
        connection = connectionFactory.newConnection();
    }
    
    @AfterClass
    static public void afterClass() throws Exception {
        if (connection != null) {
            try {
                connection.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    static public void createQueueIfNotExists(
            String queueName) throws IOException {
        
        final Channel channel = connection.createChannel();
        try {
            channel.queueDeclare(queueName, false, false, false, null);
        }
        finally {
            RabbitHelper.closeQuietly(channel);
        }
    }
    
}