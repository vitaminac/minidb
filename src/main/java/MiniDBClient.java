import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.StringTokenizer;

public class MiniDBClient {
    public static void main(String[] args) throws IOException {
        String host = "localhost";
        int port = 9000;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h")) {
                host = args[i + 1];
            } else if (args[i].equals("-p")) {
                port = Integer.parseInt(args[i]);
            }
        }
        try (
                var conn = new MiniDBConnector(host, port);
                var scanner = new Scanner(new BufferedReader(new InputStreamReader(System.in)));
        ) {
            while (conn.isAlive()) {
                if (scanner.hasNextLine()) {
                    var line = scanner.nextLine();
                    var tokenizer = new StringTokenizer(line);
                    if (tokenizer.countTokens() > 0) {
                        Reply reply;
                        var command = tokenizer.nextToken();
                        if (command.equals(Command.CommandType.PING.name())) {
                            reply = conn.ping();
                        } else if (command.equals(Command.CommandType.SELECT.name())) {
                            reply = conn.select(Integer.parseInt(tokenizer.nextToken()));
                        } else if (command.equals(Command.CommandType.KEYS.name())) {
                            var pattern = tokenizer.nextToken();
                            reply = conn.keys(pattern);
                        } else if (command.equals(Command.CommandType.GET.name())) {
                            var key = tokenizer.nextToken();
                            reply = conn.get(key);
                        } else if (command.equals(Command.CommandType.SET.name())) {
                            var key = tokenizer.nextToken();
                            var value = tokenizer.nextToken();
                            reply = conn.set(key, value);
                        } else if (command.equals(Command.CommandType.EXISTS.name())) {
                            var key = tokenizer.nextToken();
                            reply = conn.exists(key);
                        } else if (command.equals(Command.CommandType.DEL.name())) {
                            var key = tokenizer.nextToken();
                            reply = conn.delete(key);
                        } else if (command.equals(Command.CommandType.EXPIRE.name())) {
                            var key = tokenizer.nextToken();
                            if (tokenizer.hasMoreTokens()) {
                                var milliseconds = Long.parseLong(tokenizer.nextToken());
                                reply = conn.expire(key, milliseconds);
                            } else {
                                reply = Reply.EXPIRE_SYNTAX_ERROR;
                            }
                        } else if (command.equals(Command.CommandType.HKEYS.name())) {
                            reply = conn.hkeys(tokenizer.nextToken(), tokenizer.nextToken());
                        } else if (command.equals(Command.CommandType.HGET.name())) {
                            reply = conn.hget(tokenizer.nextToken(), tokenizer.nextToken());
                        } else if (command.equals(Command.CommandType.HSET.name())) {
                            reply = conn.hset(tokenizer.nextToken(), tokenizer.nextToken(), tokenizer.nextToken());
                        } else if (command.equals(Command.CommandType.HEXISTS.name())) {
                            reply = conn.hexists(tokenizer.nextToken(), tokenizer.nextToken());
                        } else if (command.equals(Command.CommandType.HDEL.name())) {
                            reply = conn.hdelete(tokenizer.nextToken(), tokenizer.nextToken());
                        } else if (command.equals(Command.CommandType.LEN.name())) {
                            var key = tokenizer.nextToken();
                            reply = conn.length(key);
                        } else if (command.equals(Command.CommandType.FIRST.name())) {
                            reply = conn.first(tokenizer.nextToken());
                        } else if (command.equals(Command.CommandType.LAST.name())) {
                            reply = conn.last(tokenizer.nextToken());
                        } else if (command.equals(Command.CommandType.LPUSH.name())) {
                            var key = tokenizer.nextToken();
                            var value = tokenizer.nextToken();
                            reply = conn.leftPush(key, value);
                        } else if (command.equals(Command.CommandType.LPOP.name())) {
                            var key = tokenizer.nextToken();
                            reply = conn.leftPop(key);
                        } else if (command.equals(Command.CommandType.RPUSH.name())) {
                            var key = tokenizer.nextToken();
                            var value = tokenizer.nextToken();
                            reply = conn.rightPush(key, value);
                        } else if (command.equals(Command.CommandType.RPOP.name())) {
                            var key = tokenizer.nextToken();
                            reply = conn.rightPop(key);
                        } else if (command.equals(Command.CommandType.TYPE.name())) {
                            var key = tokenizer.nextToken();
                            reply = conn.type(key);
                        } else if (command.equals(Command.CommandType.QUIT.name())) {
                            reply = conn.quit();
                            conn.close();
                        } else {
                            reply = Reply.fail("ERR: Unknown Command " + command);
                        }
                        System.out.println(reply.getExtras());
                    }
                }
            }
        }
    }
}