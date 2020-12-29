package com.fizzed.rabbitmq.util;

import java.io.IOException;

public interface RabbitMessageCodec<T> {
    
    T deserialize(RabbitMessage message) throws IOException;
    
}