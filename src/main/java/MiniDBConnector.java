import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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
        return this.send(Command.createPingCommand());
    }

    public Reply select(int index) {
        return this.send(Command.createSelectCommand(index));
    }

    public Reply keys(String pattern) {
        return this.send(Command.createKeysCommand(pattern));
    }

    public Reply get(Object key) {
        return this.send(Command.createGetCommand(key));
    }

    public Reply set(Object key, Object value) {
        return this.send(Command.createSetCommand(key, value));
    }

    public Reply delete(Object key) {
        return this.send(Command.createDelCommand(key));
    }

    public Reply exists(Object key) {
        return this.send(Command.createExistsCommand(key));
    }

    public Reply expire(Object key, long milliseconds) {
        return this.send(Command.createExpireCommand(key, milliseconds));
    }

    public Reply length(Object key) {
        return this.send(Command.createLengthCommand(key));
    }

    public Reply leftPush(Object key, Object value) {
        return this.send(Command.createLeftPushCommand(key, value));
    }

    public Reply leftPop(Object key) {
        return this.send(Command.createLeftPopCommand(key));
    }

    public Reply rightPush(Object key, Object value) {
        return this.send(Command.createRightPushCommand(key, value));
    }

    public Reply rightPop(Object key) {
        return this.send(Command.createRightPopCommand(key));
    }

    public Reply type(Object key) {
        return this.send(Command.createTypeCommand(key));
    }

    public Reply quit() {
        return this.send(Command.createQuitCommand());
    }

    public boolean isAlive() {
        return !this.socket.isClosed() && this.ping().isOk();
    }

    @Override
    public void close() throws IOException {
        this.socket.shutdownInput();
        this.socket.shutdownOutput();
        this.oos.close();
        this.ois.close();
        this.socket.close();
    }
}
