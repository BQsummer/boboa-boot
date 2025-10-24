package com.bqsummer.service.robot;

import com.bqsummer.common.dto.robot.RobotTask;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * RobotTaskScheduler 测试基类
 * 
 * 提供测试工具方法，用于创建测试任务和验证场景
 */
public abstract class RobotTaskSchedulerTestBase {
    
    /**
     * 创建测试任务
     * 
     * @param id 任务ID
     * @param actionType 动作类型 (SEND_MESSAGE, SEND_VOICE, SEND_NOTIFICATION)
     * @param scheduledAt 计划执行时间
     * @return 测试任务对象
     */
    protected RobotTask createTask(Long id, String actionType, LocalDateTime scheduledAt) {
        RobotTask task = new RobotTask();
        task.setId(id);
        task.setUserId(1001L);
        task.setRobotId(2001L);
        task.setTaskType("TEST");
        task.setActionType(actionType);
        task.setActionPayload("{\"message\":\"test\"}");
        task.setScheduledAt(scheduledAt);
        task.setStatus("PENDING");
        return task;
    }
    
    /**
     * 创建批量测试任务
     * 
     * @param count 任务数量
     * @param actionType 动作类型
     * @param scheduledAt 计划执行时间
     * @return 任务列表
     */
    protected List<RobotTask> createTasks(int count, String actionType, LocalDateTime scheduledAt) {
        List<RobotTask> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tasks.add(createTask((long) (i + 1), actionType, scheduledAt));
        }
        return tasks;
    }
    
    /**
     * 创建立即执行的测试任务
     * 
     * @param id 任务ID
     * @param actionType 动作类型
     * @return 测试任务对象
     */
    protected RobotTask createImmediateTask(Long id, String actionType) {
        return createTask(id, actionType, LocalDateTime.now());
    }
    
    /**
     * 创建延迟执行的测试任务
     * 
     * @param id 任务ID
     * @param actionType 动作类型
     * @param delaySeconds 延迟秒数
     * @return 测试任务对象
     */
    protected RobotTask createDelayedTask(Long id, String actionType, int delaySeconds) {
        return createTask(id, actionType, LocalDateTime.now().plusSeconds(delaySeconds));
    }
    
    /**
     * 等待指定时间（用于测试延迟场景）
     * 
     * @param millis 等待毫秒数
     */
    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep interrupted", e);
        }
    }
    
    /**
     * 常量：动作类型 - 发送消息
     */
    protected static final String ACTION_SEND_MESSAGE = "SEND_MESSAGE";
    
    /**
     * 常量：动作类型 - 发送语音
     */
    protected static final String ACTION_SEND_VOICE = "SEND_VOICE";
    
    /**
     * 常量：动作类型 - 发送通知
     */
    protected static final String ACTION_SEND_NOTIFICATION = "SEND_NOTIFICATION";
}
