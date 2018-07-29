package nio;

import event.EventLoop;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NIOSocket implements NIOSelectable {
    private final EventLoop loop;
    private final SocketChannel channel;
    private DataHandler onReadHandler;

    private NIOSocket(EventLoop loop, SocketChannel sc) throws IOException {
        this.loop = loop;
        this.channel = sc;
        loop.register(this, new IOEvent<>(sc, IOEvent.READ));
    }

    public void onRead(DataHandler handler) {
        this.onReadHandler = handler;
    }

    @Override
    public void onSelect(IOEvent<SelectionKey> event) throws IOException {
        if (event.canRead()) {
            final SelectionKey key = event.getSource();
            final SocketChannel sc = (SocketChannel) key.channel();
            // TODO: magic const
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int n = sc.read(buffer);
            while (n > 0) {
                buffer.flip();
                if (this.onReadHandler != null) {
                    onReadHandler.onData(buffer.asReadOnlyBuffer());
                }
                n = sc.read(buffer);
            }
            if (n < 0) {
                this.loop.unregister(key);
            }
        }
    }

    public static NIOSocket wrap(EventLoop loop, SocketChannel sc) throws IOException {
        sc.configureBlocking(false);
        return new NIOSocket(loop, sc);
    }
}
