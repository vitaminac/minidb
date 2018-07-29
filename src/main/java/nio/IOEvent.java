package nio;

import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class IOEvent<SOURCE> {
    public static final int WRITE = OP_WRITE;
    public static final int CONNECT = OP_CONNECT;
    public static final int ACCEPT = OP_ACCEPT;
    public static final int READ = OP_READ;
    private final int opCode;
    private final SOURCE source;

    public SOURCE getSource() {
        return source;
    }

    public IOEvent(SOURCE source, int... opCodes) {
        this.source = source;
        int opCode = 0;
        for (int i = 0; i < opCodes.length; i++) {
            opCode = opCode | opCodes[i];
        }
        this.opCode = opCode;
    }

    public int getOpCode() {
        return opCode;
    }

    public boolean canAccept() {
        return (this.opCode & OP_ACCEPT) == OP_ACCEPT;
    }

    public boolean canRead() {
        return (this.opCode & OP_READ) == OP_READ;
    }

    public boolean canWrite() {
        return (this.opCode & OP_WRITE) == OP_WRITE;
    }

    public boolean canConnect() {
        return (this.opCode & OP_CONNECT) == OP_CONNECT;
    }
}
