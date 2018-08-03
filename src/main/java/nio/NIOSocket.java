package nio;

import event.EventLoop;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

public class NIOSocket implements SelectHandler {
    public static NIOSocket create(EventLoop loop, SocketChannel sc, SocketHandler socketHandler) throws IOException {
        return new NIOSocket(loop, sc, socketHandler);
    }

    private final EventLoop loop;
    private final SocketHandler socketHandler;
    private final Queue<byte[]> data;
    private ByteBuffer buffer = ByteBuffer.allocate(0);

    {
        this.data = new LinkedList<>();
    }

    private NIOSocket(EventLoop loop, SocketChannel sc, SocketHandler socketHandler) throws IOException {
        this.loop = loop;
        sc.configureBlocking(false);
        this.socketHandler = socketHandler;
        loop.register(this, sc, SelectionKey.OP_READ, SelectionKey.OP_WRITE);
    }

    @Override
    public void select(SelectionKey key) throws IOException {
        if (key.isConnectable()) {
            final SocketChannel sc = (SocketChannel) key.channel();
            try {
                sc.finishConnect();
            } catch (IOException e) {
                this.socketHandler.onError(e);
                return;
            }
            this.socketHandler.onConnect();
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }
        if (key.isReadable()) {
            final SocketChannel sc = (SocketChannel) key.channel();
            // TODO: magic const
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int n = sc.read(buffer);
            while (n > 0) {
                buffer.flip();
                this.socketHandler.onData(buffer.asReadOnlyBuffer());
                n = sc.read(buffer);
            }
            if (n < 0) {
                this.loop.unregister(key);
            }
        }
        if (key.isWritable()) {
            final SocketChannel sc = (SocketChannel) key.channel();
            if (this.buffer.remaining() > 0) {
                sc.write(this.buffer);
            }
            while (key.isWritable() && !this.data.isEmpty()) {
                this.buffer = ByteBuffer.wrap(this.data.remove());
                sc.write(this.buffer);
            }
            if (this.data.isEmpty() && (this.buffer.remaining() == 0)) {
                // if no more data can be written discard the operation
                key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
            } else {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            }
        }
    }

    @Override
    public void close() {
        this.socketHandler.onClose();
    }

    public synchronized void write(byte[] bytes) {
        this.data.add(bytes);
    }

    public static NIOSocket connect(InetSocketAddress address, SocketHandler socketHandler) throws IOException {
        SocketChannel sc = SocketChannel.open();
        sc.bind(address);
        return new NIOSocket(EventLoop.DEFAULT_EVENT_LOOP, sc, socketHandler);
    }
}
