package services;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import ninja.lifecycle.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TestConsumer {
    static private final Logger log = LoggerFactory.getLogger(TestConsumer.class);
    
    private final Provider<Channel> channelProvider;

    @Inject
    public TestConsumer(Provider<Channel> channelProvider) {
        this.channelProvider = channelProvider;
    }
    
    @Start
    public void start() throws IOException {
        final Channel channel = this.channelProvider.get();
        
        channel.basicConsume("test.request", new DefaultConsumer(channel) {
            @Override
            public void handleCancel(String consumerTag) throws IOException {
                log.info("Consumer cancel {}", consumerTag);
            }

            @Override
            public void handleCancelOk(String consumerTag) {
                log.info("Consumer cancel ok {}", consumerTag);
            }

            @Override
            public void handleRecoverOk(String consumerTag) {
                log.info("Consumer recover ok {}", consumerTag);
            }

            @Override
            public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
                log.info("Consumer shutdown signal {}", consumerTag);
            }

            @Override
            public void handleConsumeOk(String consumerTag) {
                log.info("Consumer consume ok {}", consumerTag);
            }

            @Override
            public void handleDelivery(
                    String consumerTag,
                    Envelope envelope,
                    AMQP.BasicProperties properties,
                    byte[] body)
                    throws IOException {

                String routingKey = envelope.getRoutingKey();
                String contentType = properties.getContentType();
                long deliveryTag = envelope.getDeliveryTag();

                log.info("Received: {} on thread {}", body, Thread.currentThread());
                log.info(" envelope: {}", envelope);
                log.info(" props: {}", properties);

//                log.info("About to cancel consumer {}", consumerTag);
//                channel.basicCancel(consumerTag);
//                channel.basicRecover(true);

//                log.info("Pausing 5 secs before ack...");
//                try {
//                    Thread.sleep(5000L);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
                
                // (process the message components here ...)
                log.info("Sending ack!");
                channel.basicAck(deliveryTag, false);
            }
        });
        
        log.info("Channel basicConsume returned...");
    }
    
}