import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.*;

public class MiniDBServer {
    // TODO: java.nio
    public static void main(String... args) throws IOException, InterruptedException {
        // TODO: don't use Executors
        ExecutorService executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() + 1, 32, 1, TimeUnit.HOURS, new LinkedBlockingQueue<>(255));
        // TODO get port from args
        var serverSocket = new ServerSocket(9000);
        while (!serverSocket.isClosed()) {
            var client = serverSocket.accept();
            System.out.println("Received new connection from " + client.getRemoteSocketAddress().toString());
            executorService.submit(new ClientHandler(client));
        }
        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.SECONDS);
    }
}
