package com.fizzed.rabbitmq.util;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.io.IOException;

public class RabbitHelper {
 
    static public void closeQuietly(
            Channel channel) {
        
        if (channel != null) {
            try {
                channel.close();
            } catch (Exception e) {
                // do nothing
            }
        }
    }
    
    static public boolean queueExists(
            Connection connection,
            String queueName) throws IOException {
        
        Channel channel = null;
        try {
            channel = connection.createChannel();
            try {
                channel.queueDeclarePassive(queueName);
            } catch (IOException e) {
                // thrown if the queue does not exist
                return false;
            }
            
            return true;
        }
        catch (IOException e) {
            closeQuietly(channel);
            throw e;
        }
    }
    
}