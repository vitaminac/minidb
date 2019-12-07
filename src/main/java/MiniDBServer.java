import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.StringTokenizer;

public class MiniDBServer {

    private static class ClientHandler implements Runnable {
        private final Socket socket;

        private ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            // TODO: NON-BLOCKING
            try (var reader = new Scanner(this.socket.getInputStream());
                 var writer = new PrintWriter(this.socket.getOutputStream())
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
                            } else if (command.equals("QUIT")) {
                                writer.write("QUITING");
                                writer.flush();
                                this.socket.close();
                            } else {
                                writer.println("ERROR: unknown command " + command);
                                writer.flush();
                            }
                        }
                    }
                }
            } catch (IOException e) {
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
    public static void main(String... args) throws IOException {
        // TODO get port from args
        var serverSocket = new ServerSocket(9000);
        while (!serverSocket.isClosed()) {
            var client = serverSocket.accept();
            System.out.println("Received new connection from " + client.getRemoteSocketAddress().toString());
            new Thread(new ClientHandler(client)).start();
        }
        // TODO: use executor service
    }
}
