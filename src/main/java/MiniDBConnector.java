import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.AbstractMap;

public class MiniDBConnector implements AutoCloseable {
    private final Socket socket;
    private final ObjectOutputStream oos;
    private final ObjectInputStream ois;

    public MiniDBConnector(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        // TODO: Buffer Output Stream
        this.oos = new ObjectOutputStream(this.socket.getOutputStream());
        this.ois = new ObjectInputStream(this.socket.getInputStream());
    }

    private Reply send(Command command) {
        try {
            this.oos.writeObject(command);
            this.oos.flush();
            return (Reply) ois.readObject();
        } catch (Exception e) {
            return Reply.fail(e.toString());
        }
    }

    public Reply ping() {
        return this.send(Command.PING);
    }

    public Reply select(int index) {
        return this.send(new Command(Command.CommandType.SELECT, index));
    }

    public Reply keys(String pattern) {
        return this.send(new Command(Command.CommandType.KEYS, pattern));
    }

    public Reply get(Object key) {
        return this.send(new Command(Command.CommandType.GET, key));
    }

    public Reply set(Object key, Object value) {
        return this.send(new Command(Command.CommandType.SET, new AbstractMap.SimpleEntry<>(key, value)));
    }

    public Reply delete(Object key) {
        return this.send(new Command(Command.CommandType.DEL, key));
    }

    public Reply exists(Object key) {
        return this.send(new Command(Command.CommandType.EXISTS, key));
    }

    public Reply expire(Object key, long milliseconds) {
        return this.send(new Command(Command.CommandType.EXPIRE, new AbstractMap.SimpleEntry<>(key, milliseconds)));
    }

    public Reply hkeys(Object key, String pattern) {
        return this.send(new Command(Command.CommandType.HKEYS, new AbstractMap.SimpleEntry<>(key, pattern)));
    }

    public Reply hget(Object key, Object hkey) {
        return this.send(new Command(Command.CommandType.HGET, new AbstractMap.SimpleEntry<>(key, hkey)));
    }

    public Reply hset(Object key, Object hkey, Object hvalue) {
        return this.send(
                new Command(Command.CommandType.HSET,
                        new AbstractMap.SimpleEntry<>(key, new AbstractMap.SimpleEntry<>(hkey, hvalue)))
        );
    }

    public Reply hexists(Object key, Object hkey) {
        return this.send(new Command(Command.CommandType.HEXISTS, new AbstractMap.SimpleEntry<>(key, hkey)));
    }

    public Reply hdelete(Object key, Object hkey) {
        return this.send(new Command(Command.CommandType.HDEL, new AbstractMap.SimpleEntry<>(key, hkey)));
    }

    public Reply length(Object key) {
        return this.send(new Command(Command.CommandType.LEN, key));
    }

    public Reply first(Object key) {
        return this.send(new Command(Command.CommandType.FIRST, key));
    }

    public Reply last(Object key) {
        return this.send(new Command(Command.CommandType.LAST, key));
    }

    public Reply leftPush(Object key, Object value) {
        return this.send(new Command(Command.CommandType.LPUSH, new AbstractMap.SimpleEntry<>(key, value)));
    }

    public Reply leftPop(Object key) {
        return this.send(new Command(Command.CommandType.LPOP, key));
    }

    public Reply rightPush(Object key, Object value) {
        return this.send(new Command(Command.CommandType.RPUSH, new AbstractMap.SimpleEntry<>(key, value)));
    }

    public Reply rightPop(Object key) {
        return this.send(new Command(Command.CommandType.RPOP, key));
    }

    public Reply type(Object key) {
        return this.send(new Command(Command.CommandType.TYPE, key));
    }

    public Reply quit() {
        return this.send(Command.QUIT);
    }

    public boolean isAlive() {
        return !this.socket.isClosed() && this.ping().isOk();
    }

    @Override
    public void close() throws IOException {
        if (!this.socket.isClosed()) {
            this.socket.shutdownInput();
            this.socket.shutdownOutput();
            this.oos.close();
            this.ois.close();
            this.socket.close();
        }
    }
}
