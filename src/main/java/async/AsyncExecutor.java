package async;

import java.util.Date;
import java.util.PriorityQueue;
import java.util.Queue;

public class AsyncExecutor {
    public final static Queue<ScheduledTask> tasks;

    static {
        tasks = new PriorityQueue<>();
    }

    public static synchronized void schedule(Task task, Date date) {
        addScheduledTask(new ScheduledTask(date) {
            @Override
            public void doTask() {
                task.doTask();
            }
        });
    }

    public static synchronized void defer(Task task, long millisecond) {
        schedule(task, new Date(System.currentTimeMillis() + millisecond));
    }

    public static synchronized void addScheduledTask(ScheduledTask scheduledTask) {
        tasks.add(scheduledTask);
    }

    public static void run() {
        while (!tasks.isEmpty()) {
            final ScheduledTask task = tasks.remove();
            try {
                long ms;
                do {
                    ms = task.getWaitingTime();
                    if (ms <= 0) {
                        task.doTask();
                        break;
                    } else {
                        Thread.sleep(ms);
                    }
                } while (true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
