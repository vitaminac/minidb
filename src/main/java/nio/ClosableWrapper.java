package nio;

import java.io.IOException;

public interface ClosableWrapper {
    void close() throws IOException;
}
