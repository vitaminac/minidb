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

    private Result send(Command command) {
        try {
            this.oos.writeObject(command);
            this.oos.flush();
            return (Result) ois.readObject();
        } catch (Exception e) {
            return Result.fail(e.toString());
        }
    }

    public Result ping() {
        return this.send(Command.createPingCommand());
    }

    public Result get(Object key) {
        return this.send(Command.createGetCommand(key));
    }

    public Result set(Object key, Object value) {
        return this.send(Command.createSetCommand(key, value));
    }

    public Result delete(Object key) {
        return this.send(Command.createDelCommand(key));
    }

    public Result exists(Object key) {
        return this.send(Command.createExistsCommand(key));
    }

    public Result length(Object key) {
        return this.send(Command.createLengthCommand(key));
    }

    public Result leftPush(Object key, Object value) {
        return this.send(Command.createLeftPushCommand(key, value));
    }

    public Result leftPop(Object key) {
        return this.send(Command.createLeftPopCommand(key));
    }

    public Result rightPush(Object key, Object value) {
        return this.send(Command.createRightPushCommand(key, value));
    }

    public Result rightPop(Object key) {
        return this.send(Command.createRightPopCommand(key));
    }

    public Result quit() {
        return this.send(Command.createQuitCommand());
    }

    public boolean isAlive() {
        return this.ping().isOk();
    }

    @Override
    public void close() throws IOException {
        this.socket.shutdownOutput();
        this.socket.shutdownInput();
        this.oos.close();
        this.ois.close();
        this.socket.close();
    }
}
