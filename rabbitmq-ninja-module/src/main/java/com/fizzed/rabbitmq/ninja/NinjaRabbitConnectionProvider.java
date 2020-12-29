package com.fizzed.rabbitmq.ninja;

import com.fizzed.rabbitmq.util.RabbitException;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import ninja.utils.NinjaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class NinjaRabbitConnectionProvider implements Provider<Connection> {
    static private final Logger log = LoggerFactory.getLogger(NinjaRabbitConnectionProvider.class);
    
    private final ConnectionFactory factory;
    private final String connectionName;
    private final ExecutorService executor;
    private final Supplier<Connection> connectionSupplier;
    
    @Inject
    public NinjaRabbitConnectionProvider(
            NinjaProperties ninjaProperties) {

        this.factory = new ConnectionFactory();
        this.executor = Executors.newCachedThreadPool();
        
        final String url = ninjaProperties.get("rabbitmq.url");
        if (url != null) {
            // factory.setUri("amqp://userName:password@hostName:portNumber/virtualHost");
            try {
                this.factory.setUri(url);
            } catch (Exception e) {
                throw new RabbitException(e.getMessage(), e);
            }
        }
        
        final String user = ninjaProperties.get("rabbitmq.user");
        if (user != null) {
            this.factory.setUsername(user);
        }
        
        final String password = ninjaProperties.get("rabbitmq.password");
        if (password != null) {
            this.factory.setPassword(password);
        }
        
        final String _connectionName = ninjaProperties.get("rabbitmq.connection_name");
        if (_connectionName != null) {
            this.connectionName = _connectionName;
        } else {
            this.connectionName = null;
        }
        
        this.connectionSupplier = Suppliers.memoize(() -> {
           try {
                log.info("Connecting to RabbitMQ @ amq://{}:{}{}",
                    this.factory.getHost(), this.factory.getPort(), this.factory.getVirtualHost());

                return this.factory.newConnection(this.executor, this.connectionName);
            }
            catch (IOException | TimeoutException e) {
                throw new RabbitException("Unable to create connection", e);
            }
        });
    }
    
    @Override
    public Connection get() {
        return this.connectionSupplier.get();
    }
    
}