package nio;

import java.nio.ByteBuffer;

public interface ReadHandler {
    void onData(ByteBuffer data);

    void onEnd();
}
