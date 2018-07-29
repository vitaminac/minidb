package nio;

import java.nio.ByteBuffer;

public interface DataHandler {
    void onData(ByteBuffer data);
}
