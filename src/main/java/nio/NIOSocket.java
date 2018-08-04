package nio;

import event.EventLoop;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class NIOSocket implements SelectHandler, SocketWrapper {
    public static void create(EventLoop loop, SocketChannel sc, ConnectionHandler connectionHandler) throws IOException {
        new NIOSocket(loop, sc, connectionHandler);
    }

    public static NIOSocket connect(InetSocketAddress address, ConnectionHandler connectionHandler) throws IOException {
        SocketChannel sc = SocketChannel.open();
        return new NIOSocket(EventLoop.DEFAULT_EVENT_LOOP, sc, address, connectionHandler);
    }

    private final EventLoop loop;
    private final SocketChannel sc;
    private final ConnectionHandler connectionHandler;
    private final Queue<byte[]> data;
    private final SelectionKey key;
    private SocketHandler socketHandler;
    private ByteBuffer buffer = ByteBuffer.allocate(0);

    {
        this.data = new LinkedList<>();
    }

    private NIOSocket(EventLoop loop, SocketChannel sc, InetSocketAddress address, ConnectionHandler connectionHandler) throws IOException {
        this.loop = loop;
        this.sc = sc;
        this.sc.configureBlocking(false);
        this.sc.connect(address);
        this.connectionHandler = connectionHandler;
        this.key = loop.register(this, sc, OP_CONNECT);
    }

    private NIOSocket(EventLoop loop, SocketChannel sc, ConnectionHandler connectionHandler) throws IOException {
        this.loop = loop;
        this.sc = sc;
        this.sc.configureBlocking(false);
        this.connectionHandler = connectionHandler;
        this.socketHandler = connectionHandler.onConnect(this);
        this.key = loop.register(this, sc, OP_READ);
    }

    @Override
    public void select() throws IOException {
        if (((this.key.interestOps() & OP_CONNECT) == OP_CONNECT) && this.key.isConnectable()) {
            try {
                this.sc.finishConnect();
            } catch (IOException e) {
                this.connectionHandler.onError(e);
                this.close();
                return;
            }
            this.socketHandler = this.connectionHandler.onConnect(this);
            this.key.interestOps(this.key.interestOps() & ~OP_CONNECT | OP_READ);
        }
        if (((this.key.interestOps() & OP_READ) == OP_READ) && this.key.isReadable()) {
            // TODO: magic const
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int n = this.sc.read(buffer);
            while (n > 0) {
                buffer.flip();
                this.socketHandler.onData(buffer.asReadOnlyBuffer());
                n = this.sc.read(buffer);
            }
            if (n < 0) {
                key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
                this.socketHandler.onEnd();
            }
        }
        if (((this.key.interestOps() & OP_WRITE) == OP_WRITE) && key.isWritable()) {
            if (this.buffer.remaining() > 0) {
                this.sc.write(this.buffer);
            }
            while (key.isWritable() && !this.data.isEmpty()) {
                this.buffer = ByteBuffer.wrap(this.data.remove());
                this.sc.write(this.buffer);
            }
            if (this.data.isEmpty() && (this.buffer.remaining() == 0)) {
                // if no more data can be written discard the operation
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                this.socketHandler.onDrain();
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.loop.unregister(this.key);
        this.sc.close();
        if (this.socketHandler != null) {
            this.socketHandler.onClose();
        }
    }

    @Override
    public synchronized void write(byte[] bytes) {
        key.interestOps(key.interestOps() | OP_WRITE);
        this.data.add(bytes);
    }

    @Override
    public synchronized void shutdown() throws IOException {
        this.sc.shutdownInput();
    }
}
