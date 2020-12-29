package com.fizzed.rabbitmq.util;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.After;
import org.junit.Before;

public class RabbitBaseTest {
 
    protected ConnectionFactory connectionFactory;
    protected Connection connection;
    
    @Before
    public void before() throws Exception {
        this.connectionFactory = new ConnectionFactory();
        this.connectionFactory.setUri("amqp://localhost:5672");
        this.connectionFactory.setUsername("root");
        this.connectionFactory.setPassword("test");
        
        this.connection = this.connectionFactory.newConnection();
    }
    
    @After
    public void after() throws Exception {
        if (this.connection != null) {
            try {
                this.connection.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
}