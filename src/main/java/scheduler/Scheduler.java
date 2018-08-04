package scheduler;

import event.EventLoop;
import promise.DeferredPromise;
import util.Logger;

import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Scheduler {
    private static long askUpTime() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }

    private final Queue<Job> pending;
    private final Queue<ScheduledTask> timers;
    private final HashMap<Future<?>, Job> futures;
    private final ExecutorService executor;
    private final EventLoop loop;
    private final Logger logger = new Logger(this.getClass());
    private long now;

    public Scheduler(EventLoop loop) {
        this.timers = new PriorityQueue<>();
        this.pending = new ArrayDeque<>();
        this.loop = loop;
        this.futures = new HashMap<>();
        this.executor = new ThreadPoolExecutor(15, 30, 1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    public synchronized void addScheduledTask(ScheduledTask scheduledTask) {
        this.timers.add(scheduledTask);
    }

    public synchronized void arrange(Job job) {
        this.pending.add(job);
    }

    public synchronized void defer(Job job, long millisecond) {
        this.schedule(new ScheduledTask(askUpTime() + millisecond) {
            @Override
            public void doJob() throws Exception {
                job.doJob();
            }
        });
    }

    public synchronized <T> DeferredPromise<T> when(Callable<T> callable) {
        final Future<T> future = this.executor.submit(callable);
        return DeferredPromise.from(promise -> this.futures.put(future, new Job() {
            @Override
            public void doJob() throws Exception {
                if (future.isDone()) {
                    promise.resolve(future.get());
                } else if (future.isCancelled()) {
                    promise.reject(new CancellationException());
                }
            }
        }));
    }

    public void run() {
        // check if the loop is considered to be alive
        while (!this.pending.isEmpty() || !timers.isEmpty() || !this.loop.isIdle()) {
            // caches the current time at the execute of the event loop tick
            // in order to reduce the number of time-related system calls
            this.updateTime();

            // first phase
            // All active timers scheduled for a time
            // before the loop’s concept of now queue into pending list
            final long timeout = this.processTimersAndCalculateTimeout();

            // second phase
            // I/O polling
            if (!this.loop.isIdle()) {
                this.loop.poll(this.pending.isEmpty() ? timeout : 0);
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

    public synchronized void schedule(ScheduledTask task) {
        this.addScheduledTask(task);
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
        return 0;
    }

    private void updateTime() {
        now = askUpTime();
    }
}
