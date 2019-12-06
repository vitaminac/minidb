import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.StringTokenizer;

public class MiniDBServer {

    public static void main(String... args) {
        try (var sc = new Scanner(new BufferedReader(new InputStreamReader(System.in)))) {
            var ended = false;
            while (!ended) {
                if (sc.hasNext()) {
                    var line = sc.nextLine();
                    var tokenizer = new StringTokenizer(line);
                    if (tokenizer.countTokens() > 0) {
                        var command = tokenizer.nextToken();
                        if (command.equals("PING")) {
                            System.out.println("PONG");
                        } else {
                            System.out.println("ERROR: unknown command " + command);
                        }
                    }
                } else {
                    Thread.yield();
                }
            }
        }
    }
}
