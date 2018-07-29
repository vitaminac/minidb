package nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface NIOSelectable {
    void onSelect(IOEvent<SelectionKey> event) throws IOException;
}
