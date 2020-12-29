package controllers;

import com.fizzed.rabbitmq.util.RabbitChannelPool;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import ninja.Result;
import ninja.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.RequestConsumers;

@Singleton
public class ApplicationController {
    static private final Logger log = LoggerFactory.getLogger(ApplicationController.class);
    
    private final RequestConsumers requestConsumers;
    private final RabbitChannelPool channelPool;
    private final AtomicInteger messageCounter;
    
    @Inject
    public ApplicationController(
            RequestConsumers requestConsumers,
            RabbitChannelPool channelPool) {
        
        this.requestConsumers = requestConsumers;
        this.channelPool = channelPool;
        this.messageCounter = new AtomicInteger();
    }
    
    public Result home() throws Exception {
        
        final StringBuilder html = new StringBuilder();
        
        html.append("<a href='/publish'>Publish Message</a><br/>");
        
        html.append("<br/>");
        
        html.append("<b>Channel Pool</b><br/>");
        html.append("Max Total: ").append(this.channelPool.getMaxTotalCount()).append("<br/>");
        html.append("Min Idle: ").append(this.channelPool.getMinIdleCount()).append("<br/>");
        html.append("Max Idle: ").append(this.channelPool.getMaxIdleCount()).append("<br/>");
        html.append("Idle: ").append(this.channelPool.getIdleCount()).append("<br/>");
        html.append("Active: ").append(this.channelPool.getActiveCount()).append("<br/>");
        
        html.append("<br/>");
        
        html.append("<b>Request Consumers</b><br/>");
        
        if (this.requestConsumers.isStarted()) {
            html.append("<a href='/stop'>Stop</a><br/>");
        } else if (this.requestConsumers.isStopped()) {
            html.append("<a href='/start'>Start</a><br/>");
        } else {
            html.append("Currently ").append(this.requestConsumers.getState()).append("<br/>");
        }
        
        html.append("<br/>");
        
        this.requestConsumers.getRunnables().forEach(wr -> {
            html.append(wr.getName()).append("; ").append(wr.getState()).append("; ").append(wr.getMessage());
            html.append("<br/>");
        });
        
        return Results.html()
            .renderRaw(html.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    public Result stop() throws Exception {
        this.requestConsumers.shutdown();
        
        return Results.redirect("/");
    }
    
    public Result start() throws Exception {
        this.requestConsumers.start();
        
        return Results.redirect("/");
    }
    
    public Result publish() throws Exception {
        
        final String message = "Hello " + this.messageCounter.incrementAndGet() + " @ " + System.currentTimeMillis();
        
        this.channelPool.basicPublish("", "test.request", true, null, message.getBytes());
        
        log.debug("Published message " + message);
        
        return Results.redirect("/");
    }
    
//    public Result home2() throws Exception {
//        Map<String, Object> serverProperties;
//        
//        try (Channel channel = this.channelProvider.get()) {
//            log.info("Created channel {} on {}", channel.getChannelNumber(), channel.getConnection().getId());
//            
//            serverProperties = channel.getConnection().getServerProperties();
//            
//
//            channel.addReturnListener(rm -> {
//                log.info("Return: code={}, text={}, props={}", rm.getReplyCode(), rm.getReplyText(), rm.getProperties());
//            });
//            
//            channel.addConfirmListener(new ConfirmListener() {
//                @Override
//                public void handleAck(long l, boolean bln) throws IOException {
//                    log.info("Confirm ack!");
//                }
//
//                @Override
//                public void handleNack(long l, boolean bln) throws IOException {
//                    log.info("Confirm nack!");
//                }
//            });
//            
//            
//            channel.txSelect();
//            
//            byte[] messageBodyBytes = "{ \"a\": 1 }".getBytes();
//            
//            Map<String,Object> headers = new HashMap<>();
//            headers.put("X-Host", "localhost");
//            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
//                .messageId(UUID.randomUUID().toString())
//                .contentType("application/json")
//                .deliveryMode(2)
//                .timestamp(new Date())
//                .headers(headers)
//                .build();
//            
//            channel.basicPublish("", "test.request", true, props, messageBodyBytes);
//            
//            channel.txCommit();
//            
//            log.info("Publish message!");
//        }
//        
//        return Results.html()
//            .renderRaw(""+serverProperties);
//    }
    
}