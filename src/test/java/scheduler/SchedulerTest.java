package scheduler;

import nio.NIOServerSocket;
import org.junit.AfterClass;
import org.junit.Test;
import promise.Promise;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static scheduler.SchedulerHelper.run;
import static scheduler.SchedulerHelper.setImmediate;
import static scheduler.SchedulerHelper.setTimeout;

public class SchedulerTest {
    private static final int PORT = 1338;
    private static List<Object> results = new ArrayList<>();
    private NIOServerSocket server;

//    @Before
//    public void setUp() throws Exception {
//        this.server = NIOServerSocket.listen(PORT, new ServerSocketHandler() {
//                    @Override
//                    public void onError(Exception e) {
//                        SchedulerTest.this.results.add("This should not be called\n");
//                    }
//
//                    @Override
//                    public void onClose() {
//                        SchedulerTest.this.results.add("Closing server\n");
//                    }
//
//                    @Override
//                    public SocketHandler buildSocketHandler() {
//                        return new SocketHandler() {
//                            @Override
//                            public void onConnect() {
//                                SchedulerTest.this.sb.append("Connected to the server\n");
//                            }
//
//
//                            @Override
//                            public void onData(ByteBuffer data) {
//                                CharBuffer charBuffer = StandardCharsets.UTF_8.decode(data);
//                                String text = charBuffer.toString();
//                                SchedulerTest.this.sb.append(text);
//                            }
//
//                            @Override
//                            public void onError(Exception e) {
//                                SchedulerTest.this.sb.append("This should not be called\n");
//                            }
//
//                            @Override
//                            public void onClose() {
//                                SchedulerTest.this.sb.append("Closing the client\n");
//                            }
//                        };
//                    }
//                }
//        );
//    }

    @AfterClass
    public static void tearDown() {
        run();
        System.out.println(results);
        assertArrayEquals(new Object[]{1, 3, 5, 6, 7, 8, 9, 11, 12, 14}, results.toArray());
    }

    @Test
    public void setTimeoutTest() {
        Promise.from((DeferredTask<Integer>) promise -> setTimeout(() -> {
            int retVal = 12;
            results.add(retVal);
            promise.reject(new TestException(retVal));
        }, 501)).onFulfilled(result -> {
            int retVal = 13;
            results.add(retVal);
            return retVal;
        }).onRejected(e -> {
            assertEquals(12, ((TestException) e).getValue());
            int retVal = 14;
            results.add(retVal);
            return retVal;
        });

        Promise.from((DeferredTask<Integer>) promise -> setTimeout(() -> {
            int retVal = 6;
            results.add(retVal);
            promise.resolve(retVal);
        }, 500)).onFulfilled(result -> {
            assertEquals(6, result.intValue());
            int retVal = 7;
            results.add(retVal);
            return retVal;
        }).onFulfilled((result -> {
            assertEquals(7, result.intValue());
            int retVal = 8;
            results.add(retVal);
            throw new TestException(retVal);
        })).onRejected(e -> {
            assertEquals(8, ((TestException) e).getValue());
            int retVal = 9;
            results.add(retVal);
            return retVal;
        }).onRejected(e -> {
            results.add(10);
            return 10;
        }).onFinally(() -> results.add(11));
    }

    @Test
    public void setImmediateTest() {
        Promise.from((DeferredTask<Integer>) promise -> setImmediate(() -> {
            int retVal = 1;
            results.add(retVal);
            promise.reject(new TestException(retVal));
        })).then(result -> {
            results.add(2);
            return 2;
        }, e -> {
            assertEquals(1, ((TestException) e).getValue());
            int retVal = 3;
            results.add(retVal);
            return retVal;
        }).onRejected(e -> {
            int retVal = 4;
            results.add(retVal);
            return retVal;
        }).onFinally(() -> results.add(5));
    }
}