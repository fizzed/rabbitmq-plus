package com.fizzed.rabbitmq.util;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class RabbitChannelPoolConfig extends GenericObjectPoolConfig {

    public RabbitChannelPoolConfig() {
        this.setMaxTotal(5);
        this.setMinEvictableIdleTimeMillis(30000L);
    }
    
}