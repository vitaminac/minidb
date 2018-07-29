package scheduler;

import nio.DataHandler;
import nio.NIOServerSocket;
import org.junit.Test;
import promise.Promise;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static scheduler.Scheduler.run;
import static scheduler.Scheduler.setTimeout;

public class SchedulerTest {
    private static final int PORT = 1338;

    @Test
    public void testEventLoop() throws Exception {
        NIOServerSocket.create(PORT, socket -> socket.onRead(new DataHandler() {
            private StringBuilder sb = new StringBuilder();

            @Override
            public void onData(ByteBuffer data) {
                CharBuffer charBuffer = StandardCharsets.UTF_8.decode(data);
                String text = charBuffer.toString();
                sb.append(text);
                System.out.println(sb.toString());
            }
        }));
        run();
    }

    @Test
    public void test() {
        List<Integer> order = new ArrayList<>();
        Promise.create((DeferredTask<Integer>) (resolver, rejecter) -> setTimeout(() -> {
            order.add(6);
            resolver.resolve(6);
        }, 2000)).onFulfilled(value -> {
            order.add(7);
            return 7;
        }).onFulfilled((result -> {
            order.add(8);
            throw new Exception("8");
        })).onRejected(e -> {
            order.add(9);
            return 9;
        }).onRejected(e -> {
            order.add(10);
            return 10;
        }).onFinally(() -> order.add(11));

        Promise.create((DeferredTask<Integer>) (resolver, rejecter) -> setTimeout(() -> {
            order.add(1);
            rejecter.reject(new Exception("1"));
        }, 1500)).then(result -> {
            order.add(2);
            return 2;
        }, e -> {
            order.add(3);
            return 3;
        }).onRejected(e -> {
            order.add(4);
            return 4;
        }).onFinally(() -> order.add(5));

        Promise.create((DeferredTask<Integer>) (resolver, rejecter) -> setTimeout(() -> {
            order.add(12);
            rejecter.reject(new Exception("from here"));
        }, 3000)).onFulfilled(result -> {
            order.add(13);
            return 13;
        }).onRejected(e -> {
            order.add(14);
            return 14;
        });

        run();
        int[] result = order.stream().mapToInt(i -> i).toArray();
        System.out.println(Arrays.toString(result));
        assertArrayEquals(new int[]{1, 3, 5, 6, 7, 8, 9, 11, 12, 14}, result);
    }
}