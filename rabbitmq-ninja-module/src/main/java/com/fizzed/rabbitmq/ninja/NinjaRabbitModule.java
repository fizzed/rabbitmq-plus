package com.fizzed.rabbitmq.ninja;

import com.fizzed.rabbitmq.util.RabbitChannelPool;
import com.google.inject.AbstractModule;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

public class NinjaRabbitModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(Connection.class).toProvider(NinjaRabbitConnectionProvider.class);
        bind(Channel.class).toProvider(NinjaRabbitChannelProvider.class);
        bind(RabbitChannelPool.class).toProvider(NinjaRabbitChannelPoolProvider.class);
        bind(NinjaRabbitLifecycle.class);
    }
    
}