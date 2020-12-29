package com.fizzed.rabbitmq.util;

import com.fizzed.rabbitmq.util.RabbitHelper;
import com.rabbitmq.client.Channel;
import java.util.UUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class RabbitHelperTest extends RabbitBaseTest {
 
    @Test
    public void queueExists() throws Exception {
        try (Channel channel = this.connection.createChannel()) {
            boolean exists;
            
            String queueName = "test-does-not-exist-" + UUID.randomUUID();
            
            exists = RabbitHelper.queueExists(this.connection, queueName);
            
            assertThat(exists, is(false));
            
            // create the queue now
            try {
                channel.queueDeclare(queueName, true, false, false, null);
                
                exists = RabbitHelper.queueExists(this.connection, queueName);
            
                assertThat(exists, is(true));
            } finally {
                channel.queueDelete(queueName);
            }
        }
    }
    
}