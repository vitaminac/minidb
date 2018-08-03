package scheduler;

import event.EventLoop;
import promise.Promise;

import java.util.concurrent.Callable;

public class SchedulerHelper {

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
        Default_Scheduler.run();
    }

    public static synchronized <V> Promise<V> await(Callable<V> callable) {
        return Default_Scheduler.when(callable);
    }
}
