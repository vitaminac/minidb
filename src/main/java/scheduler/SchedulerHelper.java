package scheduler;

import event.EventLoop;
import promise.DeferredPromise;

import java.util.concurrent.Callable;

public class SchedulerHelper {

    private static final Scheduler Default_Scheduler;

    static {
        Default_Scheduler = new Scheduler(EventLoop.DEFAULT_EVENT_LOOP);
    }

    public static synchronized void setTimeout(Job job, long millisecond) {
        Default_Scheduler.defer(job, millisecond);
    }

    public static synchronized Task setInterval(Job job, int period) {
        return Default_Scheduler.repeat(job, period);
    }

    public static synchronized void setImmediate(Job job) {
        Default_Scheduler.arrange(job);
    }

    public static synchronized void run() {
        Default_Scheduler.run();
    }

    public static synchronized <V> DeferredPromise<V> await(Callable<V> callable) {
        return Default_Scheduler.when(callable);
    }
}
