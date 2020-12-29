
import com.fizzed.blaze.Config;
import com.fizzed.blaze.Contexts;
import static com.fizzed.blaze.Systems.exec;
import com.fizzed.blaze.Task;
import java.io.IOException;
import org.slf4j.Logger;

public class blaze {
    private final Logger log = Contexts.logger();
    private final Config config = Contexts.config();

    @Task(order=1, value="Setup depenencies")
    public void setup() throws InterruptedException {
        log.info("Setting up rabbit queues...");
        exec("docker", "exec", "-i", "greenback-rabbit",
            "rabbitmqadmin",
            "-u", "root", "-p", "test",
            "declare", "queue", "name=test.request", "durable=true")
            .exitValues(0)
            .run();
    }

    @Task(order=2, value="Nukes all setup. Should get you starting from scratch.")
    public void nuke() {
        log.info("Nuking up rabbit queues...");
        exec("docker", "exec", "-i", "greenback-rabbit",
            "rabbitmqadmin",
            "-u", "root", "-p", "test",
            "delete", "queue", "name=test.request")
            .exitValues(0)
            .run();
    }

    @Task(order=11, value="Run ninja")
    public void ninja() throws IOException {
        exec("mvn", "-Pninja-run", "process-classes").run();
    }
    
}