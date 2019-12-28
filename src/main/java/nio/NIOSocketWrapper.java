package nio;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

import static java.nio.channels.SelectionKey.OP_WRITE;

class NIOSocketWrapper implements SelectHandler, NIOSocket {
    private final SocketChannel sc;
    private final SelectionKey key;
    private final NIOSocketHandler handler;
    private final Queue<ByteBuffer> data = new LinkedList<>();
    private ByteBuffer buffer = ByteBuffer.allocate(0);

    public NIOSocketWrapper(SocketChannel sc, SelectionKey key, NIOSocketHandler handler) {
        this.sc = sc;
        this.key = key;
        this.handler = handler;
    }

    @Override
    public synchronized void write(ByteBuffer buffer) {
        this.key.interestOps(key.interestOps() | OP_WRITE);
        this.data.add(buffer);
    }

    @Override
    public void select() throws IOException {
        if (this.key.isReadable()) {
            // TODO: magic const
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int n;
            while ((n = sc.read(buffer)) > 0) {
                buffer.flip();
                this.handler.onData(buffer.asReadOnlyBuffer());
            }
            // if the channel has reached end-of-stream
            if (n < 0) {
                this.key.interestOps(this.key.interestOps() & (~SelectionKey.OP_READ));
                this.handler.onEnd();
            }
        }
        if (key.isWritable()) {
            if (this.buffer.remaining() > 0) {
                this.sc.write(this.buffer);
            }
            while (key.isWritable() && !this.data.isEmpty()) {
                this.sc.write(this.data.remove());
            }
            if (this.data.isEmpty() && (this.buffer.remaining() == 0)) {
                // if no more data can be written discard the operation
                this.key.interestOps(this.key.interestOps() & ~OP_WRITE);
                this.handler.onDrain();
            }
        }
    }

    @Override
    public synchronized void close() throws IOException {
        this.sc.close();
        this.handler.onClose();
    }

    @Override
    public Socket getSocket() {
        return this.sc.socket();
    }
}
