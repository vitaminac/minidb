package scheduler;

import nio.ConnectionHandler;
import nio.NIOServerSocket;
import nio.NIOSocket;
import nio.ServerSocketHandler;
import nio.ServerSocketWrapper;
import nio.SocketHandler;
import nio.SocketWrapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import promise.DeferredPromise;
import promise.Promise;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static scheduler.SchedulerHelper.run;
import static scheduler.SchedulerHelper.setImmediate;
import static scheduler.SchedulerHelper.setTimeout;

public class SchedulerTest {
    private static final int PORT = 13458;
    private static List<Object> results = new ArrayList<Object>() {
        @Override
        public boolean add(Object o) {
            System.out.println(o);
            return super.add(o);
        }
    };
    private static ServerSocketWrapper server;

    @BeforeClass
    public static void setUp() throws Exception {
        server = NIOServerSocket.listen(PORT, new ServerSocketHandler() {
                    @Override
                    public void onError(Exception e) {
                        results.add(e.getMessage());
                    }

                    @Override
                    public void onClose() {
                        results.add("Closing server socket");
                    }

                    @Override
                    public ConnectionHandler buildConnectionHandler() {
                        results.add("Received connection from client");
                        return new ConnectionHandler() {
                            @Override
                            public SocketHandler onConnect(SocketWrapper socket) {
                                return new SocketHandler() {
                                    private StringBuilder sb = new StringBuilder();

                                    @Override
                                    public void onClose() {
                                        results.add("Closing the connection from client");
                                    }

                                    @Override
                                    public void onData(ByteBuffer data) {
                                        CharBuffer charBuffer = StandardCharsets.UTF_8.decode(data);
                                        String text = charBuffer.toString();
                                        this.sb.append(text);
                                    }

                                    @Override
                                    public void onEnd() {
                                        results.add(sb.toString());
                                        try {
                                            socket.write(sb.toString().getBytes());
                                        } catch (IOException e) {
                                            results.add(e.getMessage());
                                        }
                                    }

                                    @Override
                                    public void onDrain() {
                                        try {
                                            socket.close();
                                        } catch (IOException e) {
                                            results.add(e.getMessage());
                                        }
                                    }
                                };
                            }

                            @Override
                            public void onError(Exception e) {
                                results.add(e.getMessage());
                            }
                        };
                    }
                }
        );
    }

    @AfterClass
    public static void tearDown() {
        setTimeout(() -> server.close(), 5000);
        run();
        System.out.println(results);
        assertArrayEquals(new Object[]{1, 3, 5, 6, 7, 8, 9, 11, 15, 12, 14, 101, 102, 103, 16, 17, 105, "Successfully connected to the server", "Received connection from client", "Closing the connection from server", "Sent from client", "Closing the connection from client", "Closing server socket"}, results.toArray());
    }

    @Test
    public void socketTest() {
        setTimeout(() -> {
            NIOSocket.connect(new InetSocketAddress(PORT), new ConnectionHandler() {
                @Override
                public SocketHandler onConnect(SocketWrapper socket) {
                    results.add("Successfully connected to the server");
                    return new SocketHandler() {
                        private StringBuilder sb = new StringBuilder();

                        {
                            try {
                                socket.write("Sent from client".getBytes());
                            } catch (IOException e) {
                                results.add(e.getMessage());
                            }
                        }

                        @Override
                        public void onClose() {
                            results.add("Closing the connection from server");
                        }

                        @Override
                        public void onData(ByteBuffer data) {
                            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(data);
                            String text = charBuffer.toString();
                            this.sb.append(text);
                        }

                        @Override
                        public void onEnd() {
                            results.add(sb.toString());
                        }

                        @Override
                        public void onDrain() {
                            try {
                                socket.shutdown();
                                socket.close();
                            } catch (IOException e) {
                                results.add(e.getMessage());
                            }
                        }
                    };
                }

                @Override
                public void onError(Exception e) {
                    results.add(e.getMessage());
                }
            });
        }, 2000);
    }

    @Test
    public void setTimeoutTest() {
        DeferredPromise.from(promise -> setTimeout(() -> {
            int retVal = 12;
            results.add(retVal);
            promise.reject(new TestException(retVal));
        }, 600)).onFulfilled(result -> {
            int retVal = 13;
            results.add(retVal);
            return retVal;
        }).onRejected(e -> {
            assertEquals(12, ((TestException) e).getValue());
            int retVal = 14;
            results.add(retVal);
            return retVal;
        });

        DeferredPromise.from((Executor<Promise<Integer>>) promise -> setTimeout(() -> {
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
        DeferredPromise.from(promise -> setImmediate(() -> {
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

    @Test
    public void thenTest() {
        final Promise<Integer> p = DeferredPromise.from((Executor<Promise<Integer>>) promise -> setTimeout(() -> {
            int retVal = 15;
            results.add(retVal);
            promise.resolve(retVal);
        }, 550)).then(i -> {
            return DeferredPromise.from(promise -> setTimeout(() -> {
                int retVal = i + 1;
                results.add(retVal);
                assertEquals(retVal, 16);
                promise.resolve(retVal);
            }, 500));
        });
        p.onFulfilled((i) -> {
            int retVal = i + 1;
            assertEquals(17, retVal);
            results.add(retVal);
            return retVal;
        });
    }

    @Test
    public void allPromisesTest() {
        final Promise<Integer> p1 = DeferredPromise.from(p -> setTimeout(() -> p.resolve(101), 1000));
        final Promise<Integer> p2 = DeferredPromise.from(p -> setTimeout(() -> p.resolve(102), 800));
        final Promise<Integer> p3 = DeferredPromise.from(p -> setTimeout(() -> p.resolve(103), 100));
        final Promise<List<Integer>> all = Promise.all(Arrays.asList(p1, p2, p3));
        all.onFulfilled(integers -> {
            results.addAll(integers);
            return null;
        });
    }

    @Test
    public void allPromisesAndThrow() {
        final Promise<Integer> p1 = DeferredPromise.from(p -> setTimeout(() -> {
            p.resolve(104);
        }, 1100));
        final Promise<Integer> p2 = DeferredPromise.from(p -> setTimeout(() -> {
            p.reject(new TestException(105));
        }, 900));
        final Promise<Integer> p3 = DeferredPromise.from(p -> setTimeout(() -> p.resolve(106), 200));
        final Promise<List<Integer>> all = Promise.all(Arrays.asList(p1, p2, p3));
        all.then(integers -> {
            results.addAll(integers);
            return null;
        }, e -> {
            results.add(((TestException) e).getValue());
            return null;
        });
    }
}