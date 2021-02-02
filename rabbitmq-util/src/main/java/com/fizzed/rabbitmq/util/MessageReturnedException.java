package com.fizzed.rabbitmq.util;

public class MessageReturnedException extends RabbitException {

    private final int replyCode;
    private final String replyText;
    private final String exchange;
    private final String routingKey;

    public MessageReturnedException(int replyCode, String replyText, String exchange, String routingKey) {
        super("Message returned " + replyText + " ["
            + "code=" + replyCode
            + ", exchange=" + exchange
            + ", routingKey=" + routingKey
            + "]");
        this.replyCode = replyCode;
        this.replyText = replyText;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public int getReplyCode() {
        return replyCode;
    }

    public String getReplyText() {
        return replyText;
    }

    public String getExchange() {
        return exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

}