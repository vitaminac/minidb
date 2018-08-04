package nio;

public interface ServerSocketHandler extends NIOHandler {
    ConnectionHandler buildConnectionHandler();
}
