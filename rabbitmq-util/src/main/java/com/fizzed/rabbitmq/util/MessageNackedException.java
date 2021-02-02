package com.fizzed.rabbitmq.util;

public class MessageNackedException extends RabbitException {

    private final String messageId;

    public MessageNackedException(String messageId) {
        super("Message " + messageId + " delivery failed to broker");
        this.messageId = messageId;
    }

    public String getMessageId() {
        return messageId;
    }
    
}