package services;

import com.fizzed.crux.util.TimeDuration;
import com.fizzed.executors.core.WorkerService;
import com.fizzed.executors.ninja.NinjaWorkerService;
import com.google.inject.Injector;
import com.fizzed.rabbitmq.util.RabbitPullQueue;
import com.rabbitmq.client.Connection;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import ninja.lifecycle.Dispose;
import ninja.lifecycle.Start;
import ninja.utils.NinjaProperties;

@Singleton
public class RequestConsumers extends WorkerService<RequestConsumer> {

    private final NinjaProperties ninjaProperties;
    private final Injector injector;
    private final Provider<Connection> connectionProvider;
    private RabbitPullQueue<String> queue;
    
    @Inject
    public RequestConsumers(
            NinjaProperties ninjaProperties,
            Injector injector,
            Provider<Connection> connectionProvider) {
        
        super("RequestConsumer");
        this.ninjaProperties = ninjaProperties;
        this.injector = injector;
        
        this.setExecuteDelay(TimeDuration.seconds(5));
        this.setInitialDelay(TimeDuration.seconds(10));
        this.setInitialDelayStagger(0.5);
        
        this.connectionProvider = connectionProvider;
    }
    
    @Override
    public RequestConsumer newWorker(long workerId, String workerName) {
        log.info("Building request consumer worker...");
        RequestConsumer worker = this.injector.getInstance(RequestConsumer.class);
        worker.setQueue(this.queue);
        return worker;
    }

    @Override @Start(order = 91)
    public void start() {
        // delegate most of configuration to helper method
        NinjaWorkerService.configure("demo.request_consumer", this.ninjaProperties, this);
        
        if (this.queue == null) {
            this.queue = new RabbitPullQueue<>(this.connectionProvider.get(), "test.request");
        }
        
        super.start();
    }
    
    @Override @Dispose
    public void stop() {
        super.stop();
    }

}