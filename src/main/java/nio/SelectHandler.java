package nio;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface SelectHandler extends Closeable {
    void select(SelectionKey key) throws IOException;
}
