package scheduler;

import java.util.Date;

public abstract class ScheduledTask implements Comparable<ScheduledTask>, Task {
    private Date date;

    public ScheduledTask(Date date) {
        this.date = date;
    }

    @Override
    public int compareTo(ScheduledTask o) {
        return this.date.compareTo(o.date);
    }

    public long getWaitingTime() {
        return this.date == null ? 0 : this.date.compareTo(new Date());
    }
}
