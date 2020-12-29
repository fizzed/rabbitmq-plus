package com.fizzed.rabbitmq.ninja;

import com.fizzed.rabbitmq.util.RabbitException;
import com.fizzed.rabbitmq.util.RabbitHelper;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import ninja.lifecycle.Start;
import ninja.utils.NinjaProperties;

@Singleton
public class NinjaRabbitLifecycle {
 
    private final Provider<Connection> connectionProvider;
    private final List<String> verifyQueues;
    
    @Inject
    public NinjaRabbitLifecycle(
            NinjaProperties ninjaProperties,
            Provider<Connection> connectionProvider) {
        
        this.connectionProvider = connectionProvider;
        
        String[] queueNames = ninjaProperties.getStringArray("rabbitmq.verify_queues");
        if (queueNames != null && queueNames.length > 0) {
            this.verifyQueues = asList(queueNames);
        } else {
            this.verifyQueues = null;
        }
    }
    
    @Start
    public void start() throws IOException {
        if (this.verifyQueues != null && !this.verifyQueues.isEmpty()) {
            final Connection connection = this.connectionProvider.get();
            final List<String> missingQueueNames = new ArrayList<>();
            
            for (String queueName : this.verifyQueues) {
                if (!RabbitHelper.queueExists(connection, queueName)) {
                    missingQueueNames.add(queueName);
                }
            }
            
            if (!missingQueueNames.isEmpty()) {
                throw new RabbitException("Queues in RabbitMQ do not exist: " + missingQueueNames);
            }
        }
    }
    
}