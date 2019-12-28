import nio.EventLoop;
import util.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

public class ClientHandler implements Runnable {
    private static final Logger logger = new Logger(ClientHandler.class);

    private static final ConcurrentMap<Object, Object> MINIDB = new ConcurrentHashMap<>();
    private static final Deque<Object> EMPTY = new LinkedList<>();

    private final Socket socket;
    private final EventLoop loop;

    public ClientHandler(final EventLoop loop, final Socket socket) {
        this.loop = loop;
        this.socket = socket;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        try (
                // TODO: Buffer output
                var oos = new ObjectOutputStream(this.socket.getOutputStream());
                var ois = new ObjectInputStream(this.socket.getInputStream());
        ) {
            while (!this.socket.isClosed()) {
                Command command = (Command) ois.readObject();
                switch (command.getType()) {
                    case PING: {
                        oos.writeObject(Result.ok("PONG"));
                        break;
                    }
                    case GET: {
                        oos.writeObject(Result.ok(MINIDB.get(command.getExtras())));
                        break;
                    }
                    case SET: {
                        var entry = (Map.Entry) command.getExtras();
                        MINIDB.put(entry.getKey(), entry.getValue());
                        oos.writeObject(Result.ok(entry));
                        break;
                    }
                    case EXISTS: {
                        oos.writeObject(Result.ok(MINIDB.containsKey(command.getExtras()) ? "YES" : "NO"));
                        break;
                    }
                    case DEL: {
                        oos.writeObject(Result.ok(MINIDB.remove(command.getExtras())));
                        break;
                    }
                    case LEN: {
                        var result = (Deque) MINIDB.getOrDefault(command.getExtras(), EMPTY);
                        oos.writeObject(Result.ok(result.size()));
                        break;
                    }
                    case EXPIRE: {
                        var entry = (Map.Entry<Object, Long>) command.getExtras();
                        this.loop.defer(() -> MINIDB.remove(entry.getKey()), entry.getValue());
                        oos.writeObject(Result.ok("OK"));
                        break;
                    }
                    case FIRST: {
                        var result = (Deque) MINIDB.getOrDefault(command.getExtras(), EMPTY);
                        oos.writeObject(Result.ok(result.peekFirst()));
                        break;
                    }
                    case LAST: {
                        var result = (Deque) MINIDB.getOrDefault(command.getExtras(), EMPTY);
                        oos.writeObject(Result.ok(result.peekLast()));
                        break;
                    }
                    case LPUSH: {
                        // TODO: PUSH A LIST OF ELEMENTS
                        var entry = (Map.Entry) command.getExtras();
                        MINIDB.putIfAbsent(entry.getKey(), new ConcurrentLinkedDeque<>());
                        oos.writeObject(Result.ok(((Deque) MINIDB.compute(entry.getKey(), (key, val) -> {
                            ((Deque) val).addFirst(entry.getValue());
                            return val;
                        })).size()));
                        break;
                    }
                    case LPOP: {
                        oos.writeObject(Result.ok(((Deque) MINIDB.getOrDefault(command.getExtras(), EMPTY)).pollFirst()));
                        break;
                    }
                    case RPUSH: {
                        var entry = (Map.Entry) command.getExtras();
                        MINIDB.putIfAbsent(entry.getKey(), new ConcurrentLinkedDeque<>());
                        oos.writeObject(Result.ok(((Deque) MINIDB.compute(entry.getKey(), (key, val) -> {
                            ((Deque) val).addLast(entry.getValue());
                            return val;
                        })).size()));
                        break;
                    }
                    case RPOP: {
                        oos.writeObject(Result.ok(((Deque) MINIDB.getOrDefault(command.getExtras(), EMPTY)).pollLast()));
                        break;
                    }
                    case QUIT: {
                        oos.writeObject(Result.ok("Quitting"));
                        this.socket.shutdownInput();
                        this.socket.shutdownOutput();
                        return;
                    }
                    default: {
                        oos.writeObject(Result.fail("ERR: unknown command"));
                    }
                }
                oos.flush();
            }
        } catch (Exception e) {
            logger.error(e);
        } finally {
            try {
                this.socket.close();
            } catch (IOException e) {
                logger.error(e);
            }
        }
    }
}
