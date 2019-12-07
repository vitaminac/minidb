import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class MiniDBClient {
    public static void main(String... args) throws IOException {
        // TODO: get host and port from configuration file
        try (Socket socket = new Socket("localhost", 9000)) {
            try (
                    var scanner = new Scanner(new BufferedReader(new InputStreamReader(System.in)));
                    var writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
                    var reader = new Scanner(new BufferedReader(new InputStreamReader(socket.getInputStream())));
            ) {
                while (!socket.isClosed()) {
                    if (scanner.hasNext()) {
                        var line = scanner.nextLine();
                        writer.println(line);
                        writer.flush();
                        System.out.println(reader.nextLine());
                    }
                }
            }
        }
    }
}