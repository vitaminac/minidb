import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

public class ClientHandler implements Runnable {
    private static final ConcurrentMap<Object, Object> MINIDB = new ConcurrentHashMap<>();
    private static final Deque<Object> EMPTY = new LinkedList<>();

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        // TODO: NON-BLOCKING
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
                        var entry = (DictEntry) command.getExtras();
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
                    case LENGTH: {
                        var result = (Deque) MINIDB.getOrDefault(command.getExtras(), EMPTY);
                        oos.writeObject(Result.ok(result.size()));
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
                    case LEFT_PUSH: {
                        // TODO: PUSH A LIST OF ELEMENTS
                        var entry = (DictEntry) command.getExtras();
                        MINIDB.putIfAbsent(entry.getKey(), new ConcurrentLinkedDeque<>());
                        oos.writeObject(Result.ok(((Deque) MINIDB.compute(entry.getKey(), (key, val) -> {
                            ((Deque) val).addFirst(entry.getValue());
                            return val;
                        })).size()));
                        break;
                    }
                    case LEFT_POP: {
                        oos.writeObject(Result.ok(((Deque) MINIDB.getOrDefault(command.getExtras(), EMPTY)).pollFirst()));
                        break;
                    }
                    case RIGHT_PUSH: {
                        var entry = (DictEntry) command.getExtras();
                        MINIDB.putIfAbsent(entry.getKey(), new ConcurrentLinkedDeque<>());
                        oos.writeObject(Result.ok(((Deque) MINIDB.compute(entry.getKey(), (key, val) -> {
                            ((Deque) val).addLast(entry.getValue());
                            return val;
                        })).size()));
                        break;
                    }
                    case RIGHT_POP: {
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
