package nio;

import event.EventLoop;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class NIOServerSocket implements SelectHandler, ServerSocketWrapper {
    public static NIOServerSocket listen(EventLoop loop, InetSocketAddress address, ServerSocketHandler handler) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(address);
        return new NIOServerSocket(loop, ssc, handler);
    }

    public static ServerSocketWrapper listen(int port, ServerSocketHandler handler) throws IOException {
        return listen(EventLoop.DEFAULT_EVENT_LOOP, new InetSocketAddress(port), handler);
    }

    private final EventLoop loop;
    private final ServerSocketChannel ssc;
    private final ServerSocketHandler handler;
    private final SelectionKey key;

    private NIOServerSocket(EventLoop loop, ServerSocketChannel ssc, ServerSocketHandler handler) throws IOException {
        this.loop = loop;
        this.ssc = ssc;
        this.ssc.configureBlocking(false);
        this.handler = handler;
        this.key = loop.register(this, ssc, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void select() throws IOException {
        if (this.key.isAcceptable()) {
            final SocketChannel sc = this.ssc.accept();
            // TODO: should not close the server socket on error
            NIOSocket.create(this.loop, sc, this.handler.buildConnectionHandler());
        }
    }

    @Override
    public void close() throws IOException {
        this.loop.unregister(this.key);
        this.ssc.close();
        this.handler.onClose();
    }
}
