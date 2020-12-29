package com.fizzed.rabbitmq.ninja;

import com.fizzed.rabbitmq.util.RabbitException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class NinjaRabbitChannelProvider implements Provider<Channel> {

    private final Provider<Connection> connectionProvider;
    
    @Inject
    public NinjaRabbitChannelProvider(
            Provider<Connection> connectionProvider) {

        this.connectionProvider = connectionProvider;
    }
    
    @Override
    public Channel get() {
        final Connection connection = this.connectionProvider.get();
        try {
            return connection.createChannel();
        } catch (IOException e) {
            throw new RabbitException(e.getMessage(), e);
        }
    }
    
}