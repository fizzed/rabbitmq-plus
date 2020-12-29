package conf;

import com.fizzed.rabbitmq.ninja.NinjaRabbitModule;
import ninja.conf.FrameworkModule;
import ninja.conf.NinjaClassicModule;
import ninja.utils.NinjaProperties;
import services.RequestConsumers;

public class Module extends FrameworkModule {

    private final NinjaProperties ninjaProperties;
    
    public Module(NinjaProperties ninjaProperties) {
        this.ninjaProperties = ninjaProperties;
    }
    
    @Override
    protected void configure() {
        install(new NinjaClassicModule(ninjaProperties)
            .freemarker(false)
            .xml(false)
            .jpa(false)
            .cache(false));
        
        install(new NinjaRabbitModule());

        // example consumers, and pools
        bind(RequestConsumers.class);
    }

}