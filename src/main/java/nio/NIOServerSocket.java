package nio;

import event.EventLoop;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class NIOServerSocket implements NIOSelectable {
    private final EventLoop loop;
    private final ServerSocketChannel ssc;
    private ConnectionHandler handler;

    private NIOServerSocket(EventLoop loop, ServerSocketChannel ssc, ConnectionHandler handler) throws IOException {
        this.ssc = ssc;
        this.loop = loop;
        this.handler = handler;
        loop.register(this, new IOEvent<>(ssc, IOEvent.ACCEPT));
    }

    @Override
    public void onSelect(IOEvent<SelectionKey> event) throws IOException {
        if (event.canAccept()) {
            final SelectionKey key = event.getSource();
            final ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            final SocketChannel sc = ssc.accept();
            sc.configureBlocking(false);
            final NIOSocket socket = NIOSocket.wrap(this.loop, sc);
            if (this.handler != null) {
                handler.onConnect(socket);
            }
        }
    }

    public static NIOServerSocket create(int port, ConnectionHandler handler) throws IOException {
        InetSocketAddress address = new InetSocketAddress(port);
        EventLoop loop = EventLoop.DEFAULT_EVENT_LOOP;
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.socket().bind(address);
        final NIOServerSocket serverSocket = new NIOServerSocket(EventLoop.DEFAULT_EVENT_LOOP, ssc, handler);
        return serverSocket;
    }
}
