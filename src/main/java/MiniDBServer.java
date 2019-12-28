import nio.EventLoop;
import nio.NIOServerSocketHandler;
import util.Logger;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class MiniDBServer {
    private static final Logger logger = new Logger(MiniDBServer.class);

    public static void main(String... args) throws IOException {
        final var loop = EventLoop.create();
        // TODO get port from args
        loop.listen(9000, new NIOServerSocketHandler() {
            @Override
            public void onListen(ServerSocketChannel ssc) {
                System.out.println("Started listening on " + ssc.socket().getLocalSocketAddress().toString());
            }

            @Override
            public void onAccept(SocketChannel sc) {
                logger.info("Received new connection from " + sc.socket().getRemoteSocketAddress().toString());
                loop.start(new ClientHandler(sc.socket()));
            }

            @Override
            public void onClose() {
                logger.info("Closing Server");
            }
        });
        loop.run();
        logger.info("Closed Server");
    }
}
