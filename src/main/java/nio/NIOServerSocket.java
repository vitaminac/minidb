package nio;

import event.EventLoop;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class NIOServerSocket implements SelectHandler {
    private final EventLoop loop;
    private final ServerSocketChannel ssc;
    private ServerSocketHandler handler;

    private NIOServerSocket(EventLoop loop, ServerSocketChannel ssc, ServerSocketHandler handler) throws IOException {
        this.ssc = ssc;
        this.ssc.configureBlocking(false);
        this.loop = loop;
        this.handler = handler;
        loop.register(this, ssc, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void select(SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            final ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            final SocketChannel sc = ssc.accept();
            sc.configureBlocking(false);
            NIOSocket.create(this.loop, sc, this.handler.buildSocketHandler());
        }
    }

    public static NIOServerSocket listen(EventLoop loop, int port, ServerSocketHandler handler) throws IOException {
        var address = new InetSocketAddress(port);
        var ssc = ServerSocketChannel.open();
        ssc.socket().bind(address);
        final var serverSocket = new NIOServerSocket(loop, ssc, handler);
        return serverSocket;
    }

    public static NIOServerSocket listen(int port, ServerSocketHandler handler) throws IOException {
        return listen(EventLoop.DEFAULT_EVENT_LOOP, port, handler);
    }

    @Override
    public void close() {
        this.handler.onClose();
    }
}
