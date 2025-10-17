package com.bqsummer.service.robot;

import com.bqsummer.common.dto.robot.RobotTask;
import lombok.Getter;

import java.time.ZoneId;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 任务包装类，实现 Delayed 接口用于 DelayQueue
 */
@Getter
public class RobotTaskWrapper implements Delayed {
    
    private final RobotTask task;
    private final long executeAtMillis;
    
    public RobotTaskWrapper(RobotTask task) {
        this.task = task;
        this.executeAtMillis = task.getScheduledAt()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }
    
    @Override
    public long getDelay(TimeUnit unit) {
        long diff = executeAtMillis - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public int compareTo(Delayed o) {
        if (o instanceof RobotTaskWrapper) {
            return Long.compare(this.executeAtMillis, 
                              ((RobotTaskWrapper) o).executeAtMillis);
        }
        return 0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RobotTaskWrapper)) return false;
        RobotTaskWrapper that = (RobotTaskWrapper) o;
        return task.getId().equals(that.task.getId());
    }
    
    @Override
    public int hashCode() {
        return task.getId().hashCode();
    }
}
