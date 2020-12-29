package com.fizzed.rabbitmq.util;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

public class RabbitMessage {
 
    private final Envelope envelope;
    private final AMQP.BasicProperties properties;
    private final byte[] body;

    public RabbitMessage(Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        this.envelope = envelope;
        this.properties = properties;
        this.body = body;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public AMQP.BasicProperties getProperties() {
        return properties;
    }

    public byte[] getBody() {
        return body;
    }

}