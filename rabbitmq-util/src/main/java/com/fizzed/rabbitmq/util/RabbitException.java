package com.fizzed.rabbitmq.util;

public class RabbitException extends RuntimeException {
    
    public RabbitException(String message) {
        super(message);
    }
    
    public RabbitException(String message, Throwable cause) {
        super(message, cause);
    }
    
}