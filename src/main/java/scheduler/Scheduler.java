package scheduler;

import java.util.Date;
import java.util.PriorityQueue;
import java.util.Queue;

public class Scheduler {
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

    public static synchronized void repeat(Task task, int period) {
        defer(new Task() {
            @Override
            public void doTask() {
                task.doTask();
                defer(this, period);
            }
        }, period);
    }

    public static void run() {
        while (!tasks.isEmpty()) {
            final ScheduledTask task = tasks.remove();
            try {
                long ms = task.getWaitingTime();
                while (ms > 0) {
                    Thread.sleep(ms);
                    ms = task.getWaitingTime();
                }
                task.doTask();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
