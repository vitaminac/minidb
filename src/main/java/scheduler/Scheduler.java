package scheduler;

import event.EventLoop;

import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

public class Scheduler {
    private static final Scheduler Default_Scheduler;

    static {
        Default_Scheduler = new Scheduler(EventLoop.DEFAULT_EVENT_LOOP);
    }

    public static synchronized void setTimeout(Task task, long millisecond) {
        Default_Scheduler.defer(task, millisecond);
    }

    public static synchronized void setInterval(Task task, int period) {
        Default_Scheduler.repeat(task, period);
    }

    public static synchronized void setImmediate(Task task) {
        Default_Scheduler.arrange(task);
    }

    public static synchronized void run() {
        Default_Scheduler.start();
    }

    public static long askUpTime() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }

    private static void log(Throwable e) {
        e.printStackTrace();
    }

    private final Queue<Task> pending;
    private final Queue<ScheduledTask> timers;
    private final EventLoop loop;
    private long now;

    private Scheduler(EventLoop loop) {
        this.timers = new PriorityQueue<>();
        this.pending = new ArrayDeque<>();
        this.loop = loop;
    }

    public synchronized void addScheduledTask(ScheduledTask scheduledTask) {
        this.timers.add(scheduledTask);
    }

    public synchronized void arrange(Task task) {
        this.pending.add(task);
    }

    public synchronized void defer(Task task, long millisecond) {
        this.schedule(new ScheduledTask(askUpTime() + millisecond) {
            @Override
            public void doJob() throws Exception {
                task.doJob();
            }
        });
    }

    public void start() {
        // check if the loop is considered to be alive
        while (!this.pending.isEmpty() || !timers.isEmpty() || !this.loop.isIdle()) {
            // caches the current time at the start of the event loop tick
            // in order to reduce the number of time-related system calls
            this.updateTime();

            // first phase
            // All active timers scheduled for a time
            // before the loop’s concept of now queue into pending list
            final long timeout = this.processTimersAndCalculateTimeout();

            // second phase
            // I/O polling
            if (!this.loop.isIdle()) {
                this.loop.poll(timeout);
            }

            // third phase
            // Pending callbacks are called
            while (!this.pending.isEmpty()) {
                final Task task = this.pending.remove();
                try {
                    task.doJob();
                } catch (Exception e) {
                    log(e);
                }
            }
        }
    }

    public synchronized void repeat(Task task, int period) {
        this.defer(new Task() {
            @Override
            public void doJob() throws Exception {
                task.doJob();
                defer(this, period);
            }
        }, period);
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
