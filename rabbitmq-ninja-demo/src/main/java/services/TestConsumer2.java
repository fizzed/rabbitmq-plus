package services;

import com.fizzed.rabbitmq.util.RabbitMessage;
import com.fizzed.rabbitmq.util.RabbitPullQueue;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import ninja.lifecycle.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TestConsumer2 {
    static private final Logger log = LoggerFactory.getLogger(TestConsumer2.class);
    
    private final Provider<Connection> connectionProvider;
    private RabbitPullQueue queue;
    private ExecutorService executor;

    @Inject
    public TestConsumer2(Provider<Connection> connectionProvider) {
        this.connectionProvider = connectionProvider;
    }
    
    @Start
    public void start() throws IOException {
        this.queue = new RabbitPullQueue(this.connectionProvider.get(), "test.request");
        this.executor = Executors.newSingleThreadExecutor();
        this.executor.submit(() -> {
            try {
//           while (true) {
               log.debug("Will wait for queue");
               RabbitMessage message = this.queue.pop(1000, TimeUnit.HOURS);
               log.debug("Popped message: {}", message);
//           } 
            
            Thread.sleep(1000000000L);
            } catch (Exception e) {
                log.error("", e);
            }
        });
    }
    
}