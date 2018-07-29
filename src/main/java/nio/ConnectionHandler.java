package nio;

public interface ConnectionHandler {
    void onConnect(NIOSocket socket);
}
