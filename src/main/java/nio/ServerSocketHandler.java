package nio;

public interface ServerSocketHandler extends NIOHandler {
    SocketHandler buildSocketHandler();
}
