import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MiniDBServer {
    private static final ConcurrentHashMap<Object, Object> DICT = new ConcurrentHashMap<>();

    private static class ClientHandler implements Runnable {
        private final Socket socket;

        private ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            // TODO: NON-BLOCKING
            try (
                    // TODO: Buffer output
                    var oos = new ObjectOutputStream(this.socket.getOutputStream());
                    var ois = new ObjectInputStream(this.socket.getInputStream());
            ) {
                while (!this.socket.isClosed()) {
                    Command command = (Command) ois.readObject();
                    switch (command.getType()) {
                        case GET:
                            oos.writeObject(Result.ok(DICT.get(command.getExtras())));
                            break;
                        case PING:
                            oos.writeObject(Result.ok("PONG"));
                            break;
                        case DEL:
                            oos.writeObject(Result.ok(DICT.remove(command.getExtras())));
                            break;
                        case SET:
                            DictEntry entry = (DictEntry) command.getExtras();
                            DICT.put(entry.getKey(), entry.getValue());
                            oos.writeObject(Result.ok(entry));
                            break;
                        case EXISTS:
                            oos.writeObject(Result.ok(DICT.containsKey(command.getExtras()) ? "YES" : "NO"));
                            break;
                        case QUIT:
                            oos.writeObject(Result.ok("Quitting"));
                            this.socket.shutdownInput();
                            this.socket.shutdownOutput();
                            return;
                        default:
                            oos.writeObject(Result.fail("ERR: unknown command"));
                    }
                    oos.flush();
                }
            } catch (Exception e) {
                // TODO: logging
                e.printStackTrace();
            } finally {
                try {
                    this.socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // TODO: java.nio
    public static void main(String... args) throws IOException, InterruptedException {
        // TODO: don't use Executors
        ExecutorService executorService = Executors.newFixedThreadPool(10);
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
