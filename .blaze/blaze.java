
import com.fizzed.blaze.Config;
import com.fizzed.blaze.Contexts;
import static com.fizzed.blaze.Systems.exec;
import com.fizzed.blaze.Task;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import org.slf4j.Logger;

public class blaze {
    private final Logger log = Contexts.logger();

    @Task(order=1, value="Setup depenencies")
    public void setup() throws InterruptedException {
        this.setupRabbit();
        log.info("Setting up rabbit queues...");
        exec("docker", "exec", "-i", "fizzed-rabbitmq-plus",
            "rabbitmqadmin",
            "-u", "root", "-p", "test",
            "declare", "queue", "name=test.request", "durable=true")
            .exitValues(0)
            .run();
    }

    @Task(order=2, value="Nukes all setup. Should get you starting from scratch.")
    public void nuke() {
        log.info("Nuking up rabbit queues...");
        exec("docker", "exec", "-i", "fizzed-rabbitmq-plus",
            "rabbitmqadmin",
            "-u", "root", "-p", "test",
            "delete", "queue", "name=test.request")
            .exitValues(0, 1)
            .run();
        this.nukeRabbit();
    }

    public void setupRabbit() {
        log.info("Setting up rabbit mq...");
        exec("docker", "run", "--name", "fizzed-rabbitmq-plus",
            "-p", "27672:5672", "-p", "28672:15672",
            "-e", "RABBITMQ_DEFAULT_USER=root",
            "-e", "RABBITMQ_DEFAULT_PASS=test",
            "-d",
            "rabbitmq:3.8.11-management")
            .exitValues(0, 125)
            .run();
        this.waitForHttp(28672);
    }

    @Task(order=1403)
    public void nukeRabbit() {
        exec("docker", "rm", "-f", "fizzed-rabbitmq-plus").exitValues(0, 1).run();
    }
    
    @Task(order=11, value="Run ninja")
    public void ninja() throws IOException {
        exec("mvn", "-Pninja-run", "process-classes").run();
    }
    
    private void waitForHttp(int port) {
        try {
            for (int i = 0; i < 30; i++) {
                log.info("Waiting for port {} to be ready", port);
                if (i > 0) {
                    Thread.sleep(1000L);
                }
                try {
                    URL url = new URL("http://localhost:" + port);
                    try (InputStream is = url.openStream()) {
                        return;
                    }
                } catch (Exception e) {
                    // do nothing
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        throw new RuntimeException("Unable to confirm port " + port);
    }
    
}