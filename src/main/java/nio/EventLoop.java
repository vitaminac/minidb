package nio;

import promise.DeferredPromise;
import scheduler.Job;
import scheduler.ScheduledTask;
import scheduler.Task;
import util.Logger;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.*;

import static java.nio.channels.SelectionKey.OP_CONNECT;

public class EventLoop {
    private static long askUpTime() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }

    private static final Logger logger = new Logger(EventLoop.class);

    // you might run into an infinite loop if you omit volatile, since the first thread might cache the value of stop.
    // Here, the volatile serves as a hint to the compiler to be a bit more careful with optimizations.
    private volatile boolean stopped = false;
    private long now;

    // The Java NIO Selector is a component which can examine one or more Java NIO Channel instances,
    // and determine which channels are ready for e.g. reading or writing
    private final Selector selector;
    // TODO: list of event handle?
    private final Map<SelectionKey, SelectHandler> events;
    private final ExecutorService executorService;
    private final Queue<Job> pending;
    private final Queue<ScheduledTask> timers;
    private final HashMap<Future<?>, Job> futures;

    private EventLoop(
            Selector selector,
            ExecutorService executorService
    ) {
        this.selector = selector;
        this.executorService = executorService;
        this.events = new HashMap<>();
        this.pending = new LinkedList<>();
        this.timers = new PriorityQueue<>();
        this.futures = new HashMap<>();
    }

    public static EventLoop create() {
        try {
            return new EventLoop(
                    Selector.open(),
                    new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() + 1, 32, 1, TimeUnit.HOURS, new LinkedBlockingQueue<>(255))
            );
        } catch (IOException e) {
            logger.error(e);
            return null;
        }
    }

    public synchronized void unregister(SelectionKey key) {
        key.cancel();
        this.events.remove(key);
    }

    public synchronized void accept(final SocketChannel sc, final NIOSocketHandler handler) throws IOException {
        sc.configureBlocking(false);
        final var key = sc.register(this.selector, SelectionKey.OP_READ);
        var socket = new NIOSocketWrapper(sc, key, handler);
        this.events.put(key, socket);
        handler.onConnected(socket);
    }

    public synchronized void connect(final InetSocketAddress address, final NIOSocketHandler handler) throws IOException {
        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);
        sc.connect(address);
        final var key = sc.register(this.selector, OP_CONNECT);
        this.events.put(key, new SelectHandler() {
            @Override
            public void select() throws IOException {
                if (key.isConnectable()) {
                    sc.finishConnect();
                    // don't do unregister, will raise CancelledKeyException
                    // If this key has already been cancelled then invoking this method has no effect.
                    // Once cancelled, a key remains forever invalid.
                    // Don't cancel the key, don't re-register: just change its interestOps
                    key.interestOpsAnd(~OP_CONNECT);
                    key.interestOpsOr(SelectionKey.OP_READ);
                    // Blocking mode
                    // A selectable channel is either in blocking mode or in non-blocking mode.
                    // In blocking mode, every I/O operation invoked upon the channel will block until it completes.
                    // In non-blocking mode an I/O operation will never block and may transfer fewer bytes than were requested or possibly no bytes at all.
                    // The blocking mode of a selectable channel may be determined by invoking its isBlocking method.
                    // Newly-created selectable channels are always in blocking mode.
                    // Non-blocking mode is most useful in conjunction with selector-based multiplexing.
                    // A channel must be placed into non-blocking mode before being registered with a selector,
                    // and may not be returned to blocking mode until it has been deregistered.
                    var socket = new NIOSocketWrapper(sc, key, handler);
                    synchronized (EventLoop.this.events) {
                        EventLoop.this.events.put(key, socket);
                    }
                    handler.onConnected(socket);
                }
            }

            @Override
            public void close() throws IOException {
                sc.close();
            }
        });
    }

    public synchronized void listen(final int port, final NIOServerSocketHandler handler) throws IOException {
        final ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress(port));
        ssc.configureBlocking(false);
        final SelectionKey key = ssc.register(this.selector, SelectionKey.OP_ACCEPT);
        this.events.put(key, new SelectHandler() {
            @Override
            public void select() throws IOException {
                if (key.isAcceptable()) {
                    final SocketChannel sc = ssc.accept();
                    // TODO: should not close the server socket on error
                    handler.onAccept(sc);
                }
            }

            @Override
            public void close() throws IOException {
                ssc.close();
                handler.onClose();
            }
        });
        handler.onListen(ssc);
    }

    public void poll(long timeout, boolean now) {
        System.out.println("poll with " + timeout + " and " + now);
        // Processing events in the poll queue
        int n = 0;
        try {
            if (now) {
                n = selector.selectNow();
            } else {
                n = selector.select(timeout);
            }
        } catch (IOException e) {
            logger.error(e);
        }
        if (n > 0) {
            // we must use iterator and remove already processed key
            final Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                final var key = it.next();
                final var handler = this.events.get(key);
                if (key.isValid() && key.channel().isOpen()) {
                    try {
                        handler.select();
                    } catch (Exception e) {
                        logger.error(e);
                    }
                } else {
                    try {
                        handler.close();
                    } catch (IOException e) {
                        logger.error(e);
                    }
                    this.unregister(key);
                }
                it.remove();
            }
        }
    }

    public boolean isIdle() {
        return this.events.isEmpty();
    }

    public void start(Runnable runnable) {
        this.executorService.submit(runnable);
    }

    public synchronized void arrange(Job job) {
        this.pending.add(job);
    }

    public synchronized void schedule(ScheduledTask task) {
        this.timers.add(task);
    }

    public synchronized void defer(Job job, long millisecond) {
        this.schedule(new ScheduledTask(askUpTime() + millisecond) {
            @Override
            public void doJob() throws Exception {
                job.doJob();
            }
        });
    }

    public synchronized Task repeat(Job job, int period) {
        Task task = new Task() {
            private boolean cancelled = false;

            @Override
            public void cancel() {
                this.cancelled = true;
            }

            @Override
            public void doJob() throws Exception {
                if (!this.cancelled) {
                    job.doJob();
                    defer(this, period);
                }
            }
        };
        this.defer(task, period);
        return task;
    }

    public synchronized <T> DeferredPromise<T> await(Callable<T> callable) {
        final Future<T> future = this.executorService.submit(callable);
        return DeferredPromise.from(promise -> this.futures.put(future, () -> {
            if (future.isDone()) {
                promise.resolve(future.get());
            } else if (future.isCancelled()) {
                promise.reject(new CancellationException());
            }
        }));
    }

    private synchronized long processTimersAndCalculateTimeout() {
        final Iterator<ScheduledTask> it = timers.iterator();
        while (it.hasNext()) {
            final ScheduledTask scheduledTask = it.next();
            final long earliestStartTime = scheduledTask.getStartDate();
            if (earliestStartTime < this.now) {
                this.pending.add(scheduledTask);
                it.remove();
            } else {
                return earliestStartTime - this.now;
            }
        }
        // zero mean wait for infinity
        return 0;
    }

    public void run() {
        // check if the loop is considered to be alive
        while (!this.stopped && (!this.pending.isEmpty() || !this.timers.isEmpty() || !this.isIdle())) {
            // caches the current time at the execute of the event loop tick
            // in order to reduce the number of time-related system calls
            this.now = askUpTime();

            // first phase
            // All active timers scheduled for a time
            // before the loop’s concept of now queue into pending list
            final long timeout = this.processTimersAndCalculateTimeout();

            // second phase
            // I/O polling
            if (!this.isIdle()) {
                this.poll(timeout, !this.pending.isEmpty() || this.stopped /* if there is a timer that requested to stop */);
            }

            // third phase
            // check future
            final Iterator<Map.Entry<Future<?>, Job>> it = this.futures.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<Future<?>, Job> entry = it.next();
                final Future<?> future = entry.getKey();
                if (future.isDone() || future.isCancelled()) {
                    this.pending.add(entry.getValue());
                    it.remove();
                }
            }

            // fourth phase
            // Pending callbacks are called
            while (!this.pending.isEmpty()) {
                final Job job = this.pending.remove();
                try {
                    job.doJob();
                } catch (Exception e) {
                    logger.error(e);
                }
            }
        }
        this.executorService.shutdown();
        // clear the unfinished jobs
        for (var job : this.pending) {
            try {
                job.doJob();
            } catch (Exception e) {
                logger.error(e);
            }
        }
        for (var entry : this.events.entrySet()) {
            try {
                entry.getValue().close();
            } catch (IOException e) {
                logger.error(e);
            }
        }
        try {
            this.executorService.awaitTermination(0, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error(e);
        }
    }

    public void stop() {
        this.stopped = true;
    }
}
