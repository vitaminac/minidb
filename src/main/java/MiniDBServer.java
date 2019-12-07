import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.StringTokenizer;
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
            // OBJECT OUTPUT STREAM, OBJECT INPUT STREAM
            try (
                    var reader = new Scanner(new BufferedReader(new InputStreamReader(this.socket.getInputStream())));
                    var writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream())))
            ) {
                while (!this.socket.isClosed()) {
                    if (reader.hasNext()) {
                        var line = reader.nextLine();
                        System.out.println("Received: " + line);
                        var tokenizer = new StringTokenizer(line);
                        if (tokenizer.countTokens() > 0) {
                            var command = tokenizer.nextToken();
                            if (command.equals("PING")) {
                                writer.println("PONG");
                                writer.flush();
                            } else if (command.equals("SET")) {
                                var key = tokenizer.nextToken();
                                var value = tokenizer.nextToken();
                                System.out.println("ADIOS");
                                DICT.put(key, value);
                                writer.println("OK");
                                writer.flush();
                                System.out.println("HOLA");
                            } else if (command.equals("DEL")) {
                                var key = tokenizer.nextToken();
                                DICT.remove(key);
                                writer.println("OK");
                                writer.flush();
                            } else if (command.equals("GET")) {
                                var key = tokenizer.nextToken();
                                writer.println(DICT.get(key).toString());
                                writer.flush();
                            } else if (command.equals("EXISTS")) {
                                var key = tokenizer.nextToken();
                                writer.println(DICT.containsKey(key) ? "YES" : "NO");
                                writer.flush();
                            } else if (command.equals("QUIT")) {
                                writer.println("QUITING");
                                writer.flush();
                                this.socket.close();
                            } else {
                                writer.println("ERR: unknown command " + command);
                                writer.flush();
                            }
                        }
                    }
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
