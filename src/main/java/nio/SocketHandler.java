package nio;

public interface SocketHandler extends NIOHandler, DataHandler {
    void onConnect();
}
