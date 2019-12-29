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

    private static final int DB_SIZE = 16;
    private static final ConcurrentMap<Object, Object>[] MiniDBs;
    private static final Deque<Object> EMPTY = new LinkedList<>();

    static {
        MiniDBs = new ConcurrentHashMap[DB_SIZE];
        for (int i = 0; i < DB_SIZE; i++) {
            MiniDBs[i] = new ConcurrentHashMap<>();
        }
    }

    public static void main(String... args) throws IOException {
        final var loop = EventLoop.create();
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
                                    reply = Reply.ok(db.containsKey(command.getExtras()) ? "YES" : "NO");
                                    break;
                                }
                                case DEL: {
                                    reply = Reply.ok(db.remove(command.getExtras()));
                                    break;
                                }
                                case LEN: {
                                    var results = (Deque) db.getOrDefault(command.getExtras(), EMPTY);
                                    reply = Reply.ok(results.size());
                                    break;
                                }
                                case EXPIRE: {
                                    final var entry = (Map.Entry<Object, Long>) command.getExtras();
                                    final var frozenDB = db;
                                    loop.defer(() -> frozenDB.remove(entry.getKey()), entry.getValue());
                                    reply = Reply.OK;
                                    break;
                                }
                                case FIRST: {
                                    var results = (Deque) db.getOrDefault(command.getExtras(), EMPTY);
                                    reply = Reply.ok(results.peekFirst());
                                    break;
                                }
                                case LAST: {
                                    var results = (Deque) db.getOrDefault(command.getExtras(), EMPTY);
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
                                    reply = Reply.ok(((Deque) db.getOrDefault(command.getExtras(), EMPTY)).pollFirst());
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
                                    reply = Reply.ok(((Deque) db.getOrDefault(command.getExtras(), EMPTY)).pollLast());
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
