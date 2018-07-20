package scheduler;

import java.util.Date;
import java.util.PriorityQueue;
import java.util.Queue;

public class Scheduler {
    private static final Scheduler Default_Scheduler = new Scheduler();

    public static synchronized void setTimeout(Task task, long millisecond) {
        Default_Scheduler.defer(task, millisecond);
    }

    public static synchronized void setInterval(Task task, int period) {
        Default_Scheduler.repeat(task, period);
    }

    public static synchronized void run() {
        Default_Scheduler.loop();
    }

    public final Queue<ScheduledTask> tasks;

    public Scheduler() {
        tasks = new PriorityQueue<>();
    }

    public synchronized void addScheduledTask(ScheduledTask scheduledTask) {
        this.tasks.add(scheduledTask);
    }

    public synchronized void defer(Task task, long millisecond) {
        this.schedule(task, new Date(System.currentTimeMillis() + millisecond));
    }

    public void loop() {
        while (!tasks.isEmpty()) {
            final ScheduledTask task = this.tasks.remove();
            try {
                long ms = task.getWaitingTime();
                while (ms > 0) {
                    this.sleep(ms);
                    ms = task.getWaitingTime();
                }
                task.doTask();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void repeat(Task task, int period) {
        this.defer(new Task() {
            @Override
            public void doTask() throws Exception {
                task.doTask();
                defer(this, period);
            }
        }, period);
    }

    public synchronized void schedule(Task task, Date date) {
        this.addScheduledTask(new ScheduledTask(date) {
            @Override
            public void doTask() throws Exception {
                task.doTask();
            }
        });
    }

    private void sleep(long ms) {
        // TODO
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
