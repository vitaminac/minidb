package nio;

import event.EventLoop;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NIOSocket implements SelectHandler {
    public static NIOSocket create(EventLoop loop, SocketChannel sc, SocketHandler socketHandler) throws IOException {
        return new NIOSocket(loop, sc, socketHandler);
    }

    private final EventLoop loop;
    private final SocketHandler socketHandler;

    private NIOSocket(EventLoop loop, SocketChannel sc, SocketHandler socketHandler) throws IOException {
        this.loop = loop;
        sc.configureBlocking(false);
        this.socketHandler = socketHandler;
        loop.register(this, sc, SelectionKey.OP_READ, SelectionKey.OP_WRITE);
    }

    @Override
    public void select(SelectionKey key) throws IOException {
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
    }

    @Override
    public void close() {
        this.socketHandler.onClose();
    }
}
