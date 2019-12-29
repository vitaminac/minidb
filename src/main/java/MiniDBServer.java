import nio.EventLoop;
import nio.NIOServerSocketHandler;
import util.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MiniDBServer {
    private static final Logger logger = new Logger(MiniDBServer.class);

    private final EventLoop loop;
    private final ConcurrentMap<Object, Object> MiniDB = new ConcurrentHashMap<>();
    private final Deque<Object> EMPTY = new LinkedList<>();

    public MiniDBServer(EventLoop loop) {
        this.loop = loop;
    }

    @SuppressWarnings("unchecked")
    public Result processCommand(Command command) {
        switch (command.getType()) {
            case PING: {
                return Result.ok("PONG");
            }
            case KEYS: {
                var pattern = (String) command.getExtras();
                Pattern p = Pattern.compile(pattern);
                return Result.ok(MiniDB.keySet().stream().filter(key -> {
                    Matcher matcher = p.matcher(key.toString());
                    return matcher.matches();
                }).collect(Collectors.toList()));
            }
            case GET: {
                return Result.ok(MiniDB.get(command.getExtras()));
            }
            case SET: {
                var entry = (Map.Entry) command.getExtras();
                MiniDB.put(entry.getKey(), entry.getValue());
                return Result.ok(entry);
            }
            case EXISTS: {
                return Result.ok(MiniDB.containsKey(command.getExtras()) ? "YES" : "NO");
            }
            case DEL: {
                return Result.ok(MiniDB.remove(command.getExtras()));
            }
            case LEN: {
                var result = (Deque) MiniDB.getOrDefault(command.getExtras(), EMPTY);
                return Result.ok(result.size());
            }
            case EXPIRE: {
                var entry = (Map.Entry<Object, Long>) command.getExtras();
                this.loop.defer(() -> MiniDB.remove(entry.getKey()), entry.getValue());
                return Result.ok("OK");
            }
            case FIRST: {
                var result = (Deque) MiniDB.getOrDefault(command.getExtras(), EMPTY);
                return Result.ok(result.peekFirst());
            }
            case LAST: {
                var result = (Deque) MiniDB.getOrDefault(command.getExtras(), EMPTY);
                return Result.ok(result.peekLast());
            }
            case LPUSH: {
                // TODO: PUSH A LIST OF ELEMENTS
                var entry = (Map.Entry) command.getExtras();
                MiniDB.putIfAbsent(entry.getKey(), new ConcurrentLinkedDeque<>());
                return Result.ok(((Deque) MiniDB.compute(entry.getKey(), (key, val) -> {
                    ((Deque) val).addFirst(entry.getValue());
                    return val;
                })).size());
            }
            case LPOP: {
                return Result.ok(((Deque) MiniDB.getOrDefault(command.getExtras(), EMPTY)).pollFirst());
            }
            case RPUSH: {
                var entry = (Map.Entry) command.getExtras();
                MiniDB.putIfAbsent(entry.getKey(), new ConcurrentLinkedDeque<>());
                return Result.ok(((Deque) MiniDB.compute(entry.getKey(), (key, val) -> {
                    ((Deque) val).addLast(entry.getValue());
                    return val;
                })).size());
            }
            case RPOP: {
                return Result.ok(((Deque) MiniDB.getOrDefault(command.getExtras(), EMPTY)).pollLast());
            }
            case TYPE: {
                var value = MiniDB.get(command.getExtras());
                return Result.ok(value == null ? null : value.getClass().getCanonicalName());
            }
            case QUIT: {
                return Result.QUITTING;
            }
            default: {
                return Result.UNKNOWN_COMMAND;
            }
        }
    }

    public static void main(String... args) throws IOException {
        final var loop = EventLoop.create();
        final var server = new MiniDBServer(loop);
        // TODO get port from args
        loop.listen(9000, new NIOServerSocketHandler() {
            @Override
            public void onListen(ServerSocketChannel ssc) {
                System.out.println("Started listening on " + ssc.socket().getLocalSocketAddress().toString());
            }

            @Override
            public void onAccept(SocketChannel sc) {
                final Socket socket = sc.socket();
                logger.info("Received new connection from " + socket.getRemoteSocketAddress().toString());
                loop.start(() -> {
                    try (
                            // TODO: Buffer output
                            var oos = new ObjectOutputStream(socket.getOutputStream());
                            var ois = new ObjectInputStream(socket.getInputStream());
                    ) {
                        while (!socket.isClosed()) {
                            Command command = (Command) ois.readObject();
                            var result = server.processCommand(command);
                            oos.writeObject(result);
                            oos.flush();
                            if (result == Result.QUITTING) return;
                        }
                    } catch (Exception e) {
                        logger.error(e);
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            logger.error(e);
                        }
                    }
                });
            }

            @Override
            public void onClose() {
                logger.info("Closing Server");
            }
        });
        loop.run();
        logger.info("Closed Server");
    }
}
