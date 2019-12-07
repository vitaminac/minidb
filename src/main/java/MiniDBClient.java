import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.StringTokenizer;

public class MiniDBClient {
    public static void main(String... args) throws IOException {
        // TODO: get host and port from configuration file
        try (
                var conn = new MiniDBConnector("localhost", 9000);
                var scanner = new Scanner(new BufferedReader(new InputStreamReader(System.in)));
        ) {
            while (conn.isAlive()) {
                if (scanner.hasNextLine()) {
                    var line = scanner.nextLine();
                    var tokenizer = new StringTokenizer(line);
                    if (tokenizer.countTokens() > 0) {
                        Result result;
                        var command = tokenizer.nextToken();
                        switch (command) {
                            case "PING":
                                result = conn.ping();
                                break;
                            case "SET": {
                                var key = tokenizer.nextToken();
                                var value = tokenizer.nextToken();
                                result = conn.set(key, value);
                                break;
                            }
                            case "DEL": {
                                var key = tokenizer.nextToken();
                                result = conn.delete(key);
                                break;
                            }
                            case "GET": {
                                var key = tokenizer.nextToken();
                                result = conn.get(key);
                                break;
                            }
                            case "EXISTS": {
                                var key = tokenizer.nextToken();
                                result = conn.exists(key);
                                break;
                            }
                            case "QUIT":
                                result = conn.quit();
                                break;
                            default:
                                result = Result.fail("ERR: unknown command " + command);
                        }
                        System.out.println(result.getExtras());
                    }
                }
            }
        }
    }
}