import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MiniDBServer {
    private static final Logger logger = Logger.getLogger(MiniDBServer.class.getName());

    private static final int DB_SIZE = 16;
    private static final ConcurrentMap<Object, Object>[] MiniDBs;
    private static final Deque<Object> EMPTY_LIST = new LinkedList<>();

    static {
        MiniDBs = new ConcurrentHashMap[DB_SIZE];
        for (int i = 0; i < DB_SIZE; i++) {
            MiniDBs[i] = new ConcurrentHashMap<>();
        }
    }

    public static void main(String... args) throws IOException {
        final ServerSocket serverSocket = new ServerSocket(9000);
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        System.out.println("Started listening on " + serverSocket.getLocalSocketAddress().toString());
        while (!serverSocket.isClosed()) {
            final Socket socket = serverSocket.accept();
            executor.execute(() -> {
                logger.info("Received new connection from " + socket.getRemoteSocketAddress().toString());
                try (
                        var oos = new ObjectOutputStream(socket.getOutputStream());
                        var ois = new ObjectInputStream(socket.getInputStream());
                ) {
                    var db = MiniDBs[0];
                    while (!socket.isClosed()) {
                        Command command = (Command) ois.readObject();
                        Reply reply;
                        switch (command.getType()) {
                            case PING: {
                                reply = Reply.PONG;
                                break;
                            }
                            case SELECT: {
                                var index = (Integer) command.getExtras();
                                if (index < MiniDBs.length) {
                                    db = MiniDBs[index];
                                    reply = Reply.OK;
                                } else {
                                    reply = Reply.fail("Invalid index " + index);
                                }
                                break;
                            }
                            case KEYS: {
                                var pattern = (String) command.getExtras();
                                Pattern p = Pattern.compile(pattern);
                                reply = Reply.ok(db.keySet().stream().filter(key -> {
                                    Matcher matcher = p.matcher(key.toString());
                                    return matcher.matches();
                                }).collect(Collectors.toList()));
                                break;
                            }
                            case GET: {
                                reply = Reply.ok(db.get(command.getExtras()));
                                break;
                            }
                            case SET: {
                                var entry = (Map.Entry) command.getExtras();
                                db.put(entry.getKey(), entry.getValue());
                                reply = Reply.ok(entry);
                                break;
                            }
                            case EXISTS: {
                                reply = db.containsKey(command.getExtras()) ? Reply.YES : Reply.NO;
                                break;
                            }
                            case DEL: {
                                reply = Reply.ok(db.remove(command.getExtras()));
                                break;
                            }
                            case EXPIRE: {
                                final var entry = (Map.Entry<Object, Long>) command.getExtras();
                                final var frozenDB = db;
                                executor.schedule(() -> frozenDB.remove(entry.getKey()), entry.getValue(), TimeUnit.MILLISECONDS);
                                reply = Reply.OK;
                                break;
                            }
                            case HKEYS: {
                                var entry = (Map.Entry<Object, String>) command.getExtras();
                                Pattern p = Pattern.compile(entry.getValue());
                                reply = Reply.ok(
                                        ((Map<Object, Object>) (db.getOrDefault(entry.getKey(), Collections.emptyMap())))
                                                .keySet()
                                                .stream()
                                                .filter(key -> {
                                                    Matcher matcher = p.matcher(key.toString());
                                                    return matcher.matches();
                                                }).collect(Collectors.toList()));
                                break;
                            }
                            case HGET: {
                                var entry = (Map.Entry) command.getExtras();
                                reply = Reply.ok(
                                        ((Map) (db.getOrDefault(entry.getKey(), Collections.emptyMap()))).get(entry.getValue())
                                );
                                break;
                            }
                            case HSET: {
                                var entry = (Map.Entry) command.getExtras();
                                var hentry = (Map.Entry) entry.getValue();
                                db.compute(entry.getKey(), (key, htable) -> {
                                    if (htable == null) {
                                        htable = new ConcurrentHashMap();
                                    }
                                    ((Map) htable).put(hentry.getKey(), hentry.getValue());
                                    return htable;
                                });
                                reply = Reply.OK;
                                break;
                            }
                            case HEXISTS: {
                                var entry = (Map.Entry) command.getExtras();
                                var htable = (Map) db.get(entry.getKey());
                                reply = htable != null && htable.containsKey(entry.getValue()) ? Reply.YES : Reply.NO;
                                break;
                            }
                            case HDEL: {
                                var entry = (Map.Entry) command.getExtras();
                                db.computeIfPresent(entry.getKey(), (key, htable) -> {
                                    ((Map) htable).remove(entry.getValue());
                                    return htable;
                                });
                                reply = Reply.OK;
                                break;
                            }
                            case LEN: {
                                var results = (Deque) db.getOrDefault(command.getExtras(), EMPTY_LIST);
                                reply = Reply.ok(results.size());
                                break;
                            }
                            case FIRST: {
                                var results = (Deque) db.getOrDefault(command.getExtras(), EMPTY_LIST);
                                reply = Reply.ok(results.peekFirst());
                                break;
                            }
                            case LAST: {
                                var results = (Deque) db.getOrDefault(command.getExtras(), EMPTY_LIST);
                                reply = Reply.ok(results.peekLast());
                                break;
                            }
                            case LPUSH: {
                                // TODO: PUSH A LIST OF ELEMENTS
                                var entry = (Map.Entry) command.getExtras();
                                db.putIfAbsent(entry.getKey(), new ConcurrentLinkedDeque<>());
                                reply = Reply.ok(((Deque) db.compute(entry.getKey(), (key, val) -> {
                                    ((Deque) val).addFirst(entry.getValue());
                                    return val;
                                })).size());
                                break;
                            }
                            case LPOP: {
                                reply = Reply.ok(((Deque) db.getOrDefault(command.getExtras(), EMPTY_LIST)).pollFirst());
                                break;
                            }
                            case RPUSH: {
                                var entry = (Map.Entry) command.getExtras();
                                db.putIfAbsent(entry.getKey(), new ConcurrentLinkedDeque<>());
                                reply = Reply.ok(((Deque) db.compute(entry.getKey(), (key, val) -> {
                                    ((Deque) val).addLast(entry.getValue());
                                    return val;
                                })).size());
                                break;
                            }
                            case RPOP: {
                                reply = Reply.ok(((Deque) db.getOrDefault(command.getExtras(), EMPTY_LIST)).pollLast());
                                break;
                            }
                            case TYPE: {
                                var value = db.get(command.getExtras());
                                reply = Reply.ok(value == null ? null : value.getClass().getCanonicalName());
                                break;
                            }
                            case QUIT: {
                                reply = Reply.QUITTING;
                                break;
                            }
                            default: {
                                reply = Reply.UNKNOWN_COMMAND;
                            }
                        }
                        oos.writeObject(reply);
                        oos.flush();
                        if (reply == Reply.QUITTING) return;
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "error occurred then processing client request", e);
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "error occurred when closing client socket", e);
                    }
                }
            });
        }
        logger.info("Closed Server");
    }
}
