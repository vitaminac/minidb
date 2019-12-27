package nio;


public interface ConnectionHandler extends ErrorHandler {
    SocketHandler onConnect(SocketWrapper socket);
}
