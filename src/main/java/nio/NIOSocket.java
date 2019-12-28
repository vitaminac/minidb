package nio;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

public interface NIOSocket {
    void write(ByteBuffer buffer);

    void close() throws IOException;

    Socket getSocket();
}
