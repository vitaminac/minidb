package nio;

public interface NIOHandler {
    void onError(Exception e);

    void onClose();
}
