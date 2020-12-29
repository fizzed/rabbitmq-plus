package com.fizzed.rabbitmq.ninja;

import com.fizzed.rabbitmq.util.RabbitChannelPool;
import com.fizzed.rabbitmq.util.RabbitChannelPoolConfig;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.rabbitmq.client.Connection;
import static java.util.Optional.ofNullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import ninja.utils.NinjaProperties;

@Singleton
public class NinjaRabbitChannelPoolProvider implements Provider<RabbitChannelPool> {

    private final Supplier<RabbitChannelPool> channelPoolSupplier;
    
    @Inject
    public NinjaRabbitChannelPoolProvider(
            NinjaProperties ninjaProperties,
            Provider<Connection> connectionProvider) {

        this.channelPoolSupplier = Suppliers.memoize(() -> {
            final RabbitChannelPoolConfig config = new RabbitChannelPoolConfig();
            
            final String prefix = "rabbitmq.pool.";

            ofNullable(ninjaProperties.getInteger(prefix + "min_idle")).ifPresent(v -> {
                config.setMinIdle(v);
            });

            ofNullable(ninjaProperties.getInteger(prefix + "max_idle")).ifPresent(v -> {
                config.setMaxIdle(v);
            });

            ofNullable(ninjaProperties.getInteger(prefix + "max_total")).ifPresent(v -> {
                config.setMaxTotal(v);
            });
            
            ofNullable(ninjaProperties.getInteger(prefix + "evictable_idle_time_millis")).ifPresent(v -> {
                config.setMinEvictableIdleTimeMillis(v);
                config.setTimeBetweenEvictionRunsMillis(v*2);
            });
            
            return new RabbitChannelPool(connectionProvider.get(), config);
        });
    }
    
    @Override
    public RabbitChannelPool get() {
        return this.channelPoolSupplier.get();
    }
    
}