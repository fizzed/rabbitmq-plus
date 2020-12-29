package com.fizzed.rabbitmq.util;

import com.fizzed.rabbitmq.util.RabbitChannelPool;
import com.fizzed.rabbitmq.util.RabbitPooledChannel;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class RabbitChannelPoolTest extends RabbitBaseTest {
 
    @Test
    public void works() throws Exception {
        RabbitChannelPool pool = new RabbitChannelPool(this.connection);
        
        assertThat(pool.getIdleCount(), is(0));
        
        try (RabbitPooledChannel pooledChannel = pool.getChannel()) {
            assertThat(pool.getActiveCount(), is(1));
        }
        
        assertThat(pool.getActiveCount(), is(0));
        assertThat(pool.getIdleCount(), is(1));
    }
    
}