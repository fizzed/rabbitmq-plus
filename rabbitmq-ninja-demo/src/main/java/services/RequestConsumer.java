package services;

import com.fizzed.crux.util.TimeDuration;
import com.fizzed.executors.core.ExecuteStopException;
import com.fizzed.executors.core.Worker;
import com.fizzed.executors.core.WorkerContext;
import com.fizzed.rabbitmq.util.RabbitMessage;
import com.fizzed.rabbitmq.util.RabbitPullQueue;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestConsumer implements Worker {
    static private final Logger log = LoggerFactory.getLogger(RequestConsumer.class);

    private RabbitPullQueue<String> queue;
    
    @Inject
    public RequestConsumer() {
    }

    public RabbitPullQueue<String> getQueue() {
        return queue;
    }

    public void setQueue(RabbitPullQueue<String> queue) {
        this.queue = queue;
    }
    
    @Override
    public Logger getLogger() {
        return log;
    }
    
    @Override
    public void execute(WorkerContext context) throws ExecuteStopException, InterruptedException {
       while (true) {
            try {
                context.idle(TimeDuration.seconds(0), "Waiting for message");
                
                final RabbitMessage message = this.queue.pop(1, TimeUnit.HOURS);
                
                if (message != null) {
                    log.debug("Processing message {}: {}", message.getEnvelope().getDeliveryTag(), new String(message.getBody()));

                    context.running("Processing message " + message.getEnvelope().getDeliveryTag());

                    Thread.sleep(TimeDuration.seconds(15).asMillis());
                }
            }
            catch (InterruptedException e) {
                log.debug("Interrupted, exiting");
                return;
            }
            catch (IOException | TimeoutException e) {
                log.error("Unable to poll", e);
                throw new RuntimeException(e);
            }
       }
    }
    
}