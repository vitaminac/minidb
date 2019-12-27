package nio;

import java.io.Closeable;
import java.io.IOException;

public interface SelectHandler extends Closeable {
    void select() throws IOException;
}
