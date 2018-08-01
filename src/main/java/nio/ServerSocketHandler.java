package nio;

public interface ServerSocketHandler {
    void onClose();

    SocketHandler buildSocketHandler();
}
