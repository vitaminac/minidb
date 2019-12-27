package nio;

import java.io.IOException;

public interface SocketWrapper {
    void write(byte[] bytes) throws IOException;

    void shutdown() throws IOException;

    void close() throws IOException;
}
