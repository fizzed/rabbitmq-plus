package com.fizzed.rabbitmq.ninja;

import ninja.utils.NinjaProperties;
import org.junit.Before;
import static org.mockito.Mockito.mock;

public class NinjaRabbitChannelProviderTest {
 
    private NinjaProperties ninjaProperties;
    
    @Before
    public void before() {
        this.ninjaProperties = mock(NinjaProperties.class);
    }
    
//    @Test
//    public void buildCluster() {
//        when(ninjaProperties.getStringArray("cassandra.contact_points"))
//            .thenReturn(new String[] { "localhost:9403" });
//        
//        Cluster.Builder clusterBuilder = new NinjaCassandraClusterProvider(ninjaProperties)
//            .createBuilder();
//        
//        assertThat(clusterBuilder.getContactPoints(), hasSize(1));
//        
//        Cluster cluster = clusterBuilder.build();
//    }
    
}